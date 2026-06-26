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
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center max-w-sm w-full px-6">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
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
          <h1 className="text-xl font-bold text-gray-900 mb-2">
            결제가 취소되었습니다
          </h1>
          <p className="text-gray-500 text-sm mb-1">
            {message || "결제 과정에서 오류가 발생했습니다."}
          </p>
          {code && (
              <p className="text-gray-400 text-xs mb-6">오류 코드: {code}</p>
          )}
          <button
              onClick={() => router.back()}
              className="w-full bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 rounded-xl transition-colors text-sm"
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
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
              <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
            </div>
          }
      >
        <PaymentFailContent />
      </Suspense>
  );
}