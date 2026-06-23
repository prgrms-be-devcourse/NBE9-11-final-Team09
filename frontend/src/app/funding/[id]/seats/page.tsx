"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import type { Seat, SeatLayout, FundingDetail } from "@/types/funding";
import { getFunding, getSeatLayout, holdSeat, createParticipation } from "@/lib/fundingApi";
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

                const funding: FundingDetail = await getFunding(fundingId);

                // 왕복 여부 확인
                const roundTrip = funding.tripType === "ROUND";
                setIsRoundTrip(roundTrip);

                // 가는편 노선
                const outbound = funding.pathinfos.find((p) => p.direction === "OUTBOUND");
                // 오는편 노선
                const returnPath = funding.pathinfos.find((p) => p.direction === "RETURN");

                if (!outbound) {
                    setError("노선 정보를 찾을 수 없습니다.");
                    return;
                }

                if (returnPath) setReturnPathId(returnPath.pathinfoId);

                setOutboundRouteInfo(`${outbound.departureAddress} → ${outbound.arrivalAddress}`);
                if (returnPath) {
                    setReturnRouteInfo(`${returnPath.departureAddress} → ${returnPath.arrivalAddress}`);
                }

                const perPerson = funding.currentParticipants > 0
                    ? Math.ceil(Number(funding.totalPrice) / funding.currentParticipants)
                    : Number(funding.minPrice);
                setFinalAmount(perPerson);

                const layout = await getSeatLayout(outbound.pathinfoId);
                setSeatLayout(layout);

            } catch (err) {
                setError("정보를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, [fundingId, isValidId]);

    async function handleSeatClick(seat: Seat) {
        if (seat.status === "BOOKED") return;

        if (seat.status === "HOLD" && !seat.mySeat) {
            setModal({
                title: "이미 선택된 좌석입니다",
                message: "다른 분이 먼저 선택했어요. 잠시 후 새로고침하여 다시 시도해주세요.",
            });
            return;
        }

        try {
            const updatedSeat = await holdSeat(seat.seatId);

            setSeatLayout((prev) =>
                prev ? {
                    ...prev,
                    seats: prev.seats.map((s) =>
                        s.seatId === seat.seatId
                            ? { ...s, status: "HOLD" as const, mySeat: true }
                            : s
                    ),
                } : prev
            );

            if (step === "outbound") setSelectedSeat(updatedSeat);
            else setReturnSeat(updatedSeat);

        } catch (err: unknown) {
            if (err instanceof Error && err.message === "ALREADY_HELD") {
                setModal({
                    title: "이미 선택된 좌석입니다",
                    message: "다른 분이 먼저 선택했어요. 잠시 후 새로고침하여 다시 시도해주세요.",
                });
            } else {
                setModal({ title: "오류", message: "좌석 선점에 실패했습니다." });
            }
        }
    }

    async function handlePaymentClick() {
        // 좌석 미선택 검증
        if (step === "outbound" && !selectedSeat) {
            setModal({
                title: "좌석을 선택해주세요",
                message: "가는편 좌석을 먼저 선택해주세요.",
            });
            return;
        }

        if (step === "return" && !returnSeat) {
            setModal({
                title: "좌석을 선택해주세요",
                message: "오는편 좌석을 먼저 선택해주세요.",
            });
            return;
        }

        // 왕복이고 가는편만 선택한 경우 -> 오는편 좌석 선택 단계로
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
                setModal({ title: "오류", message: "오는편 좌석 정보를 불러오는데 실패했습니다." });
            }
            return;
        }

        // 참여 신청 — 좌석 확정 후 participationId 발급 
        try {
            const participation = await createParticipation(
                fundingId,
                selectedSeat!.seatId,
                returnSeat?.seatId ?? null
            );
            router.push(`/payment/${participation.participationId}`);
        } catch (err) {
            setModal({
                title: "참여 신청 실패",
                message: err instanceof Error ? err.message : "참여 신청에 실패했습니다. 다시 시도해주세요.",
            });
        }
    }

    if (!isValidId) return <div className="flex justify-center p-10 text-red-500">잘못된 접근입니다.</div>;
    if (loading) return <div className="flex justify-center p-10">로딩 중...</div>;
    if (error) return <div className="flex justify-center p-10 text-red-500">{error}</div>;
    if (!seatLayout) return null;

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            {/* 왕복 단계 표시 */}
            {isRoundTrip && (
                <div className="text-center mb-4 text-sm font-medium text-gray-600">
                    {step === "outbound" ? "1단계: 가는편 좌석 선택" : "2단계: 오는편 좌석 선택"}
                </div>
            )}

            <div className="flex gap-6 justify-center">
                {/* 좌석 배치도 */}
                <SeatMap
                    busType={seatLayout.busType}
                    seats={seatLayout.seats}
                    onSeatClick={handleSeatClick}
                />

                {/* 선택 정보 확인 패널 - 단계별 노선 정보 표시 */}
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

            {/* 공통 모달 */}
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