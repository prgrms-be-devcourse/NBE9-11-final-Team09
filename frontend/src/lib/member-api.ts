import type {
  ApiResponse,
  HistoryTab,
  MemberFunding,
  MemberHistoryPages,
  MemberKakaoCodeLoginRequest,
  MemberLoginResponse,
  MemberParticipation,
  MemberPayment,
  MemberProfile,
  MemberSocialLoginRequest,
  MemberUpdateRequest,
  MemberUpdateResponse,
  MemberWithdrawRequest,
  MyParticipation,
  PageResponse,
} from "@/types/member";

const ACCESS_TOKEN_KEY = "accessToken";

export class AuthenticationRequiredError extends Error {
  constructor() {
    super("로그인이 필요합니다.");
    this.name = "AuthenticationRequiredError";
  }
}

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

export function getAccessToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return (
    window.sessionStorage.getItem(ACCESS_TOKEN_KEY) ??
    window.localStorage.getItem(ACCESS_TOKEN_KEY)
  );
}

export function storeAccessToken(accessToken: string, keepLogin: boolean) {
  if (typeof window === "undefined") {
    return;
  }

  const targetStorage = keepLogin ? window.localStorage : window.sessionStorage;
  const otherStorage = keepLogin ? window.sessionStorage : window.localStorage;

  targetStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  otherStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function clearAccessToken() {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}

function getErrorDetails(payload: unknown) {
  if (!payload || typeof payload !== "object") {
    return {};
  }

  const errorPayload = payload as Record<string, unknown>;
  const message =
    typeof errorPayload.message === "string"
      ? errorPayload.message
      : typeof errorPayload.msg === "string"
        ? errorPayload.msg
        : undefined;
  const code =
    typeof errorPayload.code === "string" ? errorPayload.code : undefined;

  return { message, code };
}

async function parseJsonResponse(response: Response) {
  const responseText = await response.text();

  if (!responseText) {
    return null;
  }

  try {
    return JSON.parse(responseText) as unknown;
  } catch {
    return null;
  }
}

async function publicRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    ...init,
    headers,
    cache: "no-store",
  });

  const payload = await parseJsonResponse(response);

  if (!response.ok) {
    const { message, code } = getErrorDetails(payload);

    throw new ApiRequestError(
      message ?? "요청을 처리하지 못했습니다.",
      response.status,
      code,
    );
  }

  return payload as T;
}

async function authorizedRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const accessToken = getAccessToken();

  if (!accessToken) {
    throw new AuthenticationRequiredError();
  }

  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${accessToken}`);

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    ...init,
    headers,
    cache: "no-store",
  });

  const payload = await parseJsonResponse(response);

  if (!response.ok) {
    const { message, code } = getErrorDetails(payload);

    if (
      (response.status === 401 || response.status === 403) &&
      code !== "USR012"
    ) {
      clearAccessToken();
      throw new AuthenticationRequiredError();
    }

    throw new ApiRequestError(
      message ?? "요청을 처리하지 못했습니다.",
      response.status,
      code,
    );
  }

  return payload as T;
}

export async function socialLoginMember(request: MemberSocialLoginRequest) {
  const response = await publicRequest<ApiResponse<MemberLoginResponse>>(
    "/api/members/social-login",
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );

  return response.data;
}

export async function kakaoAuthorizationCodeLogin(
  request: MemberKakaoCodeLoginRequest,
) {
  const response = await publicRequest<ApiResponse<MemberLoginResponse>>(
    "/api/members/social-login/kakao",
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );

  return response.data;
}

export async function getMyProfile() {
  const response = await authorizedRequest<ApiResponse<MemberProfile>>(
    "/api/members/me",
  );

  return response.data;
}

export async function updateMyProfile(request: MemberUpdateRequest) {
  const response = await authorizedRequest<ApiResponse<MemberUpdateResponse>>(
    "/api/members/me",
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );

  return response.data;
}

const HISTORY_PATHS: Record<HistoryTab, string> = {
  participations: "/api/members/me/participations",
  fundings: "/api/members/me/fundings",
  payments: "/api/members/me/payments",
};

type HistoryResponseMap = {
  participations: MemberParticipation;
  fundings: MemberFunding;
  payments: MemberPayment;
};

export async function getMyHistory<T extends HistoryTab>(
  tab: T,
  page = 0,
  size = 5,
) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const response = await authorizedRequest<
    ApiResponse<PageResponse<HistoryResponseMap[T]>>
  >(`${HISTORY_PATHS[tab]}?${query.toString()}`);

  return response.data;
}

export async function getMyDashboard(): Promise<{
  profile: MemberProfile;
  histories: MemberHistoryPages;
}> {
  const [profile, participations, fundings, payments] = await Promise.all([
    getMyProfile(),
    getMyHistory("participations"),
    getMyHistory("fundings"),
    getMyHistory("payments"),
  ]);

  return {
    profile,
    histories: { participations, fundings, payments },
  };
}

export async function logoutMember() {
  await authorizedRequest<ApiResponse<null>>("/api/members/logout", {
    method: "POST",
  });
  clearAccessToken();
}

export async function withdrawMember(request: MemberWithdrawRequest) {
  await authorizedRequest<ApiResponse<null>>("/api/members/me", {
    method: "DELETE",
    body: JSON.stringify(request),
  });
  clearAccessToken();
}

export async function getMyParticipations() {
  const response = await authorizedRequest<ApiResponse<MyParticipation[]>>(
      "/api/participations/me",
  );
  return response.data;
}