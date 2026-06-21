"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { Seat, SeatLayout } from "@/types/seat";
import { getSeatLayout, holdSeat, releaseSeat } from "@/api/seat";
import SeatMap from "@/components/seat/SeatMap";
import SeatInfoPanel from "@/components/seat/SeatInfoPanel";
import CommonModal from "@/components/common/CommonModal";

export default function SeatsPage() {
    const params = useParams();
    const router = useRouter();
    const fundingId = Number(params.id);

    // 상태 관리
    const [seatLayout, setSeatLayout] = useState<SeatLayout | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [selectedSeat, setSelectedSeat] = useState<Seat | null>(null);
    const [returnSeat, setReturnSeat] = useState<Seat | null>(null);
    const [step, setStep] = useState<"outbound" | "return">("outbound");

    const [modal, setModal] = useState<{ title: string; message: string } | null>(null);

    // TODO: 펀딩 상세 API 연동 후 실제 데이터로 교체 필요
    const isRoundTrip = false;           // 왕복 여부 (임시)
    const routeInfo = "서울역 → 잠실";  // 노선 정보 (임시)
    const finalAmount = 25000;           // 최종 결제 금액 (임시) - FundingDetailResponse.minPrice ~ maxPrice 범위로 교체 예정
    const pathId = fundingId;            // TODO: 펀딩 상세 API에서 pathId 받아야 함

    // 좌석 배치도 조회
    useEffect(() => {
        async function fetchSeatLayout() {
            try {
                setLoading(true);
                const data = await getSeatLayout(pathId);
                setSeatLayout(data);
            } catch (_err) {
                setError("좌석 정보를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        }

        void fetchSeatLayout();
    }, [pathId]);

    // 좌석 클릭 핸들러
    async function handleSeatClick(seat: Seat) {
        // BOOKED → 아무 반응 없음
        if (seat.status === "BOOKED") return;

        // HOLD (다른 사람 선점 중) → 팝업
        if (seat.status === "HOLD" && !seat.mySeat) {
            setModal({
                title: "선택 불가",
                message: "이미 다른 사람이 선점한 좌석입니다. 새로고침 후 다시 시도해주세요.",
            });
            return;
        }

        // 내가 선점한 좌석 클릭 → 취소
        if (seat.mySeat) {
            try {
                await releaseSeat(seat.seatId);
                setSeatLayout((prev) =>
                    prev
                        ? {
                            ...prev,
                            seats: prev.seats.map((s) =>
                                s.seatId === seat.seatId
                                    ? { ...s, status: "AVAILABLE" as const, mySeat: false }
                                    : s
                            ),
                        }
                        : prev
                );
                if (step === "outbound") setSelectedSeat(null);
                else setReturnSeat(null);
            } catch (_err) {
                setModal({ title: "오류", message: "좌석 취소에 실패했습니다." });
            }
            return;
        }

        try {
            const updatedSeat = await holdSeat(seat.seatId);
            setSeatLayout((prev) =>
                prev
                    ? {
                        ...prev,
                        seats: prev.seats.map((s) =>
                            s.seatId === seat.seatId
                                ? { ...s, status: "HOLD" as const, mySeat: true }
                                : s
                        ),
                    }
                    : prev
            );
            if (step === "outbound") setSelectedSeat(updatedSeat);
            else setReturnSeat(updatedSeat);
        } catch (err: unknown) {
            if (err instanceof Error && err.message === "ALREADY_HELD") {
                setModal({
                    title: "선택 불가",
                    message: "이미 다른 사람이 선점한 좌석입니다. 새로고침 후 다시 시도해주세요.",
                });
            } else {
                setModal({ title: "오류", message: "좌석 선점에 실패했습니다." });
            }
        }
    }

    // 결제하기 버튼 클릭 핸들러
    function handlePaymentClick() {
        if (isRoundTrip && step === "outbound") {
            setStep("return");
            return;
        }
        router.push(`/funding/${fundingId}/payment`);
    }

    // 로딩 / 에러 처리
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

                {/* 선택 정보 확인 패널 */}
                <SeatInfoPanel
                    routeInfo={routeInfo}
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