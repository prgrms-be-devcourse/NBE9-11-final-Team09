"use client";

import { Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";

function BalanceFailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const code = searchParams.get("code");
  const message = searchParams.get("message");
  const participationId = searchParams.get("participationId");

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1] px-5">
      <div className="w-full max-w-sm rounded-xl border border-[#dbe7dc] bg-white p-6 text-center shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
          <svg className="w-8 h-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h1 className="mb-2 text-xl font-bold text-slate-950">잔금 결제가 취소되었습니다</h1>
        <p className="mb-1 text-sm text-slate-500">
          {message || "결제 과정에서 오류가 발생했습니다."}
        </p>
        {code && <p className="mb-6 text-xs text-slate-400">오류 코드: {code}</p>}
        <button
          onClick={() =>
            participationId
              ? router.push(`/payment/balance/${participationId}`)
              : router.back()
          }
          className="w-full rounded-xl bg-[#4f7a61] py-3 text-sm font-semibold text-white transition-colors hover:bg-[#426f55]"
        >
          다시 시도하기
        </button>
      </div>
    </div>
  );
}

export default function BalanceFailPage() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
      </div>
    }>
      <BalanceFailContent />
    </Suspense>
  );
}
