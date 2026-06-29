export interface ApiResponse<T> {
  resultCode: string;
  msg: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface MemberProfile {
  memberId: number;
  email: string;
  name: string;
  nickname: string;
  phoneNumber: string | null;
  provider: string | null;
  status: string;
  createdAt: string;
}

export interface MemberUpdateRequest {
  nickname: string;
  phoneNumber: string;
}

export interface MemberUpdateResponse {
  memberId: number;
  email: string;
  name: string;
  nickname: string;
  phoneNumber: string | null;
  updatedAt: string;
}

export interface MemberWithdrawRequest {
  password: string;
}

export interface MemberLoginUser {
  userId: number;
  email: string;
  name: string;
  nickname: string;
}

export interface MemberLoginResponse {
  accessToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  user: MemberLoginUser;
}

export interface MemberKakaoCodeLoginRequest {
  code: string;
  redirectUri: string;
}

export interface MemberParticipation {
  participationId: number;
  fundingId: number;
  fundingTitle: string;
  departureDate: string;
  status: string;
  paymentStatus: string;
  createdAt: string;
}

export interface MemberFunding {
  fundingId: number;
  fundingTitle: string;
  departureDate: string;
  currentParticipants: number;
  maxParticipants: number;
  status: string;
  createdAt: string;
  hostSeatNumbers: string[];
}

export interface MemberPayment {
  paymentId: number;
  fundingTitle: string;
  type: string;
  amount: number | string;
  status: string;
  createdAt: string;
}

export type HistoryTab = "participations" | "fundings" | "payments";

export interface MemberHistoryPages {
  participations: PageResponse<MemberParticipation>;
  fundings: PageResponse<MemberFunding>;
  payments: PageResponse<MemberPayment>;
}

export type ParticipationStatus = "ACTIVE" | "CANCELED" | "COMPLETED";

export type ParticipationPaymentStatus =
    | "PENDING"
    | "ACTIVE"
    | "CANCELED"
    | "COMPLETED"
    | "NO_SHOW";

export interface MyParticipation {
  participationId: number;
  fundingId: number;
  fundingTitle: string;
  routeInfo: string;
  outboundSeatNumber: string;
  returnSeatNumber: string | null;
  status: ParticipationStatus;
  paymentStatus: ParticipationPaymentStatus;
  canBoard: boolean;
  departureTime: string;
  balanceAmount: number;
}