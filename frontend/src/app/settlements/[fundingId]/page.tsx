"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { getSettlement, type SettlementResponse } from "@/lib/payment-api";
import { formatMoney } from "@/lib/fundingFormat";

const STATUS_LABEL: Record<SettlementResponse["status"], string> = {
  CALCULATED: "정산 대기",
  APPROVED: "정산 승인",
  REJECTED: "정산 거절",
  COMPLETED: "정산 완료",
};

const STATUS_COLOR: Record<SettlementResponse["status"], string> = {
  CALCULATED: "bg-yellow-100 text-yellow-700",
  APPROVED: "bg-[#eef5ea] text-[#426f55]",
  REJECTED: "bg-red-100 text-red-700",
  COMPLETED: "bg-green-100 text-green-700",
};

function formatKoreanDateTime(value: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: "Asia/Seoul",
  }).format(date);
}

export default function SettlementPage() {
  const params = useParams<{ fundingId: string }>();
  const router = useRouter();
  const fundingId = Number(params.fundingId);

  const [settlement, setSettlement] = useState<SettlementResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!Number.isFinite(fundingId)) {
      setError("잘못된 접근입니다.");
      setLoading(false);
      return;
    }

    getSettlement(fundingId)
      .then(setSettlement)
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [fundingId]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={() => router.back()}
            className="text-sm font-semibold text-[#426f55] underline"
          >
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  if (!settlement) return null;

  const rows: { label: string; value: string }[] = [
    { label: "총 정산 금액", value: `${formatMoney(Number(settlement.totalAmount))}원` },
    { label: "플랫폼 수수료", value: `${formatMoney(Number(settlement.platformFee))}원` },
    { label: "호스트 지급액", value: `${formatMoney(Number(settlement.hostPaybackAmount))}원` },
    {
      label: "지급 보류",
      value: settlement.paybackHold ? "보류 중" : "정상",
    },
    {
      label: "지급 완료 일시",
      value: formatKoreanDateTime(settlement.paybackPaidAt),
    },
    {
      label: "정산 생성일",
      value: formatKoreanDateTime(settlement.createdAt),
    },
  ];

  return (
    <main className="min-h-screen bg-[#f3f7f1]">
      <div className="mx-auto max-w-2xl px-4 py-8">
        <button
          onClick={() => router.back()}
          className="mb-6 flex items-center gap-1 text-sm font-semibold text-slate-500 hover:text-[#426f55]"
        >
          ← 돌아가기
        </button>

        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-slate-950">정산 현황</h1>
          <span
            className={`text-sm font-semibold px-3 py-1 rounded-full ${STATUS_COLOR[settlement.status]}`}
          >
            {STATUS_LABEL[settlement.status]}
          </span>
        </div>

        <div className="rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
          <dl className="divide-y divide-[#dbe7dc]">
            {rows.map(({ label, value }) => (
              <div key={label} className="flex justify-between py-3 text-sm">
                <dt className="text-slate-500">{label}</dt>
                <dd className="font-semibold text-slate-900">{value}</dd>
              </div>
            ))}
          </dl>
        </div>

        {settlement.status === "COMPLETED" && (
          <div className="mt-4 bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700">
            정산이 완료되어 호스트에게 지급되었습니다.
          </div>
        )}

        {settlement.status === "REJECTED" && (
          <div className="mt-4 bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
            정산이 거절되었습니다. 관리자에게 문의해 주세요.
          </div>
        )}

        {settlement.paybackHold && settlement.status !== "REJECTED" && (
          <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-700">
            지급이 일시 보류 중입니다. 확인 후 처리됩니다.
          </div>
        )}
        </div>
    </main>
  );
}
