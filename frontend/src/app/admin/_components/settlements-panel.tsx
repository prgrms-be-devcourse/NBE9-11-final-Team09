"use client";

import { getAdminSettlement, getAdminSettlements } from "@/lib/admin-api";
import type { AdminPageResponse, AdminSettlement, AdminSettlementDetail } from "@/types/admin";
import { useEffect, useState } from "react";
import {
  AdminEmpty,
  AdminLoading,
  AdminModal,
  AdminPagination,
  AdminStatusBadge,
  DetailRow,
  formatAdminCurrency,
  formatAdminDate,
} from "./admin-ui";

const EMPTY_PAGE: AdminPageResponse<AdminSettlement> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true,
};

export default function SettlementsPanel({ onError }: { onError: (error: unknown) => void }) {
  const [settlements, setSettlements] = useState(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<AdminSettlementDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    let ignored = false;

    getAdminSettlements(page)
      .then((result) => {
        if (!ignored) setSettlements(result);
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

  async function openDetail(settlementId: number) {
    setDetailLoading(true);
    try {
      setDetail(await getAdminSettlement(settlementId));
    } catch (error) {
      onError(error);
    } finally {
      setDetailLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end"><div><p className="text-sm font-bold text-[#2b6b88]">MANAGEMENT</p><h2 className="mt-2 text-3xl font-black tracking-tight">정산 관리</h2><p className="mt-2 text-sm text-slate-500">펀딩별 결제 집계와 방장 페이백 현황을 확인합니다.</p></div><p className="text-sm font-bold text-slate-500">총 {settlements.totalElements.toLocaleString("ko-KR")}건</p></div>

      <section className="mt-6 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/80">
        {loading ? <AdminLoading /> : settlements.content.length === 0 ? <AdminEmpty message="조회된 정산 내역이 없습니다." /> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1050px] text-left">
              <thead className="border-b border-slate-100 bg-slate-50/80 text-xs font-bold text-slate-500"><tr><th className="px-6 py-4">정산 / 펀딩</th><th className="px-4 py-4">방장</th><th className="px-4 py-4">총 결제액</th><th className="px-4 py-4">페이백</th><th className="px-4 py-4">상태</th><th className="px-4 py-4">생성일</th><th className="px-6 py-4 text-right">관리</th></tr></thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {settlements.content.map((settlement) => (
                  <tr key={settlement.settlementId} className="hover:bg-slate-50/70">
                    <td className="max-w-xs px-6 py-4"><p className="truncate font-bold text-slate-900">{settlement.fundingTitle}</p><p className="mt-1 text-xs text-slate-400">정산 #{settlement.settlementId} · 펀딩 #{settlement.fundingId}</p></td>
                    <td className="px-4 py-4 text-slate-600">{settlement.hostEmail}</td>
                    <td className="px-4 py-4 font-bold text-slate-800">{formatAdminCurrency(settlement.totalAmount)}</td>
                    <td className="px-4 py-4 text-slate-600">{formatAdminCurrency(settlement.hostPaybackAmount)}</td>
                    <td className="px-4 py-4"><AdminStatusBadge status={settlement.status} /></td>
                    <td className="px-4 py-4 text-slate-500">{formatAdminDate(settlement.createdAt)}</td>
                    <td className="px-6 py-4 text-right"><button type="button" onClick={() => openDetail(settlement.settlementId)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50">상세 조회</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <AdminPagination page={settlements} loading={loading} onChange={(nextPage) => { setLoading(true); setPage(nextPage); }} />
      </section>

      {(detail || detailLoading) && (
        <AdminModal title="정산 상세 정보" description="결제 집계와 방장 페이백 금액을 확인합니다." onClose={() => setDetail(null)}>
          {detailLoading || !detail ? <AdminLoading /> : (
            <>
              <dl><DetailRow label="정산 번호" value={detail.settlementId} /><DetailRow label="펀딩" value={detail.fundingTitle} /><DetailRow label="방장" value={`${detail.hostNickname} (${detail.hostEmail})`} /><DetailRow label="출발일" value={formatAdminDate(detail.departureDate)} /><DetailRow label="펀딩 상태" value={<AdminStatusBadge status={detail.fundingStatus} />} /><DetailRow label="정산 상태" value={<AdminStatusBadge status={detail.status} />} /><DetailRow label="총 결제 금액" value={formatAdminCurrency(detail.totalAmount)} /><DetailRow label="플랫폼 수수료" value={formatAdminCurrency(detail.platformFee)} /><DetailRow label="방장 페이백" value={formatAdminCurrency(detail.hostPaybackAmount)} /><DetailRow label="지급 일시" value={formatAdminDate(detail.paybackPaidAt, true)} /></dl>
              <div className="mt-6 rounded-2xl bg-[#f1f7f8] p-5"><p className="text-xs font-bold tracking-widest text-[#2b6b88]">PAYMENT SUMMARY</p><div className="mt-4 grid grid-cols-2 gap-3"><Summary label="전체 결제" value={`${detail.paymentSummary.totalPaidCount}건`} /><Summary label="보증금" value={`${detail.paymentSummary.depositPaidCount}건`} /><Summary label="잔액" value={`${detail.paymentSummary.balancePaidCount}건`} /><Summary label="결제 합계" value={formatAdminCurrency(detail.paymentSummary.totalPaidAmount)} /></div></div>
            </>
          )}
        </AdminModal>
      )}
    </div>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl bg-white p-3"><p className="text-xs text-slate-400">{label}</p><p className="mt-1 text-sm font-black text-slate-800">{value}</p></div>;
}
