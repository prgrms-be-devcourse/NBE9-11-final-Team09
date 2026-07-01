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
                <div className="flex h-14 w-14 items-center justify-center rounded border-2 border-dashed border-slate-300 text-xs text-slate-500">
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
                <div className="flex h-14 w-14 items-center justify-center rounded border-2 border-dashed border-slate-300 text-xs text-slate-500">
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
        <div className="flex flex-wrap gap-4 text-xs font-medium text-slate-600">
            <div className="flex items-center gap-1">
                <div className="h-4 w-4 rounded border border-slate-400" />
                <span>선택 가능</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="h-4 w-4 rounded bg-[#4f7a61]" />
                <span>선택한 좌석</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="h-4 w-4 rounded bg-slate-500" />
                <span>다른 사람 선점 중</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="h-4 w-4 rounded bg-slate-300" />
                <span>예매 완료</span>
            </div>
        </div>
    );
}

export default function SeatMap({ busType, seats, selectedSeatId, onSeatClick }: SeatMapProps) {
    return (
        <div className="rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
            <h3 className="mb-4 text-sm font-bold text-slate-950">
                탑승 좌석 선택 ({busType === "BUS_25" ? "25인승 우등형" : "45인승 일반형"})
            </h3>

            <div className="mt-6 flex justify-center">
                <div className="rounded-3xl border border-[#dbe7dc] bg-[#f8faf9] p-8">
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
