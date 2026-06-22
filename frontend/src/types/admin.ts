export interface AdminApiResponse<T> {
  resultCode: string;
  msg: string;
  data: T;
}

export interface AdminPageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminLoginResponse {
  accessToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  admin: {
    adminId: number;
    loginId: string;
    role: string;
  };
}

export interface AdminStatistics {
  totalUsers: number;
  activeUsers: number;
  withdrawnUsers: number;
  activeFundings: number;
  completedFundings: number;
  cancelledFundings: number;
  totalPaymentAmount: number;
  pendingSettlements: number;
  pendingReports: number;
}

export interface AdminMember {
  memberId: number;
  email: string;
  name: string;
  nickname: string;
  phoneNumber: string;
  provider: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminMemberDetail extends AdminMember {
  providerId: string | null;
  participationCount: number;
  fundingCount: number;
  paymentCount: number;
}

export interface AdminFunding {
  fundingId: number;
  memberId: number;
  hostEmail: string;
  title: string;
  content: string;
  departureDate: string;
  busType: string;
  status: string;
  minParticipants: number;
  maxParticipants: number;
  currentParticipants: number;
  createdAt: string;
}

export interface AdminSettlement {
  settlementId: number;
  memberId: number;
  hostEmail: string;
  fundingId: number;
  fundingTitle: string;
  totalAmount: number | string;
  platformFee: number | string;
  hostPaybackAmount: number | string;
  status: string;
  paybackPaidAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminSettlementDetail extends AdminSettlement {
  hostNickname: string;
  fundingStatus: string;
  departureDate: string;
  paymentSummary: {
    totalPaidCount: number;
    depositPaidCount: number;
    balancePaidCount: number;
    totalPaidAmount: number;
  };
}

export type AdminSection = "overview" | "members" | "fundings" | "settlements";
