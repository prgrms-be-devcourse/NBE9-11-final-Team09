import { Seat, SeatDisplayStatus } from "@/types/funding";

interface SeatButtonProps {
    seat: Seat;
    isSelected: boolean;
    onClick: () => void;
}

function getSeatStyle(status: SeatDisplayStatus, isSelected: boolean): string {
    if (isSelected) {
        return "bg-black text-white cursor-pointer";
    }

    switch (status) {
        case "AVAILABLE":
            return "bg-white text-black border border-gray-400 cursor-pointer hover:bg-gray-100";
        case "HOLD":
            return "bg-gray-500 text-white cursor-pointer";
        case "BOOKED":
            return "bg-gray-300 text-gray-500 cursor-not-allowed";
        default:
            return "bg-white border border-gray-400";
    }
}

export default function SeatButton({ seat, isSelected, onClick }: SeatButtonProps) {
    return (
        <button
            className={`
        w-14 h-14 rounded text-sm font-medium
        flex items-center justify-center
        ${getSeatStyle(seat.status, isSelected)}
      `}
            onClick={onClick}
            disabled={seat.status === "BOOKED"}
        >
            {seat.seatNumber}
        </button>
    );
}