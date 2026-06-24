import { getFundingAccessToken as getAccessToken } from "@/lib/fundingAuth";

type ApiResponse<T> = { data: T; resultCode: string; msg: string };

export type ParticipationResult = {
  participationId: number;
  finalAmount: string;
  outboundSeatId: number;
  returnSeatId: number | null;
  status: string;
  paymentStatus: string;
  createdAt: string;
};

function authHeaders(token: string | null): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// 백엔드 에러 응답: { message: string, code: string, status: number }
// ApiResponse 에러는 msg 필드 사용, 둘 다 대응
async function parseError(res: Response, fallback: string): Promise<Error> {
  const body = (await res.json().catch(() => ({}))) as {
    message?: string;
    msg?: string;
  };
  return new Error(body.message ?? body.msg ?? fallback);
}

export async function createParticipation(
  fundingId: number,
  outboundSeatId: number,
  returnSeatId?: number | null,
): Promise<ParticipationResult> {
  const token = getAccessToken();
  const res = await fetch("/api/participations", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify({ fundingId, outboundSeatId, returnSeatId }),
  });
  if (!res.ok) throw await parseError(res, "참여 신청에 실패했습니다.");
  const body = (await res.json()) as ApiResponse<ParticipationResult>;
  return body.data;
}

export async function preparePayment(
  participationId: number,
): Promise<{ orderId: string; amount: number }> {
  const token = getAccessToken();
  const res = await fetch(
    `/api/payments/prepare?participationId=${participationId}`,
    {
      method: "POST",
      headers: authHeaders(token),
    },
  );
  if (!res.ok) throw await parseError(res, "결제 준비에 실패했습니다.");
  const body = (await res.json()) as ApiResponse<{ orderId: string; amount: number }>;
  return body.data;
}

export async function confirmDeposit(params: {
  paymentKey: string;
  amount: number;
  participationId: number;
}): Promise<void> {
  const token = getAccessToken();
  const res = await fetch("/api/payments/deposit/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify(params),
  });
  if (!res.ok) throw await parseError(res, "결제 승인에 실패했습니다.");
}

export async function confirmBalance(params: {
  paymentKey: string;
  amount: number;
  participationId: number;
}): Promise<void> {
  const token = getAccessToken();
  const res = await fetch("/api/payments/balance/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify(params),
  });
  if (!res.ok) throw await parseError(res, "잔금 결제 승인에 실패했습니다.");
}

export type SettlementResponse = {
  settlementId: number;
  totalAmount: string;
  platformFee: string;
  hostPaybackAmount: string;
  status: "CALCULATED" | "APPROVED" | "REJECTED" | "COMPLETED";
  paybackHold: boolean;
  paybackPaidAt: string | null;
  createdAt: string;
};

export async function getSettlement(
  fundingId: number,
): Promise<SettlementResponse> {
  const token = getAccessToken();
  const res = await fetch(`/api/settlements/funding/${fundingId}`, {
    headers: authHeaders(token),
  });
  if (!res.ok) throw await parseError(res, "정산 정보를 불러오지 못했습니다.");
  const body = (await res.json()) as ApiResponse<SettlementResponse>;
  return body.data;
}
