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
  phoneNumber: string;
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
  phoneNumber: string;
  updatedAt: string;
}

export interface MemberWithdrawRequest {
  password: string;
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
