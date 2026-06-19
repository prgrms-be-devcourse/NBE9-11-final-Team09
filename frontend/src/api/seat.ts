import { ApiResponse, SeatLayout, Seat } from "@/types/seat";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

// TODO: 로그인 구현 확인 후 토큰 저장 방식 맞춰서 수정 필요
//       현재는 localStorage 기준으로 임시 작성
//       쿠키 방식이면 credentials: "include"로 변경
function getAuthHeader(): HeadersInit {
    const token = localStorage.getItem("accessToken");
    return {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
    };
}

// 좌석 배치도 조회
export async function getSeatLayout(pathId: number): Promise<SeatLayout> {
    const response = await fetch(`${BASE_URL}/pathinfos/${pathId}/seats`, {
        method: "GET",
        headers: getAuthHeader(),
    });

    if (!response.ok) {
        throw new Error("좌석 배치도를 불러오는데 실패했습니다.");
    }

    const result: ApiResponse<SeatLayout> = await response.json();
    return result.data;
}

// 좌석 선점 (5분 홀딩)
export async function holdSeat(seatId: number): Promise<Seat> {
    const response = await fetch(`${BASE_URL}/seats/${seatId}/hold`, {
        method: "POST",
        headers: getAuthHeader(),
    });

    if (!response.ok) {
        // 409 = 이미 다른 사람이 선점한 좌석
        if (response.status === 409) {
            throw new Error("ALREADY_HELD");
        }
        throw new Error("좌석 선점에 실패했습니다.");
    }

    const result: ApiResponse<Seat> = await response.json();
    return result.data;
}

// 좌석 홀딩 취소
export async function releaseSeat(seatId: number): Promise<void> {
    const response = await fetch(`${BASE_URL}/seats/${seatId}/hold`, {
        method: "DELETE",
        headers: getAuthHeader(),
    });

    if (!response.ok) {
        throw new Error("좌석 홀딩 취소에 실패했습니다.");
    }
}