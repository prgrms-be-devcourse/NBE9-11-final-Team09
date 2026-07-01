"use client";

import { getAdminStatistics } from "@/lib/admin-api";
import type { AdminStatistics } from "@/types/admin";
import { useEffect, useState } from "react";
import { AdminLoading, formatAdminCurrency } from "./admin-ui";

export default function OverviewPanel({ onError }: { onError: (error: unknown) => void }) {
  const [statistics, setStatistics] = useState<AdminStatistics | null>(null);

  useEffect(() => {
    let cancelled = false;
    getAdminStatistics()
      .then((result) => {
        if (!cancelled) setStatistics(result);
      })
      .catch((error) => {
        if (!cancelled) onError(error);
      });
    return () => {
      cancelled = true;
    };
  }, [onError]);

  if (!statistics) return <AdminLoading />;

  const primaryCards = [
    { label: "전체 회원", value: statistics.totalUsers.toLocaleString("ko-KR"), unit: "명", tone: "bg-[#173452] text-white" },
    { label: "진행 중 펀딩", value: statistics.activeFundings.toLocaleString("ko-KR"), unit: "건", tone: "bg-[#2b6b88] text-white" },
    { label: "누적 결제 금액", value: formatAdminCurrency(statistics.totalPaymentAmount), unit: "", tone: "bg-white text-slate-950" },
    { label: "대기 정산", value: statistics.pendingSettlements.toLocaleString("ko-KR"), unit: "건", tone: "bg-[#e5f4f5] text-[#173452]" },
  ];

  return (
    <div className="mx-auto max-w-7xl space-y-7">
      <section>
        <p className="text-sm font-bold text-[#2b6b88]">OVERVIEW</p>
        <div className="mt-2 flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
          <div>
            <h2 className="text-3xl font-black tracking-tight">서비스 현황</h2>
            <p className="mt-2 text-sm text-slate-500">현재 모여타의 주요 운영 지표입니다.</p>
          </div>
          <p className="text-xs text-slate-400">실시간 집계 기준</p>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {primaryCards.map((card) => (
          <article key={card.label} className={`rounded-3xl p-6 shadow-sm ring-1 ring-slate-900/5 ${card.tone}`}>
            <p className="text-sm font-semibold opacity-65">{card.label}</p>
            <p className="mt-5 text-3xl font-black tracking-tight">
              {card.value}<span className="ml-1 text-sm font-bold opacity-60">{card.unit}</span>
            </p>
          </article>
        ))}
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <article className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/80 sm:p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-bold tracking-widest text-[#2b6b88]">MEMBERS</p>
              <h3 className="mt-2 text-lg font-black">회원 상태</h3>
            </div>
            <span className="text-xs text-slate-400">전체 {statistics.totalUsers}명</span>
          </div>
          <div className="mt-7 grid grid-cols-2 gap-4">
            <Metric label="활성 회원" value={statistics.activeUsers} tone="text-emerald-700 bg-emerald-50" />
            <Metric label="탈퇴 회원" value={statistics.withdrawnUsers} tone="text-rose-700 bg-rose-50" />
          </div>
        </article>

        <article className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/80 sm:p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-bold tracking-widest text-[#2b6b88]">FUNDINGS</p>
              <h3 className="mt-2 text-lg font-black">펀딩 상태</h3>
            </div>
            <span className="text-xs text-slate-400">운영 현황</span>
          </div>
          <div className="mt-7 grid grid-cols-3 gap-3">
            <Metric label="진행" value={statistics.activeFundings} tone="text-blue-700 bg-blue-50" />
            <Metric label="완료" value={statistics.completedFundings} tone="text-emerald-700 bg-emerald-50" />
            <Metric label="취소" value={statistics.cancelledFundings} tone="text-rose-700 bg-rose-50" />
          </div>
        </article>
      </section>

      {statistics.pendingReports > 0 && (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 px-6 py-5 text-sm text-amber-800">
          처리 대기 중인 신고가 <strong>{statistics.pendingReports}건</strong> 있습니다. 신고 관리 기능은 후속 기능으로 제공됩니다.
        </section>
      )}
    </div>
  );
}

function Metric({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div className={`rounded-2xl p-4 ${tone}`}>
      <p className="text-xs font-bold opacity-70">{label}</p>
      <p className="mt-2 text-2xl font-black">{value.toLocaleString("ko-KR")}</p>
    </div>
  );
}
