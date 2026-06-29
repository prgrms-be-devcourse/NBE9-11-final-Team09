"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { confirmBalance } from "@/lib/payment-api";

function BalanceResultContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const paymentKey = searchParams.get("paymentKey");
  const amount = searchParams.get("amount");
  const participationId = searchParams.get("participationId");

  const [status, setStatus] = useState<"pending" | "success" | "error">(
    "pending",
  );
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (!paymentKey || !amount || !participationId) {
      setStatus("error");
      setErrorMessage("결제 정보가 올바르지 않습니다.");
      return;
    }

    confirmBalance({
      paymentKey,
      amount: Number(amount),
      participationId: Number(participationId),
    })
      .then(() => setStatus("success"))
      .catch((err: Error) => {
        setStatus("error");
        setErrorMessage(err.message);
      });
  }, [paymentKey, amount, participationId]);

  if (status === "pending") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="text-center">
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
          <p className="text-sm text-slate-600">잔금 결제 처리 중...</p>
        </div>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1] px-5">
        <div className="w-full max-w-sm rounded-xl border border-[#dbe7dc] bg-white p-6 text-center shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
            <svg className="w-8 h-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h1 className="mb-2 text-xl font-bold text-slate-950">결제에 실패했습니다</h1>
          <p className="mb-6 text-sm text-slate-500">{errorMessage}</p>
          <button
            onClick={() => router.back()}
            className="w-full rounded-xl bg-[#4f7a61] py-3 text-sm font-semibold text-white transition-colors hover:bg-[#426f55]"
          >
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1] px-5">
      <div className="w-full max-w-sm rounded-xl border border-[#dbe7dc] bg-white p-6 text-center shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <svg className="w-8 h-8 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h1 className="mb-2 text-xl font-bold text-slate-950">잔금 결제 완료!</h1>
        <p className="mb-6 text-sm text-slate-500">
          잔금이 성공적으로 처리되었습니다. 이제 탑승 준비가 완료되었습니다.
        </p>
        <div className="space-y-2">
          <button
            onClick={() => router.push("/fundings")}
            className="w-full rounded-xl bg-[#4f7a61] py-3 text-sm font-semibold text-white transition-colors hover:bg-[#426f55]"
          >
            펀딩 목록으로
          </button>
          <button
            onClick={() => router.push("/mypage")}
            className="w-full rounded-xl border border-[#dbe7dc] bg-white py-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-[#eef5ea]"
          >
            마이페이지에서 확인
          </button>
        </div>
      </div>
    </div>
  );
}

export default function BalanceResultPage() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
      </div>
    }>
      <BalanceResultContent />
    </Suspense>
  );
}
