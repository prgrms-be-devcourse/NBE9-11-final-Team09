import { BusType } from "@/types/seat";

interface SeatInfoPanelProps {
    routeInfo: string;          // 노선 정보
    busType: BusType;           // 버스 종류
    selectedSeatNumber: string | null; // 선택한 좌석 번호 (없으면 null)
    finalAmount: number | null; // 최종 결제 금액 (없으면 null)
    onPaymentClick: () => void; // 결제하기 버튼 클릭 핸들러
}

// 우측 선택 정보 확인 패널
export default function SeatInfoPanel({
                                          routeInfo,
                                          busType,
                                          selectedSeatNumber,
                                          finalAmount,
                                          onPaymentClick,
                                      }: SeatInfoPanelProps) {
    return (
        <div className="border border-gray-200 rounded-xl p-6 bg-white w-64">
            <h3 className="text-base font-semibold mb-4">선택 정보 확인</h3>

            {/* 노선 정보 */}
            <div className="flex justify-between mb-3">
                <span className="text-sm text-gray-500">노선 정보</span>
                <span className="text-sm font-medium">{routeInfo}</span>
            </div>

            {/* 버스 종류 */}
            <div className="flex justify-between mb-3">
                <span className="text-sm text-gray-500">버스 종류</span>
                <span className="text-sm font-medium">
          {busType === "BUS_25" ? "25인승 우등" : "45인승 일반"}
        </span>
            </div>

            {/* 선택한 좌석 */}
            <div className="flex justify-between mb-4">
                <span className="text-sm text-gray-500">선택한 좌석</span>
                <span className="text-sm font-medium text-red-500">
          {selectedSeatNumber ? `${selectedSeatNumber}번 좌석` : "미선택"}
        </span>
            </div>

            <hr className="mb-4" />

            {/* 최종 결제 금액 */}
            <div className="flex justify-between mb-6">
                <span className="text-sm font-semibold">최종 결제 금액</span>
                <span className="text-base font-bold">
        {finalAmount !== null
            ? `${finalAmount.toLocaleString()} 원`
            : "-"}
    </span>
            </div>

            {/* 결제하기 버튼 */}
            <button
                className={`w-full py-3 rounded-lg text-white font-semibold text-sm
          ${selectedSeatNumber
                    ? "bg-black cursor-pointer hover:bg-gray-800"
                    : "bg-gray-300 cursor-not-allowed"
                }`}
                onClick={onPaymentClick}
                disabled={!selectedSeatNumber} // 좌석 선택 안 하면 비활성화
            >
                결제하기 →
            </button>
        </div>
    );
}