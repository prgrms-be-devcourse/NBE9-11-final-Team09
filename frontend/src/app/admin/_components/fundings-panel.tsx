"use client";

import { cancelAdminFunding, getAdminFundings } from "@/lib/admin-api";
import type { AdminFunding, AdminPageResponse } from "@/types/admin";
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

const EMPTY_PAGE: AdminPageResponse<AdminFunding> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true,
};

export default function FundingsPanel({
  onError,
  onSuccess,
}: {
  onError: (error: unknown) => void;
  onSuccess: (message: string) => void;
}) {
  const [fundings, setFundings] = useState(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<AdminFunding | null>(null);
  const [cancelTarget, setCancelTarget] = useState<AdminFunding | null>(null);
  const [reason, setReason] = useState("");
  const [processing, setProcessing] = useState(false);

  const loadFundings = useCallback(async () => {
    try {
      setFundings(await getAdminFundings(page));
    } catch (error) {
      onError(error);
    } finally {
      setLoading(false);
    }
  }, [onError, page]);

  useEffect(() => {
    let ignored = false;

    getAdminFundings(page)
      .then((result) => {
        if (!ignored) setFundings(result);
      })
      .catch((error) => {
        if (!ignored) onError(error);
      })
      .finally(() => {
        if (!ignored) setLoading(false);
      });

    return () => {
      ignored = true;
    };
  }, [onError, page]);

  async function handleCancel() {
    if (!cancelTarget || !reason.trim()) return;
    setProcessing(true);
    try {
      await cancelAdminFunding(cancelTarget.fundingId, reason.trim());
      setCancelTarget(null);
      setReason("");
      setDetail(null);
      onSuccess("펀딩이 강제 취소되었습니다.");
      await loadFundings();
    } catch (error) {
      onError(error);
    } finally {
      setProcessing(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div><p className="text-sm font-bold text-[#2b6b88]">MANAGEMENT</p><h2 className="mt-2 text-3xl font-black tracking-tight">펀딩 관리</h2><p className="mt-2 text-sm text-slate-500">개설된 펀딩 현황을 확인하고 운영 정책 위반 펀딩을 관리합니다.</p></div>
        <p className="text-sm font-bold text-slate-500">총 {fundings.totalElements.toLocaleString("ko-KR")}건</p>
      </div>

      <section className="mt-6 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/80">
        {loading ? <AdminLoading /> : fundings.content.length === 0 ? <AdminEmpty message="조회된 펀딩이 없습니다." /> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1050px] text-left">
              <thead className="border-b border-slate-100 bg-slate-50/80 text-xs font-bold text-slate-500"><tr><th className="px-6 py-4">펀딩</th><th className="px-4 py-4">방장</th><th className="px-4 py-4">출발일</th><th className="px-4 py-4">참여 현황</th><th className="px-4 py-4">상태</th><th className="px-6 py-4 text-right">관리</th></tr></thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {fundings.content.map((funding) => (
                  <tr key={funding.fundingId} className="hover:bg-slate-50/70">
                    <td className="max-w-sm px-6 py-4"><p className="truncate font-bold text-slate-900">{funding.title}</p><p className="mt-1 text-xs text-slate-400">#{funding.fundingId} · {funding.busType}</p></td>
                    <td className="px-4 py-4 text-slate-600">{funding.hostEmail}</td>
                    <td className="px-4 py-4 text-slate-600">{formatAdminDate(funding.departureDate)}</td>
                    <td className="px-4 py-4"><p className="font-bold text-slate-700">{funding.currentParticipants}/{funding.maxParticipants}명</p><p className="mt-1 text-xs text-slate-400">최소 {funding.minParticipants}명</p></td>
                    <td className="px-4 py-4"><AdminStatusBadge status={funding.status} /></td>
                    <td className="px-6 py-4"><div className="flex justify-end gap-2"><button type="button" onClick={() => setDetail(funding)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50">상세</button><button type="button" disabled={["CANCELLED", "COMPLETED"].includes(funding.status)} onClick={() => setCancelTarget(funding)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-bold text-rose-600 hover:bg-rose-50 disabled:opacity-30">강제 취소</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <AdminPagination page={fundings} loading={loading} onChange={(nextPage) => { setLoading(true); setPage(nextPage); }} />
      </section>

      {detail && (
        <AdminModal title="펀딩 상세 정보" description={`펀딩 #${detail.fundingId}`} onClose={() => setDetail(null)}>
          <dl><DetailRow label="제목" value={detail.title} /><DetailRow label="방장 이메일" value={detail.hostEmail} /><DetailRow label="출발일" value={formatAdminDate(detail.departureDate)} /><DetailRow label="버스 유형" value={detail.busType} /><DetailRow label="상태" value={<AdminStatusBadge status={detail.status} />} /><DetailRow label="참여 인원" value={`${detail.currentParticipants}/${detail.maxParticipants}명`} /><DetailRow label="최소 인원" value={`${detail.minParticipants}명`} /><DetailRow label="개설일" value={formatAdminDate(detail.createdAt, true)} /></dl>
          <div className="mt-5 rounded-2xl bg-slate-50 p-4"><p className="text-xs font-bold text-slate-400">모집 내용</p><p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">{detail.content}</p></div>
        </AdminModal>
      )}

      {cancelTarget && (
        <AdminModal title="펀딩 강제 취소" description={`'${cancelTarget.title}' 펀딩을 취소합니다. 결제 및 참여자 처리에 영향을 줄 수 있습니다.`} onClose={() => { setCancelTarget(null); setReason(""); }}>
          <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">취소 사유</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={4} placeholder="펀딩 강제 취소 사유를 입력하세요." className="w-full resize-none rounded-xl border border-slate-200 p-4 text-sm outline-none focus:border-rose-400 focus:ring-4 focus:ring-rose-100" /></label>
          <div className="mt-5 flex justify-end gap-2"><button type="button" onClick={() => setCancelTarget(null)} className="rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold">취소</button><button type="button" disabled={!reason.trim() || processing} onClick={handleCancel} className="rounded-xl bg-rose-600 px-4 py-3 text-sm font-bold text-white disabled:opacity-40">{processing ? "처리 중" : "펀딩 취소"}</button></div>
        </AdminModal>
      )}
    </div>
  );
}
