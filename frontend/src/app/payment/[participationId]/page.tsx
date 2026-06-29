"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import { getMyProfile } from "@/lib/member-api";
import { getFunding, cancelParticipation  } from "@/lib/fundingApi";
import { preparePayment } from "@/lib/payment-api";
import { formatMoney, formatDateTime } from "@/lib/fundingFormat";
import { getFundingMemberId } from "@/lib/fundingAuth";
import type { MemberProfile } from "@/types/member";
import type { FundingDetail } from "@/types/funding";

const CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY!;

type PaymentContext = {
  fundingId: number;
  seatInfo: string;
  amount: number;
};

type TossMethod = "CARD" | "TRANSFER" | "VIRTUAL_ACCOUNT";

const PAYMENT_METHODS: { value: TossMethod; label: string; desc: string }[] = [
  { value: "CARD", label: "신용·체크카드", desc: "카드사 선택 후 결제" },
  { value: "TRANSFER", label: "계좌이체", desc: "실시간 계좌이체" },
  { value: "VIRTUAL_ACCOUNT", label: "가상계좌", desc: "무통장 입금 (24시간 내)" },
];

export default function PaymentPage() {
  const params = useParams<{ participationId: string }>();
  const router = useRouter();
  const participationId = Number(params.participationId);

  const [funding, setFunding] = useState<FundingDetail | null>(null);
  const [member, setMember] = useState<MemberProfile | null>(null);
  const [context, setContext] = useState<PaymentContext | null>(null);
  const [selectedMethod, setSelectedMethod] = useState<TossMethod>("CARD");
  const [allAgreed, setAllAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [refundAgreed, setRefundAgreed] = useState(false);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function fetchData() {
      const raw = sessionStorage.getItem(`paymentContext_${participationId}`);
      if (!raw) {
        setError("결제 정보를 찾을 수 없습니다. 좌석 선택 페이지로 돌아가 주세요.");
        return;
      }
      let ctx: PaymentContext;
      try {
        ctx = JSON.parse(raw) as PaymentContext;
      } catch {
        setError("결제 정보가 올바르지 않습니다.");
        return;
      }
      try {
        const [f, m] = await Promise.all([
          getFunding(ctx.fundingId),
          getMyProfile(),
        ]);
        setContext(ctx);
        setFunding(f);
        setMember(m);
      } catch {
        setError("정보를 불러오지 못했습니다.");
      }
    }
    void fetchData();
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

  async function handleBack() {
    try {
      if (funding?.myPaymentStatus === "PENDING") {
        await cancelParticipation(participationId);
      }
    } catch (err) {
      console.warn("참여 취소 실패 (이미 취소됐거나 완료):", err);
    } finally {
      sessionStorage.removeItem(`paymentContext_${participationId}`);
      router.back();
    }
  }

  async function handlePay() {
    if (!privacyAgreed || !refundAgreed) {
      alert("필수 약관에 동의해 주세요.");
      return;
    }
    if (!context) return;

    setPaying(true);
    setError("");
    try {
      const { orderId, amount: serverAmount } = await preparePayment(participationId);
      const memberId = getFundingMemberId();
      const tossPayments = await loadTossPayments(CLIENT_KEY);
      const payment = tossPayments.payment({
        customerKey: memberId ? `MEMBER_${memberId}` : ANONYMOUS,
      });

      const common = {
        amount: { currency: "KRW" as const, value: serverAmount },
        orderId,
        orderName: "모여타 버스 예약 보증금",
        successUrl: `${window.location.origin}/payment/result?participationId=${participationId}`,
        failUrl: `${window.location.origin}/payment/fail?participationId=${participationId}`,
      };

      if (selectedMethod === "CARD") {
        await payment.requestPayment({
          ...common,
          method: "CARD",
          card: { useEscrow: false, flowMode: "DEFAULT", useCardPoint: false, useAppCardOnly: false },
        });
      } else if (selectedMethod === "TRANSFER") {
        await payment.requestPayment({
          ...common,
          method: "TRANSFER",
          transfer: { cashReceipt: { type: "소득공제" } },
        });
      } else if (selectedMethod === "VIRTUAL_ACCOUNT") {
        const due = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split("T")[0];
        await payment.requestPayment({
          ...common,
          method: "VIRTUAL_ACCOUNT",
          virtualAccount: { cashReceipt: { type: "소득공제" }, dueDate: due },
        });
      }
    } catch (err) {
      setPaying(false);
      setError(err instanceof Error ? err.message : "결제 처리 중 오류가 발생했습니다.");
    }
  }

  const departurePathinfo = funding?.pathinfos?.find((p) => p.direction === "OUTBOUND");

  if (error) {
    return (
        <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
          <div className="text-center max-w-sm px-4">
            <p className="text-red-500 mb-4 text-sm">{error}</p>
            <button onClick={handleBack} className="text-sm font-semibold text-[#426f55] underline">
              돌아가기
            </button>
          </div>
        </div>
    );
  }

  if (!funding || !member || !context) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f3f7f1]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4f7a61] border-t-transparent" />
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-[#f3f7f1]">
      <div className="mx-auto max-w-5xl px-4 py-8">
        <button
            onClick={handleBack}
          className="mb-6 flex items-center gap-1 text-sm font-semibold text-slate-500 hover:text-[#426f55]"
        >
          ← 돌아가기
        </button>

        <h1 className="mb-6 text-2xl font-bold text-slate-950">예약 및 결제 정보 확인</h1>

        <div className="flex flex-col items-start gap-6 md:flex-row">
          {/* Left column */}
          <div className="flex-1 space-y-4">
            {/* Booking info */}
            <div className="rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
              <h2 className="mb-4 text-base font-bold text-slate-900">예약 정보</h2>
              <div className="space-y-3 text-sm divide-y divide-gray-100">
                <div className="flex justify-between pb-3">
                  <span className="text-slate-500">신청 펀딩 상품</span>
                  <span className="max-w-[60%] text-right font-semibold text-slate-900">{funding.title}</span>
                </div>

                {departurePathinfo && (
                  <div className="flex justify-between py-3">
                    <span className="text-slate-500">출발 일시</span>
                    <span className="font-semibold text-slate-900">
                      {formatDateTime(departurePathinfo.departureTime)}
                    </span>
                  </div>
                )}

                <div className="flex justify-between py-3">
                  <span className="text-slate-500">선택한 탑승 좌석</span>
                  <span className="font-semibold text-slate-900">{context.seatInfo}번 좌석</span>
                </div>

                <div className="pt-3 space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-500">예약자명</span>
                    <span className="font-semibold text-slate-900">{member.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">연락처</span>
                    <span className="font-semibold text-slate-900">{member.phoneNumber || "-"}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Terms */}
            <div className="rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
              <h2 className="mb-4 text-base font-bold text-slate-900">이용약관 동의</h2>
              <div className="space-y-3">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={allAgreed}
                    onChange={(e) => toggleAll(e.target.checked)}
                    className="h-4 w-4 accent-[#4f7a61]"
                  />
                  <span className="text-sm font-semibold text-slate-800">전체 동의</span>
                </label>

                <hr className="border-gray-100" />

                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={privacyAgreed}
                    onChange={(e) => handlePrivacy(e.target.checked)}
                    className="h-4 w-4 accent-[#4f7a61]"
                  />
                  <span className="text-sm text-slate-700">
                    개인정보 제3자 제공 동의 <span className="text-red-500">(필수)</span>
                  </span>
                </label>

                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={refundAgreed}
                    onChange={(e) => handleRefund(e.target.checked)}
                    className="h-4 w-4 accent-[#4f7a61]"
                  />
                  <span className="text-sm text-slate-700">
                    취소 및 환불 규정 동의 <span className="text-red-500">(필수)</span>
                  </span>
                </label>
              </div>
            </div>
          </div>

          {/* Right column */}
          <div className="md:w-80 w-full">
            <div className="sticky top-4 space-y-5 rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
              <div>
                <h2 className="mb-3 text-base font-bold text-slate-900">결제 수단 선택</h2>
                <div className="space-y-2">
                  {PAYMENT_METHODS.map((m) => (
                    <button
                      key={m.value}
                      type="button"
                      onClick={() => setSelectedMethod(m.value)}
                      className={`w-full flex items-center gap-3 rounded-lg border-2 px-4 py-3 text-left transition-colors ${
                        selectedMethod === m.value
                          ? "border-[#4f7a61] bg-[#eef5ea]"
                          : "border-[#dbe7dc] hover:border-[#adc7b6]"
                      }`}
                    >
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold text-slate-900">{m.label}</div>
                        <div className="text-xs text-slate-400">{m.desc}</div>
                      </div>
                      <div
                        className={`w-4 h-4 rounded-full border-2 flex-shrink-0 ${
                          selectedMethod === m.value
                            ? "border-[#4f7a61] bg-[#4f7a61]"
                            : "border-slate-300"
                        }`}
                      />
                    </button>
                  ))}
                </div>
              </div>

              <div className="border-t border-gray-100 pt-4">
                <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500">최종 결제 금액</span>
                  <span className="text-xl font-bold text-slate-950">
                    {formatMoney(context.amount)}
                  </span>
                </div>
              </div>

              <button
                onClick={handlePay}
                disabled={paying}
                className="w-full rounded-xl bg-[#4f7a61] py-3.5 text-sm font-semibold text-white transition-colors hover:bg-[#426f55] disabled:cursor-not-allowed disabled:bg-slate-300"
              >
                {paying
                  ? "결제 진행 중..."
                  : `${PAYMENT_METHODS.find((m) => m.value === selectedMethod)?.label ?? ""}로 결제하기`}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
