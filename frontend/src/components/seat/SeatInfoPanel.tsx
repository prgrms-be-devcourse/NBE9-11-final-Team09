import { BusType } from "@/types/funding";

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
        <div className="w-72 rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
            <h3 className="mb-4 text-base font-bold text-slate-950">선택 정보 확인</h3>

            {/* 노선 정보 */}
            <div className="flex justify-between mb-3">
                <span className="text-sm text-slate-500">노선 정보</span>
                <span className="text-right text-sm font-semibold text-slate-800">{routeInfo}</span>
            </div>

            {/* 버스 종류 */}
            <div className="flex justify-between mb-3">
                <span className="text-sm text-slate-500">버스 종류</span>
                <span className="text-sm font-semibold text-slate-800">
          {busType === "BUS_25" ? "25인승 우등" : "45인승 일반"}
        </span>
            </div>

            {/* 선택한 좌석 */}
            <div className="flex justify-between mb-4">
                <span className="text-sm text-slate-500">선택한 좌석</span>
                <span className="text-sm font-semibold text-[#426f55]">
          {selectedSeatNumber ? `${selectedSeatNumber}번 좌석` : "미선택"}
        </span>
            </div>

            <hr className="mb-4 border-[#dbe7dc]" />

            {/* 최종 결제 금액 */}
            <div className="flex justify-between mb-6">
                <span className="text-sm font-semibold text-slate-700">최종 결제 금액</span>
                <span className="text-base font-bold text-slate-950">
        {finalAmount !== null
            ? `${finalAmount.toLocaleString()} 원`
            : "-"}
    </span>
            </div>

            {/* 결제하기 버튼 */}
            <button
                className={`w-full rounded-lg py-3 text-sm font-semibold text-white transition
          ${selectedSeatNumber
                    ? "bg-[#4f7a61] cursor-pointer hover:bg-[#426f55]"
                    : "bg-slate-300 cursor-not-allowed"
                }`}
                onClick={onPaymentClick}
                disabled={!selectedSeatNumber} // 좌석 선택 안 하면 비활성화
            >
                결제하기 →
            </button>

            <div className="mt-3 rounded-lg bg-[#eef5ea] px-3 py-3 text-xs leading-5 text-slate-600 [word-break:keep-all]">
                <p>예약 시 최소 예상가의 50%가 보증금으로 결제됩니다.</p>
                <p>펀딩 확정 후에는 확정 인원 기준 금액에서 보증금을 제외한 잔금을 결제합니다.</p>
                <p>확정일 전 취소 시 결제한 보증금은 환불됩니다.</p>
            </div>
        </div>
    );
}
