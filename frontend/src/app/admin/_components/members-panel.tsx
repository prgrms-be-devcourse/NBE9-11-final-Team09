"use client";

import { getAdminMember, getAdminMembers, withdrawAdminMember } from "@/lib/admin-api";
import type { AdminMember, AdminMemberDetail, AdminPageResponse } from "@/types/admin";
import { useCallback, useEffect, useState } from "react";
import {
  AdminEmpty,
  AdminLoading,
  AdminModal,
  AdminPagination,
  AdminStatusBadge,
  DetailRow,
  formatAdminDate,
} from "./admin-ui";

const EMPTY_PAGE: AdminPageResponse<AdminMember> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true,
};

export default function MembersPanel({
  onError,
  onSuccess,
}: {
  onError: (error: unknown) => void;
  onSuccess: (message: string) => void;
}) {
  const [members, setMembers] = useState(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<AdminMemberDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [withdrawTarget, setWithdrawTarget] = useState<AdminMember | null>(null);
  const [reason, setReason] = useState("");
  const [processing, setProcessing] = useState(false);

  const loadMembers = useCallback(async (active = { current: true }) => {
    if (active.current) setLoading(true);

    try {
      const data = await getAdminMembers(page);
      if (active.current) setMembers(data);
    } catch (error) {
      if (active.current) onError(error);
    } finally {
      if (active.current) setLoading(false);
    }
  }, [onError, page]);

  useEffect(() => {
    const active = { current: true };
    void Promise.resolve().then(() => loadMembers(active));

    return () => {
      active.current = false;
    };
  }, [loadMembers]);
  async function openDetail(memberId: number) {
    setDetailLoading(true);
    try {
      setDetail(await getAdminMember(memberId));
    } catch (error) {
      onError(error);
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleWithdraw() {
    if (!withdrawTarget || !reason.trim()) return;
    setProcessing(true);
    try {
      await withdrawAdminMember(withdrawTarget.memberId, reason.trim());
      setWithdrawTarget(null);
      setReason("");
      setDetail(null);
      onSuccess("회원이 강제 탈퇴 처리되었습니다.");
      await loadMembers();
    } catch (error) {
      onError(error);
    } finally {
      setProcessing(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl">
      <PanelHeader title="회원 관리" description="가입 회원을 조회하고 운영 정책에 따라 계정을 관리합니다." total={members.totalElements} />
      <section className="mt-6 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/80">
        {loading ? <AdminLoading /> : members.content.length === 0 ? <AdminEmpty message="조회된 회원이 없습니다." /> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left">
              <thead className="border-b border-slate-100 bg-slate-50/80 text-xs font-bold text-slate-500">
                <tr>
                  <th className="px-6 py-4">회원</th><th className="px-4 py-4">연락처</th><th className="px-4 py-4">가입 유형</th><th className="px-4 py-4">상태</th><th className="px-4 py-4">가입일</th><th className="px-6 py-4 text-right">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {members.content.map((member) => (
                  <tr key={member.memberId} className="hover:bg-slate-50/70">
                    <td className="px-6 py-4"><p className="font-bold text-slate-900">{member.nickname}</p><p className="mt-1 text-xs text-slate-400">{member.email}</p></td>
                    <td className="px-4 py-4 text-slate-600">{member.phoneNumber}</td>
                    <td className="px-4 py-4 text-slate-600">{member.provider ?? "LOCAL"}</td>
                    <td className="px-4 py-4"><AdminStatusBadge status={member.status} /></td>
                    <td className="px-4 py-4 text-slate-500">{formatAdminDate(member.createdAt)}</td>
                    <td className="px-6 py-4"><div className="flex justify-end gap-2"><button type="button" onClick={() => openDetail(member.memberId)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50">상세</button><button type="button" disabled={member.status === "WITHDRAWN"} onClick={() => setWithdrawTarget(member)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-bold text-rose-600 hover:bg-rose-50 disabled:opacity-30">강제 탈퇴</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <AdminPagination page={members} loading={loading} onChange={(nextPage) => { setLoading(true); setPage(nextPage); }} />
      </section>

      {(detail || detailLoading) && (
        <AdminModal title="회원 상세 정보" description="회원 활동 현황과 계정 정보를 확인합니다." onClose={() => setDetail(null)}>
          {detailLoading || !detail ? <AdminLoading /> : (
            <>
              <dl><DetailRow label="회원 번호" value={detail.memberId} /><DetailRow label="이름 / 닉네임" value={`${detail.name} / ${detail.nickname}`} /><DetailRow label="이메일" value={detail.email} /><DetailRow label="전화번호" value={detail.phoneNumber} /><DetailRow label="가입 유형" value={detail.provider ?? "LOCAL"} /><DetailRow label="상태" value={<AdminStatusBadge status={detail.status} />} /><DetailRow label="가입일" value={formatAdminDate(detail.createdAt, true)} /></dl>
              <div className="mt-5 grid grid-cols-3 gap-3"><CountCard label="참여" value={detail.participationCount} /><CountCard label="모집" value={detail.fundingCount} /><CountCard label="결제" value={detail.paymentCount} /></div>
            </>
          )}
        </AdminModal>
      )}

      {withdrawTarget && (
        <AdminModal title="회원 강제 탈퇴" description={`${withdrawTarget.nickname} 회원을 탈퇴 처리합니다. 이 작업은 신중하게 진행해주세요.`} onClose={() => { setWithdrawTarget(null); setReason(""); }}>
          <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">처리 사유</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={4} placeholder="강제 탈퇴 사유를 입력하세요." className="w-full resize-none rounded-xl border border-slate-200 p-4 text-sm outline-none focus:border-rose-400 focus:ring-4 focus:ring-rose-100" /></label>
          <div className="mt-5 flex justify-end gap-2"><button type="button" onClick={() => setWithdrawTarget(null)} className="rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold">취소</button><button type="button" disabled={!reason.trim() || processing} onClick={handleWithdraw} className="rounded-xl bg-rose-600 px-4 py-3 text-sm font-bold text-white disabled:opacity-40">{processing ? "처리 중" : "강제 탈퇴"}</button></div>
        </AdminModal>
      )}
    </div>
  );
}

function PanelHeader({ title, description, total }: { title: string; description: string; total: number }) {
  return <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end"><div><p className="text-sm font-bold text-[#2b6b88]">MANAGEMENT</p><h2 className="mt-2 text-3xl font-black tracking-tight">{title}</h2><p className="mt-2 text-sm text-slate-500">{description}</p></div><p className="text-sm font-bold text-slate-500">총 {total.toLocaleString("ko-KR")}건</p></div>;
}

function CountCard({ label, value }: { label: string; value: number }) {
  return <div className="rounded-2xl bg-slate-50 p-4 text-center"><p className="text-xs text-slate-400">{label}</p><p className="mt-1 text-xl font-black text-slate-800">{value}</p></div>;
}
