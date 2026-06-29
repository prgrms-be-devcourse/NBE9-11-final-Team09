"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { cancelParticipation } from "@/lib/fundingApi";

function PaymentFailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const code = searchParams.get("code");
  const message = searchParams.get("message");
  const participationId = searchParams.get("participationId");

  useEffect(() => {
    if (participationId) {
      cancelParticipation(Number(participationId)).catch((err) => {
        console.warn("참여 취소 실패:", err);
      });
    }
  }, [participationId]);

  return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1] px-5">
        <div className="w-full max-w-sm rounded-xl border border-[#dbe7dc] bg-white p-6 text-center shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
            <svg
                className="w-8 h-8 text-red-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
            >
              <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </div>
          <h1 className="mb-2 text-xl font-bold text-slate-950">
            결제가 취소되었습니다
          </h1>
          <p className="mb-1 text-sm text-slate-500">
            {message || "결제 과정에서 오류가 발생했습니다."}
          </p>
          {code && (
              <p className="mb-6 text-xs text-slate-400">오류 코드: {code}</p>
          )}
          <button
              onClick={() => router.back()}
              className="w-full rounded-xl border border-[#dbe7dc] bg-white py-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-[#eef5ea]"
          >
            돌아가기
          </button>
        </div>
      </div>
  );
}

export default function PaymentFailPage() {
  return (
      <Suspense
          fallback={
            <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
            </div>
          }
      >
        <PaymentFailContent />
      </Suspense>
  );
}
