import { getFundingAccessToken } from "@/lib/fundingAuth";
import type {
  ApiResponse,
  FundingCreateResponse,
  FundingDetail,
  FundingListItem,
  FundingPayload,
  FundingStatus,
  PageResponse,
  Region,
  SeatLayout,
} from "@/types/funding";

export type FundingListParams = {
  statuses?: FundingStatus[];
  departureDate?: string;
  departureRegion?: Region | "";
  arrivalRegion?: Region | "";
  sort?: string;
  page?: number;
  size?: number;
};

async function request<T>(path: string, init: RequestInit = {}) {
  const token = getFundingAccessToken();
  const headers = new Headers(init.headers);

  if (!(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(path, {
    ...init,
    headers,
  });

  const body = (await response.json().catch(() => null)) as
    | ApiResponse<T>
    | { message?: string; msg?: string }
    | null;

  if (!response.ok) {
    const message =
      body && "message" in body
        ? body.message
        : body && "msg" in body
          ? body.msg
          : "요청을 처리하지 못했습니다.";
    throw new Error(message ?? "요청을 처리하지 못했습니다.");
  }

  return (body as ApiResponse<T>).data;
}

export function getFundingList(params: FundingListParams) {
  const search = new URLSearchParams();

  params.statuses?.forEach((status) => search.append("statuses", status));

  if (params.departureDate) {
    search.set("departureDate", params.departureDate);
  }
  if (params.departureRegion) {
    search.set("departureRegion", params.departureRegion);
  }
  if (params.arrivalRegion) {
    search.set("arrivalRegion", params.arrivalRegion);
  }

  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 20));
  search.set("sort", params.sort ?? "departureDate,asc");

  return request<PageResponse<FundingListItem>>(
    `/api/fundings?${search.toString()}`
  );
}

export function getFunding(fundingId: number) {
  return request<FundingDetail>(`/api/fundings/${fundingId}`);
}

export function createFunding(payload: FundingPayload) {
  return request<FundingCreateResponse>("/api/fundings", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateFunding(fundingId: number, payload: FundingPayload) {
  return request<void>(`/api/fundings/${fundingId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteFunding(fundingId: number) {
  return request<void>(`/api/fundings/${fundingId}`, {
    method: "DELETE",
  });
}

export function getSeatLayout(pathId: number) {
  return request<SeatLayout>(`/api/pathinfos/${pathId}/seats`);
}
