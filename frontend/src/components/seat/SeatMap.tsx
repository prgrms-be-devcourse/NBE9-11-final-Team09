import { Seat, BusType } from "@/types/funding";
import SeatButton from "./SeatButton";

interface SeatMapProps {
    busType: BusType;
    seats: Seat[];
    selectedSeatId: number | null;
    onSeatClick: (seat: Seat) => void;
}

interface BusLayoutProps {
    seats: Seat[];
    selectedSeatId: number | null;
    onSeatClick: (seat: Seat) => void;
}

function findSeat(seats: Seat[], seatNumber: string): Seat | undefined {
    return seats.find((seat) => seat.seatNumber === seatNumber);
}

function renderSeat(
    seats: Seat[],
    seatNumber: string,
    selectedSeatId: number | null,
    onSeatClick: (seat: Seat) => void
) {
    const seat = findSeat(seats, seatNumber);

    return seat ? (
        <SeatButton
            key={seatNumber}
            seat={seat}
            isSelected={selectedSeatId === seat.seatId}
            onClick={() => onSeatClick(seat)}
        />
    ) : (
        <div key={seatNumber} className="w-14 h-14" />
    );
}

function BUS25Layout({ seats, selectedSeatId, onSeatClick }: BusLayoutProps) {
    const rows = Array.from({ length: 8 }, (_, i) => i + 1);

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2 mt-8 mb-6">
                <div className="w-14 h-14 border-2 border-dashed border-gray-400 rounded flex items-center justify-center text-xs text-gray-500">
                    운전석
                </div>
            </div>

            {rows.map((row) => (
                <div key={row} className="flex items-center gap-2">
                    <div className="flex gap-1">
                        {["A", "B"].map((col) =>
                            renderSeat(seats, `${row}${col}`, selectedSeatId, onSeatClick)
                        )}
                    </div>
                    <div className="w-20" />
                    <div className="flex gap-1">
                        {["C"].map((col) =>
                            renderSeat(seats, `${row}${col}`, selectedSeatId, onSeatClick)
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
}

function BUS45Layout({ seats, selectedSeatId, onSeatClick }: BusLayoutProps) {
    const rows = Array.from({ length: 11 }, (_, i) => i + 1);

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2 mt-8 mb-6">
                <div className="w-14 h-14 border-2 border-dashed border-gray-400 rounded flex items-center justify-center text-xs text-gray-500">
                    운전석
                </div>
            </div>

            {rows.map((row) => (
                <div key={row} className="flex items-center gap-2">
                    <div className="flex gap-1">
                        {["A", "B"].map((col) =>
                            renderSeat(seats, `${row}${col}`, selectedSeatId, onSeatClick)
                        )}
                    </div>
                    <div className="w-20" />
                    <div className="flex gap-1">
                        {["C", "D"].map((col) =>
                            renderSeat(seats, `${row}${col}`, selectedSeatId, onSeatClick)
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
}

function SeatLegend() {
    return (
        <div className="flex flex-wrap gap-4 text-xs text-gray-600">
            <div className="flex items-center gap-1">
                <div className="w-4 h-4 border border-gray-400 rounded" />
                <span>선택 가능</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="w-4 h-4 bg-black rounded" />
                <span>선택한 좌석</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="w-4 h-4 bg-gray-500 rounded" />
                <span>다른 사람 선점 중</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="w-4 h-4 bg-gray-300 rounded" />
                <span>예매 완료</span>
            </div>
        </div>
    );
}

export default function SeatMap({ busType, seats, selectedSeatId, onSeatClick }: SeatMapProps) {
    return (
        <div className="border-2 border-gray-300 rounded-xl p-6 bg-white">
            <h3 className="text-sm font-semibold mb-4">
                탑승 좌석 선택 ({busType === "BUS_25" ? "25인승 우등형" : "45인승 일반형"})
            </h3>

            <div className="mt-6 flex justify-center">
                <div className="rounded-4xl border-2 border-gray-300 bg-gray-50 p-8">
                    <SeatLegend />
                    <div className="flex justify-center">
                        {busType === "BUS_25" ? (
                            <BUS25Layout seats={seats} selectedSeatId={selectedSeatId} onSeatClick={onSeatClick} />
                        ) : (
                            <BUS45Layout seats={seats} selectedSeatId={selectedSeatId} onSeatClick={onSeatClick} />
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}