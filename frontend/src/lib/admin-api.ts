import type {
  AdminApiResponse,
  AdminFunding,
  AdminLoginResponse,
  AdminMember,
  AdminMemberDetail,
  AdminPageResponse,
  AdminSettlement,
  AdminSettlementDetail,
  AdminStatistics,
} from "@/types/admin";

const ADMIN_TOKEN_KEY = "adminAccessToken";
const ADMIN_PROFILE_KEY = "adminProfile";

export class AdminAuthenticationError extends Error {
  constructor(message = "관리자 인증이 필요합니다.") {
    super(message);
    this.name = "AdminAuthenticationError";
  }
}

export class AdminApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = "AdminApiError";
  }
}

function parseError(payload: unknown) {
  if (!payload || typeof payload !== "object") return {};
  const value = payload as Record<string, unknown>;
  return {
    message:
      typeof value.message === "string"
        ? value.message
        : typeof value.msg === "string"
          ? value.msg
          : undefined,
    code: typeof value.code === "string" ? value.code : undefined,
  };
}

export function getAdminAccessToken() {
  if (typeof window === "undefined") return null;
  return (
    window.sessionStorage.getItem(ADMIN_TOKEN_KEY) ??
    window.localStorage.getItem(ADMIN_TOKEN_KEY)
  );
}

export function getStoredAdmin() {
  if (typeof window === "undefined") return null;
  const value =
    window.sessionStorage.getItem(ADMIN_PROFILE_KEY) ??
    window.localStorage.getItem(ADMIN_PROFILE_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value) as AdminLoginResponse["admin"];
  } catch {
    return null;
  }
}

export function storeAdminSession(response: AdminLoginResponse, persistent: boolean) {
  if (typeof window === "undefined") return;
  clearAdminSession();
  const storage = persistent ? window.localStorage : window.sessionStorage;
  storage.setItem(ADMIN_TOKEN_KEY, response.accessToken);
  storage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(response.admin));
}

export function clearAdminSession() {
  if (typeof window === "undefined") return;
  for (const storage of [window.sessionStorage, window.localStorage]) {
    storage.removeItem(ADMIN_TOKEN_KEY);
    storage.removeItem(ADMIN_PROFILE_KEY);
  }
}

async function parseBody(response: Response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

async function publicRequest<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...init.headers },
    cache: "no-store",
  });
  const payload = await parseBody(response);
  if (!response.ok) {
    const error = parseError(payload);
    throw new AdminApiError(
      error.message ?? "요청을 처리하지 못했습니다.",
      response.status,
      error.code,
    );
  }
  return payload as T;
}

async function adminRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getAdminAccessToken();
  if (!token) throw new AdminAuthenticationError();

  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers, cache: "no-store" });
  const payload = await parseBody(response);
  if (!response.ok) {
    const error = parseError(payload);
    if (response.status === 401 || response.status === 403) {
      clearAdminSession();
      throw new AdminAuthenticationError(error.message);
    }
    throw new AdminApiError(
      error.message ?? "요청을 처리하지 못했습니다.",
      response.status,
      error.code,
    );
  }
  return payload as T;
}

export async function loginAdmin(loginId: string, password: string) {
  const response = await publicRequest<AdminApiResponse<AdminLoginResponse>>(
    "/api/admin/login",
    { method: "POST", body: JSON.stringify({ loginId, password }) },
  );
  return response.data;
}

export async function logoutAdmin() {
  try {
    await adminRequest<AdminApiResponse<null>>("/api/admin/logout", {
      method: "POST",
    });
  } finally {
    clearAdminSession();
  }
}

export async function getAdminStatistics() {
  const response = await adminRequest<AdminApiResponse<AdminStatistics>>(
    "/api/admin/statistics",
  );
  return response.data;
}

function pageQuery(page: number, size = 20) {
  return new URLSearchParams({ page: String(page), size: String(size) });
}

export async function getAdminMembers(page = 0, size = 20) {
  const response = await adminRequest<
    AdminApiResponse<AdminPageResponse<AdminMember>>
  >(`/api/admin/members?${pageQuery(page, size)}`);
  return response.data;
}

export async function getAdminMember(memberId: number) {
  const response = await adminRequest<AdminApiResponse<AdminMemberDetail>>(
    `/api/admin/members/${memberId}`,
  );
  return response.data;
}

export async function withdrawAdminMember(memberId: number, reason: string) {
  await adminRequest(`/api/admin/members/${memberId}`, {
    method: "PATCH",
    body: JSON.stringify({ reason }),
  });
}

export async function getAdminFundings(page = 0, size = 20) {
  const response = await adminRequest<
    AdminApiResponse<AdminPageResponse<AdminFunding>>
  >(`/api/admin/fundings?${pageQuery(page, size)}`);
  return response.data;
}

export async function cancelAdminFunding(fundingId: number, reason: string) {
  await adminRequest(`/api/admin/fundings/${fundingId}`, {
    method: "PATCH",
    body: JSON.stringify({ reason }),
  });
}

export async function getAdminSettlements(page = 0, size = 20) {
  const response = await adminRequest<
    AdminApiResponse<AdminPageResponse<AdminSettlement>>
  >(`/api/admin/settlements?${pageQuery(page, size)}`);
  return response.data;
}

export async function getAdminSettlement(settlementId: number) {
  const response = await adminRequest<AdminApiResponse<AdminSettlementDetail>>(
    `/api/admin/settlements/${settlementId}`,
  );
  return response.data;
}
