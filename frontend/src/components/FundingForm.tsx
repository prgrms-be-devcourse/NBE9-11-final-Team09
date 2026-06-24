"use client";

import { useMemo, useState } from "react";
import SeatMap from "@/components/seat/SeatMap";
import {
  busTypeLabels,
  normalizePayload,
  regionLabels,
  tripTypeLabels,
} from "@/lib/fundingFormat";
import type { BusType, FundingPayload, Region } from "@/types/funding";
import { REGIONS } from "@/types/funding";
import type { Seat } from "@/types/funding";

type FundingFormProps = {
  initialValue?: FundingPayload;
  mode: "create" | "edit";
  textOnly?: boolean;
  submitting?: boolean;
  onSubmit: (payload: FundingPayload) => Promise<void>;
};

const MAX_PARTICIPANTS_BY_BUS_TYPE = {
  BUS_25: 23,
  BUS_45: 43,
} as const;

const MIN_DEPARTURE_OFFSET_DAYS = 14;

const ONE_WAY_PRICES: Record<string, number> = {
  [priceKey("SEOUL", "BUSAN", "BUS_45")]: 1742400,
  [priceKey("SEOUL", "BUSAN", "BUS_25")]: 1210000,
  [priceKey("SEOUL", "DAEJEON", "BUS_45")]: 774400,
  [priceKey("SEOUL", "DAEJEON", "BUS_25")]: 550000,
  [priceKey("SEOUL", "INCHEON", "BUS_45")]: 726000,
  [priceKey("SEOUL", "INCHEON", "BUS_25")]: 495000,
  [priceKey("SEOUL", "DAEGU", "BUS_45")]: 1369720,
  [priceKey("SEOUL", "DAEGU", "BUS_25")]: 959200,
  [priceKey("SEOUL", "GWANGJU", "BUS_45")]: 1452000,
  [priceKey("SEOUL", "GWANGJU", "BUS_25")]: 1016400,
  [priceKey("SEOUL", "ULSAN", "BUS_45")]: 1650440,
  [priceKey("SEOUL", "ULSAN", "BUS_25")]: 1155000,
};

const defaultPayload: FundingPayload = {
  title: "",
  content: "",
  busType: "BUS_25",
  minParticipants: 10,
  tripType: "ONE_WAY",
  hostOutboundSeatNumber: "",
  hostReturnSeatNumber: null,
  route: {
    departureTime: "",
    returnTime: null,
    departureAddress: "",
    departureRegion: "SEOUL",
    arrivalAddress: "",
    arrivalRegion: "BUSAN",
  },
};

export default function FundingForm({
  initialValue,
  mode,
  textOnly = false,
  submitting = false,
  onSubmit,
}: FundingFormProps) {
  const [payload, setPayload] = useState<FundingPayload>(
    initialValue ?? defaultPayload
  );
  const [originalPayload] = useState<FundingPayload>(
    initialValue ?? defaultPayload
  );
  const [error, setError] = useState("");

  const routeLocked = mode === "edit" && textOnly;
  const maxParticipants = MAX_PARTICIPANTS_BY_BUS_TYPE[payload.busType];
  const isRoundTrip = payload.tripType === "ROUND";
  const busTypeChanged =
    mode === "edit" && payload.busType !== originalPayload.busType;
  const changedFromOneWayToRound =
    mode === "edit" &&
    originalPayload.tripType === "ONE_WAY" &&
    payload.tripType === "ROUND";
  const outboundSeatRequired = mode === "create" || (!textOnly && busTypeChanged);
  const returnSeatRequired =
    payload.tripType === "ROUND" &&
    (mode === "create" ||
      (!textOnly && (busTypeChanged || changedFromOneWayToRound)));
  const canEditOutboundSeat = mode === "create" || (!textOnly && busTypeChanged);
  const canEditReturnSeat =
    mode === "create" ||
    (!textOnly && (busTypeChanged || changedFromOneWayToRound));
  const seatSelectorGridClass = isRoundTrip
    ? "grid grid-cols-2 gap-4"
    : "grid gap-5";
  const minimumDepartureDateTime = useMemo(
    () => toDatetimeLocalValue(addDays(new Date(), MIN_DEPARTURE_OFFSET_DAYS)),
    []
  );
  const departureTimeError = getDepartureTimeError(
    payload.route.departureTime,
    minimumDepartureDateTime
  );
  const returnTimeError = getReturnTimeError(
    payload.tripType,
    payload.route.departureTime,
    payload.route.returnTime
  );
  const priceSummary = getPriceSummary(
    payload.route.departureRegion,
    payload.route.arrivalRegion,
    payload.busType,
    payload.tripType,
    payload.minParticipants,
    maxParticipants
  );

  function getRestoredOutboundSeat(busType: FundingPayload["busType"]) {
    if (mode === "edit" && busType === originalPayload.busType) {
      return originalPayload.hostOutboundSeatNumber;
    }

    return "";
  }

  function getRestoredReturnSeat(
    busType: FundingPayload["busType"],
    tripType: FundingPayload["tripType"]
  ) {
    if (tripType !== "ROUND") {
      return null;
    }

    if (
      mode === "edit" &&
      busType === originalPayload.busType &&
      originalPayload.tripType === "ROUND"
    ) {
      return originalPayload.hostReturnSeatNumber ?? "";
    }

    return "";
  }

  const canSubmit = useMemo(() => {
    if (!payload.title.trim()) {
      return false;
    }
    if (routeLocked) {
      return true;
    }

    if (
      !payload.route.departureTime ||
      !payload.route.departureAddress.trim() ||
      !payload.route.arrivalAddress.trim()
    ) {
      return false;
    }

    if (payload.tripType === "ROUND" && !payload.route.returnTime) {
      return false;
    }

    if (departureTimeError || returnTimeError) {
      return false;
    }

    if (payload.minParticipants < 1 || payload.minParticipants > maxParticipants) {
      return false;
    }

    if (outboundSeatRequired && !payload.hostOutboundSeatNumber.trim()) {
      return false;
    }

    if (returnSeatRequired && !payload.hostReturnSeatNumber?.trim()) {
      return false;
    }

    return true;
  }, [
    maxParticipants,
    outboundSeatRequired,
    payload,
    departureTimeError,
    returnSeatRequired,
    returnTimeError,
    routeLocked,
  ]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!canSubmit) {
      setError("필수 항목을 확인해주세요.");
      return;
    }

    try {
      await onSubmit(normalizePayload(payload));
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "펀딩 요청 처리에 실패했습니다."
      );
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-8">
      {textOnly && (
        <div className="rounded border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          참여자가 있는 펀딩은 제목과 내용만 수정할 수 있습니다.
        </div>
      )}

      <section className="grid gap-4">
        <h2 className="text-lg font-semibold">기본 정보</h2>
        <label className="grid gap-2 text-sm font-medium text-gray-700">
          제목
          <input
            value={payload.title}
            onChange={(event) =>
              setPayload((current) => ({
                ...current,
                title: event.target.value,
              }))
            }
            className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
            placeholder="예: 서울에서 부산 야구장 같이 가요"
          />
        </label>
        <label className="grid gap-2 text-sm font-medium text-gray-700">
          내용
          <textarea
            value={payload.content}
            onChange={(event) =>
              setPayload((current) => ({
                ...current,
                content: event.target.value,
              }))
            }
            className="min-h-32 rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
            placeholder="탑승 안내, 집결 장소, 참고 사항을 적어주세요."
          />
        </label>
      </section>

      <fieldset disabled={routeLocked} className="grid gap-8 disabled:opacity-60">
        <section className="grid gap-4">
          <h2 className="text-lg font-semibold">운행 조건</h2>
          <div className="grid gap-4 md:grid-cols-3 md:items-start">
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              버스
              <select
                value={payload.busType}
                onChange={(event) => {
                  const busType = event.target.value as FundingPayload["busType"];
                  const nextMaxParticipants =
                    MAX_PARTICIPANTS_BY_BUS_TYPE[busType];

                  setPayload((current) => ({
                    ...current,
                    busType,
                    minParticipants: Math.min(
                      current.minParticipants,
                      nextMaxParticipants
                    ),
                    hostOutboundSeatNumber:
                      mode === "edit"
                        ? getRestoredOutboundSeat(busType)
                        : "",
                    hostReturnSeatNumber:
                      mode === "edit"
                        ? getRestoredReturnSeat(busType, current.tripType)
                        : current.tripType === "ROUND"
                          ? ""
                          : null,
                  }));
                }}
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              >
                {Object.entries(busTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <span className="min-h-4 text-xs text-transparent" aria-hidden="true">
                -
              </span>
            </label>
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              최소 인원
              <input
                type="number"
                min={1}
                max={maxParticipants}
                value={payload.minParticipants}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    minParticipants: clamp(
                      Number(event.target.value),
                      1,
                      maxParticipants
                    ),
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              />
              <span className="min-h-4 text-xs text-gray-500">
                최대 참가자 {maxParticipants}명
              </span>
            </label>
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              이동 방식
              <select
                value={payload.tripType}
                onChange={(event) => {
                  const tripType = event.target.value as FundingPayload["tripType"];

                  setPayload((current) => ({
                    ...current,
                    tripType,
                    hostOutboundSeatNumber:
                      mode === "edit"
                        ? getRestoredOutboundSeat(current.busType) ||
                          current.hostOutboundSeatNumber
                        : current.hostOutboundSeatNumber,
                    hostReturnSeatNumber:
                      mode === "edit"
                        ? getRestoredReturnSeat(current.busType, tripType)
                        : tripType === "ROUND"
                          ? ""
                          : null,
                    route: {
                      ...current.route,
                      returnTime:
                        tripType === "ROUND"
                          ? current.route.returnTime ?? ""
                          : null,
                    },
                  }));
                }}
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              >
                {Object.entries(tripTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <span className="min-h-4 text-xs text-transparent" aria-hidden="true">
                -
              </span>
            </label>
          </div>
        </section>

        <section className="grid gap-4">
          <h2 className="text-lg font-semibold">노선</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              출발 시간
              <input
                type="datetime-local"
                min={minimumDepartureDateTime}
                value={payload.route.departureTime}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      departureTime: event.target.value,
                    },
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              />
              <span
                className={`min-h-4 text-xs ${
                  departureTimeError ? "text-red-600" : "text-gray-500"
                }`}
              >
                {departureTimeError || "출발일은 현재 시점으로부터 14일 이후만 가능합니다."}
              </span>
            </label>
            {payload.tripType === "ROUND" && (
              <label className="grid gap-2 text-sm font-medium text-gray-700">
                복귀 출발 시간
                <input
                  type="time"
                  value={toTimeInputValue(payload.route.returnTime)}
                  onChange={(event) =>
                    setPayload((current) => ({
                      ...current,
                      route: {
                        ...current.route,
                        returnTime: event.target.value,
                      },
                    }))
                  }
                  className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
                />
                <span
                  className={`min-h-4 text-xs ${
                    returnTimeError ? "text-red-600" : "text-gray-500"
                  }`}
                >
                  {returnTimeError || "가는 편과 같은 날짜의 시간만 선택합니다."}
                </span>
              </label>
            )}
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <RegionSelect
              label="출발 지역"
              value={payload.route.departureRegion}
              onChange={(departureRegion) =>
                setPayload((current) => ({
                  ...current,
                  route: {
                    ...current.route,
                    departureRegion,
                  },
                }))
              }
            />
            <RegionSelect
              label="도착 지역"
              value={payload.route.arrivalRegion}
              onChange={(arrivalRegion) =>
                setPayload((current) => ({
                  ...current,
                  route: {
                    ...current.route,
                    arrivalRegion,
                  },
                }))
              }
            />
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              출발 주소
              <input
                value={payload.route.departureAddress}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      departureAddress: event.target.value,
                    },
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
                placeholder="예: 잠실종합운동장"
              />
            </label>
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              도착 주소
              <input
                value={payload.route.arrivalAddress}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      arrivalAddress: event.target.value,
                    },
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
                placeholder="예: 사직야구장"
              />
            </label>
          </div>
        </section>

        <section className="grid gap-3 rounded border border-gray-200 bg-gray-50 p-4">
          <h2 className="text-lg font-semibold">예상 요금</h2>
          {priceSummary ? (
            <div className="grid gap-3 text-sm md:grid-cols-3">
              <div>
                <p className="text-gray-500">총 가격</p>
                <p className="text-lg font-bold text-gray-950">
                  {formatWon(priceSummary.totalPrice)}
                </p>
              </div>
              <div>
                <p className="text-gray-500">
                  최소 예상가 ({maxParticipants}명 기준)
                </p>
                <p className="text-lg font-bold text-gray-950">
                  {formatWon(priceSummary.minPrice)}
                </p>
              </div>
              <div>
                <p className="text-gray-500">
                  최대 예상가 ({payload.minParticipants}명 기준)
                </p>
                <p className="text-lg font-bold text-gray-950">
                  {formatWon(priceSummary.maxPrice)}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-sm text-gray-500">
              선택한 노선의 요금을 계산할 수 없습니다.
            </p>
          )}
        </section>

        <section className="grid gap-4">
          <h2 className="text-lg font-semibold">방장 좌석</h2>
          <div className={seatSelectorGridClass}>
            <HostSeatSelector
              title="가는 편 좌석"
              busType={payload.busType}
              selectedSeatNumber={payload.hostOutboundSeatNumber}
              disabled={!canEditOutboundSeat}
              onChange={(seatNumber) =>
                setPayload((current) => ({
                  ...current,
                  hostOutboundSeatNumber: seatNumber,
                }))
              }
            />
            {payload.tripType === "ROUND" && (
              <HostSeatSelector
                title="오는 편 좌석"
                busType={payload.busType}
                selectedSeatNumber={payload.hostReturnSeatNumber ?? ""}
                disabled={!canEditReturnSeat}
                onChange={(seatNumber) =>
                  setPayload((current) => ({
                    ...current,
                    hostReturnSeatNumber: seatNumber,
                  }))
                }
              />
            )}
          </div>
        </section>
      </fieldset>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <button
        type="submit"
        disabled={submitting || !canSubmit}
        className="rounded bg-gray-950 px-5 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
      >
        {submitting ? "처리 중" : mode === "create" ? "펀딩 만들기" : "수정 저장"}
      </button>
    </form>
  );
}

function RegionSelect({
  label,
  value,
  onChange,
}: {
  label: string;
  value: FundingPayload["route"]["departureRegion"];
  onChange: (value: FundingPayload["route"]["departureRegion"]) => void;
}) {
  return (
    <label className="grid gap-2 text-sm font-medium text-gray-700">
      {label}
      <select
        value={value}
        onChange={(event) =>
          onChange(event.target.value as FundingPayload["route"]["departureRegion"])
        }
        className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
      >
        {REGIONS.map((region) => (
          <option key={region} value={region}>
            {regionLabels[region]}
          </option>
        ))}
      </select>
    </label>
  );
}

function HostSeatSelector({
  title,
  busType,
  selectedSeatNumber,
  disabled = false,
  onChange,
}: {
  title: string;
  busType: FundingPayload["busType"];
  selectedSeatNumber: string;
  disabled?: boolean;
  onChange: (seatNumber: string) => void;
}) {
  const seats = useMemo(
    () => createHostSeatMapSeats(busType, selectedSeatNumber),
    [busType, selectedSeatNumber]
  );

  return (
    <div className="grid min-w-0 gap-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="rounded bg-gray-100 px-2 py-1 text-sm font-bold text-gray-900">
          {selectedSeatNumber || "미선택"}
        </p>
      </div>
      <div
        className={`max-w-full overflow-hidden rounded-lg ${
          disabled ? "pointer-events-none opacity-55" : ""
        }`}
        onClickCapture={(event) => event.preventDefault()}
      >
        <div className="[&_.border-dashed]:!h-7 [&_.border-dashed]:!w-7 [&_.gap-1]:!gap-0.5 [&_.gap-2]:!gap-1 [&_.mb-6]:!mb-2 [&_.mt-6]:!mt-2 [&_.mt-8]:!mt-2 [&_.p-6]:!p-2 [&_.p-8]:!p-2 [&_.text-sm]:!text-xs [&_.text-xs]:!text-[9px] [&_.w-20]:!w-5 [&_button]:!h-7 [&_button]:!w-7 [&_button]:!text-[9px]">
          <SeatMap
            busType={busType}
            seats={seats}
            selectedSeatId={seats.find((s) => s.seatNumber ===
                selectedSeatNumber)?.seatId ?? null}
            onSeatClick={(seat) => {
              if (!disabled) {
                onChange(seat.seatNumber);
              }
            }}
          />
        </div>
      </div>
    </div>
  );
}

function createHostSeatMapSeats(
  busType: FundingPayload["busType"],
  selectedSeatNumber: string
): Seat[] {
  const rows = busType === "BUS_25" ? 8 : 11;
  const columns = busType === "BUS_25" ? ["A", "B", "C"] : ["A", "B", "C", "D"];
  const seats: Seat[] = [];

  for (let row = 1; row <= rows; row += 1) {
    for (const column of columns) {
      const seatNumber = `${row}${column}`;

      seats.push({
        seatId: seats.length + 1,
        seatNumber,
        status: "AVAILABLE",
        mySeat: seatNumber === selectedSeatNumber,
      });
    }
  }

  return seats;
}

function priceKey(departureRegion: Region, arrivalRegion: Region, busType: BusType) {
  return [[departureRegion, arrivalRegion].sort().join("-"), busType].join("-");
}

function getPriceSummary(
  departureRegion: Region,
  arrivalRegion: Region,
  busType: BusType,
  tripType: FundingPayload["tripType"],
  minParticipants: number,
  maxParticipants: number
) {
  const oneWayPrice =
    ONE_WAY_PRICES[priceKey(departureRegion, arrivalRegion, busType)];

  if (!oneWayPrice || minParticipants < 1 || maxParticipants < 1) {
    return null;
  }

  const totalPrice = tripType === "ROUND" ? oneWayPrice * 2 : oneWayPrice;

  return {
    totalPrice,
    minPrice: roundUpToHundred(totalPrice / maxParticipants),
    maxPrice: roundUpToHundred(totalPrice / minParticipants),
  };
}

function roundUpToHundred(value: number) {
  return Math.ceil(value / 100) * 100;
}

function formatWon(value: number) {
  return `${value.toLocaleString("ko-KR")}원`;
}

function addDays(date: Date, days: number) {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
}

function toDatetimeLocalValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
}

function toTimeInputValue(value?: string | null) {
  if (!value) {
    return "";
  }

  return value.includes("T") ? value.slice(11, 16) : value.slice(0, 5);
}

function getDepartureTimeError(
  departureTime: string,
  minimumDepartureDateTime: string
) {
  if (!departureTime) {
    return "";
  }

  if (new Date(departureTime) < new Date(minimumDepartureDateTime)) {
    return "출발날짜가 너무 빠릅니다.";
  }

  return "";
}

function getReturnTimeError(
  tripType: FundingPayload["tripType"],
  departureTime: string,
  returnTime?: string | null
) {
  if (tripType !== "ROUND" || !departureTime || !returnTime) {
    return "";
  }

  const returnDateTime = combineDepartureDateAndReturnTime(
    departureTime,
    returnTime
  );

  if (returnDateTime && new Date(returnDateTime) <= new Date(departureTime)) {
    return "돌아오는 출발시간은 가는 편 출발시간보다 늦어야 합니다.";
  }

  return "";
}

function combineDepartureDateAndReturnTime(
  departureTime: string,
  returnTime: string
) {
  const time = toTimeInputValue(returnTime);

  if (!departureTime || !time) {
    return "";
  }

  return `${departureTime.slice(0, 10)}T${time}`;
}

function clamp(value: number, min: number, max: number) {
  if (Number.isNaN(value)) {
    return min;
  }

  return Math.min(max, Math.max(min, value));
}
