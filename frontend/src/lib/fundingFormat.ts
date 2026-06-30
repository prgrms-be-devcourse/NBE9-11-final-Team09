import type {
  BusType,
  FundingDetail,
  FundingPayload,
  FundingStatus,
  Region,
  TripType,
} from "@/types/funding";

export const regionLabels: Record<Region, string> = {
  SEOUL: "서울",
  BUSAN: "부산",
  DAEJEON: "대전",
  INCHEON: "인천",
  DAEGU: "대구",
  GWANGJU: "광주",
  ULSAN: "울산",
};

export const statusLabels: Record<FundingStatus, string> = {
  RECRUITING: "모집중",
  CONFIRMED: "확정",
  CLOSED: "모집마감",
  COMPLETED: "완료",
  FAILED: "실패",
  CANCELLED: "취소",
};

export const busTypeLabels: Record<BusType, string> = {
  BUS_25: "25인승",
  BUS_45: "45인승",
};

export const tripTypeLabels: Record<TripType, string> = {
  ONE_WAY: "편도",
  ROUND: "왕복",
};

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value.replace("T", " ");
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Seoul",
  }).format(date);
}

export function formatMoney(value?: number | null) {
  if (value == null) {
    return "모집 인원 충족 후 확정";
  }

  return `${Number(value).toLocaleString("ko-KR")}원`;
}

export function toDatetimeLocal(value?: string | null) {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

export function toTimeInput(value?: string | null) {
  if (!value) {
    return "";
  }

  return value.includes("T") ? value.slice(11, 16) : value.slice(0, 5);
}

export function fromDetailToPayload(
  detail: FundingDetail
): FundingPayload {
  const outbound = detail.pathinfos.find((path) => path.direction === "OUTBOUND");
  const returned = detail.pathinfos.find((path) => path.direction === "RETURN");

  return {
    title: detail.title,
    content: detail.content ?? "",
    busType: detail.busType,
    minParticipants: detail.minParticipants,
    tripType: detail.tripType,
    hostOutboundSeatNumber: "",
    hostReturnSeatNumber: detail.tripType === "ROUND" ? "" : null,
    route: {
      departureTime: toDatetimeLocal(outbound?.departureTime),
      returnTime: toTimeInput(returned?.departureTime) || null,
      departureAddress: outbound?.departureAddress ?? "",
      departureRegion: outbound?.departureRegion ?? "SEOUL",
      arrivalAddress: outbound?.arrivalAddress ?? "",
      arrivalRegion: outbound?.arrivalRegion ?? "BUSAN",
    },
  };
}

export function normalizePayload(payload: FundingPayload): FundingPayload {
  const toApiDateTime = (value?: string | null) => {
    if (!value) {
      return null;
    }

    return value.length === 16 ? `${value}:00` : value;
  };
  const toReturnDateTime = (
    departureTime: string,
    returnTime?: string | null
  ) => {
    if (!returnTime) {
      return null;
    }

    if (returnTime.length === 5) {
      return `${departureTime.slice(0, 10)}T${returnTime}:00`;
    }

    return toApiDateTime(returnTime);
  };
  const departureTime = toApiDateTime(payload.route.departureTime) ?? "";

  return {
    ...payload,
    title: payload.title.trim(),
    content: payload.content.trim(),
    hostOutboundSeatNumber: payload.hostOutboundSeatNumber.trim(),
    hostReturnSeatNumber:
      payload.tripType === "ROUND"
        ? (payload.hostReturnSeatNumber ?? "").trim()
        : null,
    route: {
      ...payload.route,
      departureTime,
      returnTime:
        payload.tripType === "ROUND" && payload.route.returnTime
          ? toReturnDateTime(departureTime, payload.route.returnTime)
          : null,
      departureAddress: payload.route.departureAddress.trim(),
      arrivalAddress: payload.route.arrivalAddress.trim(),
    },
  };
}

export function getParticipantRatio(current: number, max: number) {
  if (!max) {
    return 0;
  }

  return Math.min(100, Math.round((current / max) * 100));
}
