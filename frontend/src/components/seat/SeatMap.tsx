import { Seat, BusType } from "@/types/seat";
import SeatButton from "./SeatButton";

interface SeatMapProps {
    busType: BusType;           // 버스 종류 (BUS_25 / BUS_45)
    seats: Seat[];              // 전체 좌석 목록
    onSeatClick: (seat: Seat) => void; // 좌석 클릭 핸들러
}

function findSeat(seats: Seat[], seatNumber: string): Seat | undefined {
    return seats.find((s) => s.seatNumber === seatNumber);
}

/*
* 25인승 배치도
* 8행 × 3열 (A B | 통로 | C)
*/
function BUS25Layout({ seats, onSeatClick }: { seats: Seat[], onSeatClick: (seat: Seat) => void }) {
    const rows = Array.from({ length: 8 }, (_, i) => i + 1);

    return (
        <div className="flex flex-col gap-2">
            {/* 운전석 */}
            <div className="flex justify-end mb-2">
                <div className="w-10 h-10 border-2 border-dashed border-gray-400 rounded flex items-center justify-center text-xs text-gray-400">
                    운전석
                </div>
            </div>

            {/* 좌석 배치 */}
            {rows.map((row) => (
                <div key={row} className="flex items-center gap-2">
                    {/* 왼쪽 2열 (A, B) */}
                    <div className="flex gap-1">
                        {["A", "B"].map((col) => {
                            const seat = findSeat(seats, `${row}${col}`);
                            return seat ? (
                                <SeatButton key={col} seat={seat} onSeatClick={onSeatClick} />
                            ) : (
                                <div key={col} className="w-10 h-10" /> // 빈 공간
                            );
                        })}
                    </div>

                    {/* 통로 */}
                    <div className="w-6" />

                    {/* 오른쪽 1열 (C) */}
                    <div className="flex gap-1">
                        {["C"].map((col) => {
                            const seat = findSeat(seats, `${row}${col}`);
                            return seat ? (
                                <SeatButton key={col} seat={seat} onSeatClick={onSeatClick} />
                            ) : (
                                <div key={col} className="w-10 h-10" />
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
}

/*
* 45인승 배치도
* 11행 × 4열 (A B | 통로 | C D)
*/
function BUS45Layout({ seats, onSeatClick }: { seats: Seat[], onSeatClick: (seat: Seat) => void }) {
    const rows = Array.from({ length: 11 }, (_, i) => i + 1); // [1, 2, ..., 11]

    return (
        <div className="flex flex-col gap-2">
            {/* 운전석 */}
            <div className="flex justify-end mb-2">
                <div className="w-10 h-10 border-2 border-dashed border-gray-400 rounded flex items-center justify-center text-xs text-gray-400">
                    운전석
                </div>
            </div>

            {/* 좌석 배치 */}
            {rows.map((row) => (
                <div key={row} className="flex items-center gap-2">
                    {/* 왼쪽 2열 (A, B) */}
                    <div className="flex gap-1">
                        {["A", "B"].map((col) => {
                            const seat = findSeat(seats, `${row}${col}`);
                            return seat ? (
                                <SeatButton key={col} seat={seat} onSeatClick={onSeatClick} />
                            ) : (
                                <div key={col} className="w-10 h-10" />
                            );
                        })}
                    </div>

                    {/* 통로 */}
                    <div className="w-6" />

                    {/* 오른쪽 2열 (C, D) */}
                    <div className="flex gap-1">
                        {["C", "D"].map((col) => {
                            const seat = findSeat(seats, `${row}${col}`);
                            return seat ? (
                                <SeatButton key={col} seat={seat} onSeatClick={onSeatClick} />
                            ) : (
                                <div key={col} className="w-10 h-10" />
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
}

// SeatMap 메인 컴포넌트
// busType에 따라 25인승 or 45인승 배치도 렌더링
export default function SeatMap({ busType, seats, onSeatClick }: SeatMapProps) {
    return (
        <div className="border-2 border-gray-300 rounded-xl p-6 bg-white">
            <h3 className="text-sm font-semibold mb-4">
                탑승 좌석 선택 ({busType === "BUS_25" ? "25인승 우등형" : "45인승 일반형"})
            </h3>

            {/* 좌석 상태 범례 */}
            <div className="flex gap-4 mb-4 text-xs text-gray-600">
                <div className="flex items-center gap-1">
                    <div className="w-4 h-4 border border-gray-400 rounded" />
                    <span>선택 가능</span>
                </div>
                <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-black rounded" />
                    <span>선택한 좌석</span>
                </div>
                <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-gray-300 rounded" />
                    <span>선택 불가 (예매 완료)</span>
                </div>
            </div>

            {/* 버스 종류에 따라 배치도 렌더링 */}
            {busType === "BUS_25" ? (
                <BUS25Layout seats={seats} onSeatClick={onSeatClick} />
            ) : (
                <BUS45Layout seats={seats} onSeatClick={onSeatClick} />
            )}
        </div>
    );
}