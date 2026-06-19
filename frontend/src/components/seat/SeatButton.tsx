import { Seat, SeatDisplayStatus } from "@/types/seat";

interface SeatButtonProps {
    seat: Seat;                    // 좌석 정보
    onSeatClick: (seat: Seat) => void; // 좌석 클릭 시 실행할 함수
}

function getSeatStyle(status: SeatDisplayStatus, mySeat: boolean): string {
    // 내가 선점한 좌석 → 검정 배경
    if (mySeat) {
        return "bg-black text-white cursor-pointer";
    }

    switch (status) {
        case "AVAILABLE":
            // 선택 가능 → 흰 배경, 테두리
            return "bg-white text-black border border-gray-400 cursor-pointer hover:bg-gray-100";
        case "HOLD":
            // 다른 사람이 선점 중 → 회색 배경, 클릭 시 팝업
            return "bg-gray-300 text-gray-500 cursor-pointer";
        case "BOOKED":
            // 예매 완료 → 회색 배경, 클릭 불가
            return "bg-gray-300 text-gray-500 cursor-not-allowed";
        default:
            return "bg-white border border-gray-400";
    }
}

// 개별 좌석 버튼 컴포넌트
export default function SeatButton({ seat, onSeatClick }: SeatButtonProps) {
    return (
        <button
            className={`
        w-10 h-10 rounded text-sm font-medium
        flex items-center justify-center
        ${getSeatStyle(seat.status, seat.mySeat)}
      `}
            onClick={() => onSeatClick(seat)}
            disabled={seat.status === "BOOKED"} // BOOKED면 클릭 자체 막기
        >
            {seat.seatNumber}
        </button>
    );
}