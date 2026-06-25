"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import { getMyProfile } from "@/lib/member-api";
import { getFunding } from "@/lib/fundingApi";
import { preparePayment } from "@/lib/payment-api";
import { formatMoney, formatDateTime } from "@/lib/fundingFormat";
import { getFundingMemberId } from "@/lib/fundingAuth";
import type { MemberProfile } from "@/types/member";
import type { FundingDetail } from "@/types/funding";

const CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY!;

type BalanceContext = {
  fundingId: number;
  seatInfo: string;
  amount: number;
};

export default function BalancePaymentPage() {
  const params = useParams<{ participationId: string }>();
  const router = useRouter();
  const participationId = Number(params.participationId);

  const [funding, setFunding] = useState<FundingDetail | null>(null);
  const [member, setMember] = useState<MemberProfile | null>(null);
  const [context, setContext] = useState<BalanceContext | null>(null);
  const [allAgreed, setAllAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [refundAgreed, setRefundAgreed] = useState(false);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const raw = sessionStorage.getItem(`balanceContext_${participationId}`);
    if (!raw) {
      setError("결제 정보를 찾을 수 없습니다. 마이페이지에서 다시 시도해 주세요.");
      return;
    }

    let ctx: BalanceContext;
    try {
      ctx = JSON.parse(raw) as BalanceContext;
    } catch {
      setError("결제 정보가 올바르지 않습니다.");
      return;
    }
    setContext(ctx);

    Promise.all([getFunding(ctx.fundingId), getMyProfile()])
      .then(([f, m]) => {
        setFunding(f);
        setMember(m);
      })
      .catch(() => setError("정보를 불러오지 못했습니다."));
  }, [participationId]);

  function toggleAll(checked: boolean) {
    setAllAgreed(checked);
    setPrivacyAgreed(checked);
    setRefundAgreed(checked);
  }

  function handlePrivacy(checked: boolean) {
    setPrivacyAgreed(checked);
    setAllAgreed(checked && refundAgreed);
  }

  function handleRefund(checked: boolean) {
    setRefundAgreed(checked);
    setAllAgreed(privacyAgreed && checked);
  }

  async function handlePay() {
    if (!privacyAgreed || !refundAgreed) {
      alert("필수 약관에 동의해 주세요.");
      return;
    }
    if (!context) return;

    setPaying(true);
    try {
      const { orderId, amount } = await preparePayment(participationId);
      const memberId = getFundingMemberId();
      const tossPayments = await loadTossPayments(CLIENT_KEY);
      const payment = tossPayments.payment({
        customerKey: memberId ? String(memberId) : ANONYMOUS,
      });

      await payment.requestPayment({
        method: "CARD",
        amount: { currency: "KRW", value: amount },
        orderId,
        orderName: "모여타 버스 잔금 결제",
        successUrl: `${window.location.origin}/payment/balance/result?participationId=${participationId}`,
        failUrl: `${window.location.origin}/payment/balance/fail?participationId=${participationId}`,
        card: {
          useEscrow: false,
          flowMode: "DEFAULT",
          useCardPoint: false,
          useAppCardOnly: false,
        },
      });
    } catch (err) {
      setPaying(false);
      setError(
        err instanceof Error ? err.message : "결제 처리 중 오류가 발생했습니다.",
      );
    }
  }

  const departurePathinfo = funding?.pathinfos?.find(
    (p) => p.direction === "OUTBOUND",
  );

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={() => router.back()}
            className="text-blue-600 underline text-sm"
          >
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  if (!funding || !member || !context) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 py-8">
        <button
          onClick={() => router.back()}
          className="flex items-center gap-1 text-gray-500 hover:text-gray-700 text-sm mb-6"
        >
          ← 돌아가기
        </button>

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">잔금 결제</h1>
          <p className="text-sm text-gray-500 mt-1">
            펀딩이 확정되어 잔금 결제가 필요합니다.
          </p>
        </div>

        <div className="flex flex-col md:flex-row gap-6 items-start">
          {/* Left column */}
          <div className="flex-1 space-y-4">
            {/* Booking info */}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="text-base font-semibold text-gray-800 mb-4">
                결제 정보
              </h2>
              <div className="space-y-3 text-sm divide-y divide-gray-100">
                <div className="flex justify-between pb-3">
                  <span className="text-gray-500">펀딩 상품</span>
                  <span className="font-medium text-gray-900 text-right max-w-[60%]">
                    {funding.title}
                  </span>
                </div>

                {departurePathinfo && (
                  <div className="flex justify-between py-3">
                    <span className="text-gray-500">출발 일시</span>
                    <span className="font-medium text-gray-900">
                      {formatDateTime(departurePathinfo.departureTime)}
                    </span>
                  </div>
                )}

                <div className="flex justify-between py-3">
                  <span className="text-gray-500">탑승 좌석</span>
                  <span className="font-medium text-gray-900">
                    {context.seatInfo}번 좌석
                  </span>
                </div>

                <div className="pt-3 space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-500">예약자명</span>
                    <span className="font-medium text-gray-900">
                      {member.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">연락처</span>
                    <span className="font-medium text-gray-900">
                      {member.phoneNumber || "-"}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Terms */}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="text-base font-semibold text-gray-800 mb-4">
                이용약관 동의
              </h2>
              <div className="space-y-3">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={allAgreed}
                    onChange={(e) => toggleAll(e.target.checked)}
                    className="w-4 h-4 accent-blue-600"
                  />
                  <span className="font-semibold text-gray-800 text-sm">
                    전체 동의
                  </span>
                </label>
                <hr className="border-gray-100" />
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={privacyAgreed}
                    onChange={(e) => handlePrivacy(e.target.checked)}
                    className="w-4 h-4 accent-blue-600"
                  />
                  <span className="text-gray-700 text-sm">
                    개인정보 제3자 제공 동의{" "}
                    <span className="text-red-500">(필수)</span>
                  </span>
                </label>
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={refundAgreed}
                    onChange={(e) => handleRefund(e.target.checked)}
                    className="w-4 h-4 accent-blue-600"
                  />
                  <span className="text-gray-700 text-sm">
                    취소 및 환불 규정 동의{" "}
                    <span className="text-red-500">(필수)</span>
                  </span>
                </label>
              </div>
            </div>
          </div>

          {/* Right column */}
          <div className="md:w-80 w-full">
            <div className="bg-white rounded-xl border border-gray-200 p-6 sticky top-4">
              <h2 className="text-base font-semibold text-gray-800 mb-4">
                결제 수단 선택
              </h2>

              <div className="border-2 border-blue-500 rounded-lg p-4 flex items-center gap-3 mb-5">
                <div className="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  T
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-sm text-gray-900">
                    토스페이
                  </div>
                  <div className="text-xs text-gray-400">간편결제</div>
                </div>
                <div className="w-4 h-4 rounded-full bg-blue-500" />
              </div>

              <div className="border-t border-gray-100 pt-4 mb-5">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-500">잔금 결제 금액</span>
                  <span className="text-xl font-bold text-gray-900">
                    {formatMoney(context.amount)}원
                  </span>
                </div>
              </div>

              <button
                onClick={handlePay}
                disabled={paying}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-semibold py-3.5 rounded-xl transition-colors text-sm"
              >
                {paying ? "결제 진행 중..." : "토스페이로 잔금 결제하기"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
