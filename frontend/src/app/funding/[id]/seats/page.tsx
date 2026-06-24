"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import type { Seat, SeatLayout, FundingDetail } from "@/types/funding";
import {
    getFunding,
    getSeatLayout,
    holdSeat,
    createParticipation,
} from "@/lib/fundingApi";
import SeatMap from "@/components/seat/SeatMap";
import SeatInfoPanel from "@/components/seat/SeatInfoPanel";
import CommonModal from "@/components/common/CommonModal";

export default function SeatsPage() {
    const params = useParams();
    const router = useRouter();

    const fundingId = Number(params.id);
    const isValidId = !isNaN(fundingId);

    const [seatLayout, setSeatLayout] = useState<SeatLayout | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [selectedSeat, setSelectedSeat] = useState<Seat | null>(null);
    const [returnSeat, setReturnSeat] = useState<Seat | null>(null);
    const [step, setStep] = useState<"outbound" | "return">("outbound");

    const [modal, setModal] = useState<{ title: string; message: string } | null>(null);

    const [isRoundTrip, setIsRoundTrip] = useState(false);
    const [outboundRouteInfo, setOutboundRouteInfo] = useState("");
    const [returnRouteInfo, setReturnRouteInfo] = useState("");
    const [finalAmount, setFinalAmount] = useState(0);
    const [returnPathId, setReturnPathId] = useState<number | null>(null);

    useEffect(() => {
        if (!isValidId) return;

        async function fetchData() {
            try {
                setLoading(true);
                setError(null);

                const funding: FundingDetail = await getFunding(fundingId);

                const roundTrip = funding.tripType === "ROUND";
                setIsRoundTrip(roundTrip);

                const outbound = funding.pathinfos.find((p) => p.direction === "OUTBOUND");
                const returnPath = funding.pathinfos.find((p) => p.direction === "RETURN");

                if (!outbound) {
                    setError("노선 정보를 찾을 수 없습니다.");
                    return;
                }

                if (returnPath) {
                    setReturnPathId(returnPath.pathinfoId);
                    setReturnRouteInfo(`${returnPath.departureAddress} → ${returnPath.arrivalAddress}`);
                }

                setOutboundRouteInfo(`${outbound.departureAddress} → ${outbound.arrivalAddress}`);

                const perPerson =
                    funding.currentParticipants > 0
                        ? Math.ceil(Number(funding.totalPrice) / funding.currentParticipants)
                        : Number(funding.minPrice);

                setFinalAmount(perPerson);

                const layout = await getSeatLayout(outbound.pathinfoId);
                setSeatLayout(layout);
            } catch {
                setError("정보를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        }

        void fetchData();
    }, [fundingId, isValidId]);

    function handleSeatClick(seat: Seat) {
        if (seat.status === "BOOKED") return;

        if (seat.status === "HOLD" && !seat.mySeat) {
            setModal({
                title: "이미 선택된 좌석입니다",
                message: "다른 분이 먼저 선택했어요. 잠시 후 새로고침하여 다시 시도해주세요.",
            });
            return;
        }

        if (step === "outbound") {
            setSelectedSeat(seat);
        } else {
            setReturnSeat(seat);
        }
    }

    async function handlePaymentClick() {
        if (step === "outbound" && !selectedSeat) {
            setModal({ title: "좌석을 선택해주세요", message: "가는편 좌석을 먼저 선택해주세요." });
            return;
        }

        if (step === "return" && !returnSeat) {
            setModal({ title: "좌석을 선택해주세요", message: "오는편 좌석을 먼저 선택해주세요." });
            return;
        }

        if (isRoundTrip && step === "outbound") {
            if (!returnPathId) {
                setModal({ title: "오류", message: "오는편 노선 정보를 찾을 수 없습니다." });
                return;
            }

            try {
                const returnLayout = await getSeatLayout(returnPathId);
                setSeatLayout(returnLayout);
                setStep("return");
            } catch {
                setModal({ title: "오류", message: "오는편 좌석 정보를 불러오지 못했습니다." });
            }

            return;
        }

        try {
            // 가는편 HOLD
            await holdSeat(selectedSeat!.seatId);

            // 왕복이면 오는편도 HOLD
            if (isRoundTrip && returnSeat) {
                await holdSeat(returnSeat.seatId);
            }

            const { participationId } = await createParticipation(
                fundingId,
                selectedSeat!.seatId,
                returnSeat?.seatId ?? null
            );

            const seatInfo =
                isRoundTrip && returnSeat
                    ? `가는편 ${selectedSeat!.seatNumber}석 · 오는편 ${returnSeat.seatNumber}석`
                    : `${selectedSeat!.seatNumber}석`;

            sessionStorage.setItem(
                `paymentContext_${participationId}`,
                JSON.stringify({ fundingId, seatInfo, amount: finalAmount })
            );

            router.push(`/payment/${participationId}`);
        } catch (err: unknown) {
            if (err instanceof Error && err.message === "ALREADY_HELD") {
                setModal({
                    title: "이미 선택된 좌석입니다",
                    message: "다른 분이 먼저 선점했어요. 다른 좌석을 선택해주세요.",
                });

                setSelectedSeat(null);
                setReturnSeat(null);
                setStep("outbound");
            } else if (err instanceof Error) {
                setModal({ title: "참여 신청 실패", message: err.message });
            } else {
                setModal({ title: "오류", message: "참여 신청에 실패했습니다." });
            }
        }
    }

    if (!isValidId) {
        return <div className="flex justify-center p-10 text-red-500">잘못된 접근입니다.</div>;
    }

    if (loading) {
        return <div className="flex justify-center p-10">로딩 중...</div>;
    }

    if (error) {
        return <div className="flex justify-center p-10 text-red-500">{error}</div>;
    }

    if (!seatLayout) return null;

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            {isRoundTrip && (
                <div className="mb-4 text-center text-sm font-medium text-gray-600">
                    {step === "outbound" ? "1단계: 가는편 좌석 선택" : "2단계: 오는편 좌석 선택"}
                </div>
            )}

            <div className="flex justify-center gap-6">
                <SeatMap
                    busType={seatLayout.busType}
                    seats={seatLayout.seats}
                    selectedSeatId={
                        step === "outbound"
                            ? selectedSeat?.seatId ?? null
                            : returnSeat?.seatId ?? null
                    }
                    onSeatClick={handleSeatClick}
                />

                <SeatInfoPanel
                    routeInfo={step === "outbound" ? outboundRouteInfo : returnRouteInfo}
                    busType={seatLayout.busType}
                    selectedSeatNumber={
                        step === "outbound"
                            ? selectedSeat?.seatNumber ?? null
                            : returnSeat?.seatNumber ?? null
                    }
                    finalAmount={finalAmount}
                    onPaymentClick={handlePaymentClick}
                />
            </div>

            {modal && (
                <CommonModal
                    title={modal.title}
                    message={modal.message}
                    onClose={() => setModal(null)}
                />
            )}
        </div>
    );
}