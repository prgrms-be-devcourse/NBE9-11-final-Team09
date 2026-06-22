// 좌석 화면 표시 상태
export type SeatDisplayStatus = "AVAILABLE" | "HOLD" | "BOOKED";

// 버스 종류
export type BusType = "BUS_25" | "BUS_45";

// 개별 좌석 타입
export interface Seat {
    seatId: number;        // 좌석 ID
    seatNumber: string;    // 좌석 번호 (예: "1", "2")
    status: SeatDisplayStatus; // 화면 표시용 상태
    mySeat: boolean;       // 내가 선점한 좌석이면 true
}

// 좌석 배치도 전체 응답 타입
export interface SeatLayout {
    pathId: number;        // 노선 ID
    busType: BusType;      // 버스 종류
    seats: Seat[];         // 전체 좌석 목록
}

// 백엔드 공통 응답 포맷
export interface ApiResponse<T> {
    status: string;        // "SUCCESS" or "ERROR"
    message: string;       // 응답 메시지
    data: T;               // 실제 데이터
}