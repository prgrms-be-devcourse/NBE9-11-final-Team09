export const REGIONS = [
  "SEOUL",
  "BUSAN",
  "DAEJEON",
  "INCHEON",
  "DAEGU",
  "GWANGJU",
  "ULSAN",
] as const;

export const FUNDING_STATUSES = [
  "RECRUITING",
  "CONFIRMED",
  "CLOSED",
  "COMPLETED",
  "FAILED",
  "CANCELLED",
] as const;

export const FUNDING_FILTER_STATUSES = [
  "RECRUITING",
  "CONFIRMED",
  "CLOSED",
  "COMPLETED",
  "FAILED",
  "CANCELLED",
] as const;

export type Region = (typeof REGIONS)[number];
export type FundingStatus = (typeof FUNDING_STATUSES)[number];
export type BusType = "BUS_25" | "BUS_45";
export type TripType = "ONE_WAY" | "ROUND";
export type Direction = "OUTBOUND" | "RETURN";
export type PathinfoStatus = "PENDING" | "COMPLETED" | "CANCELLED";
export type SeatDisplayStatus = "AVAILABLE" | "HOLD" | "BOOKED";

export type ApiResponse<T> = {
  resultCode: string;
  msg: string;
  data: T;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type RouteRequest = {
  departureTime: string;
  returnTime?: string | null;
  departureAddress: string;
  departureRegion: Region;
  arrivalAddress: string;
  arrivalRegion: Region;
};

export type FundingPayload = {
  title: string;
  content: string;
  busType: BusType;
  minParticipants: number;
  tripType: TripType;
  hostOutboundSeatNumber: string;
  hostReturnSeatNumber?: string | null;
  route: RouteRequest;
};

export type FundingListItem = {
  fundingId: number;
  title: string;
  hostNickname: string;
  departureAddress: string | null;
  arrivalAddress: string | null;
  departureTime: string | null;
  status: FundingStatus;
  tripType: TripType;
  currentParticipants: number;
  minParticipants: number;
  maxParticipants: number;
  totalPrice: number;
  finalPrice: number | null;
  currentPrice: number | null;
  minPrice: number;
  maxPrice: number;
};

export type Pathinfo = {
  pathinfoId: number;
  departureTime: string;
  departureAddress: string;
  departureRegion: Region;
  arrivalAddress: string;
  arrivalRegion: Region;
  status: PathinfoStatus;
  direction: Direction;
};

export type FundingDetail = {
  fundingId: number;
  title: string;
  content: string | null;
  hostId: number;
  hostNickname: string;
  departureDate: string;
  status: FundingStatus;
  busType: BusType;
  currentParticipants: number;
  minParticipants: number;
  maxParticipants: number;
  tripType: TripType;
  totalPrice: number;
  finalPrice: number | null;
  minPrice: number;
  maxPrice: number;
  pathinfos: Pathinfo[];
  chatRoomId: number | null;
  isHost: boolean;
  isJoined: boolean;
  createdAt: string;
  myParticipationId: number | null;
  myPaymentStatus: ParticipationPaymentStatus | null;
  isCanceled: boolean;
};

export type ParticipationPaymentStatus =
    | "PENDING"
    | "ACTIVE"
    | "COMPLETED"
    | "CANCELED"
    | "NO_SHOW";

export type FundingCreateResponse = {
  fundingId: number;
  status: FundingStatus;
  createdAt: string;
};

export type FundingPricePreviewResponse = {
  totalPrice: number;
};

export type Seat = {
  seatId: number;
  seatNumber: string;
  status: SeatDisplayStatus;
  mySeat: boolean;
};

export type SeatLayout = {
  pathId: number;
  busType: BusType;
  seats: Seat[];
};

export type ParticipationCreateResponse = {
  participationId: number;
};
