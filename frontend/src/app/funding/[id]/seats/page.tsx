"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { Seat, SeatLayout } from "@/types/seat";
import { getSeatLayout, holdSeat } from "@/api/seat";
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


    // =============================================
// 임시 mock 데이터 (API 연동 전 UI 확인용)
// TODO: API 연동 후 아래 useEffect로 교체
// =============================================
    useEffect(() => {
        const mockData: SeatLayout = {
            pathId: 1,
            busType: "BUS_25",
            seats: [
                { seatId: 1, seatNumber: "1A", status: "AVAILABLE", mySeat: false },
                { seatId: 2, seatNumber: "1B", status: "BOOKED", mySeat: false },
                { seatId: 3, seatNumber: "1C", status: "AVAILABLE", mySeat: false },
                { seatId: 4, seatNumber: "2A", status: "AVAILABLE", mySeat: false },
                { seatId: 5, seatNumber: "2B", status: "HOLD", mySeat: false },
                { seatId: 6, seatNumber: "2C", status: "AVAILABLE", mySeat: false },
                { seatId: 7, seatNumber: "3A", status: "AVAILABLE", mySeat: false },
                { seatId: 8, seatNumber: "3B", status: "AVAILABLE", mySeat: false },
                { seatId: 9, seatNumber: "3C", status: "AVAILABLE", mySeat: false },
                { seatId: 10, seatNumber: "4A", status: "AVAILABLE", mySeat: false },
                { seatId: 11, seatNumber: "4B", status: "AVAILABLE", mySeat: false },
                { seatId: 12, seatNumber: "4C", status: "AVAILABLE", mySeat: false },
                { seatId: 13, seatNumber: "5A", status: "AVAILABLE", mySeat: false },
                { seatId: 14, seatNumber: "5B", status: "AVAILABLE", mySeat: false },
                { seatId: 15, seatNumber: "5C", status: "AVAILABLE", mySeat: false },
                { seatId: 16, seatNumber: "6A", status: "AVAILABLE", mySeat: false },
                { seatId: 17, seatNumber: "6B", status: "AVAILABLE", mySeat: false },
                { seatId: 18, seatNumber: "6C", status: "AVAILABLE", mySeat: false },
                { seatId: 19, seatNumber: "7A", status: "AVAILABLE", mySeat: false },
                { seatId: 20, seatNumber: "7B", status: "AVAILABLE", mySeat: false },
                { seatId: 21, seatNumber: "7C", status: "AVAILABLE", mySeat: false },
                { seatId: 22, seatNumber: "8A", status: "AVAILABLE", mySeat: false },
                { seatId: 23, seatNumber: "8B", status: "AVAILABLE", mySeat: false },
                { seatId: 24, seatNumber: "8C", status: "AVAILABLE", mySeat: false },
            ],
        };

        // setTimeout으로 감싸서 동기 setState 에러 방지
        setTimeout(() => {
            setSeatLayout(mockData);
            setLoading(false);
        }, 0);
    }, []);

    // 좌석 클릭 핸들러
    async function handleSeatClick(seat: Seat) {
        // BOOKED → 아무 반응 없음 (맨 먼저 체크!)
        if (seat.status === "BOOKED") return;

        // HOLD (다른 사람 선점 중) → 팝업 (두 번째 체크!)
        if (seat.status === "HOLD" && !seat.mySeat) {
            setModal({
                title: "선택 불가",
                message: "이미 다른 사람이 선점한 좌석입니다. 새로고침 후 다시 시도해주세요.",
            });
            return;
        }

        // AVAILABLE → 선점 시도
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