"use client";

import { useEffect, useMemo, useState } from "react";
import SeatMap from "@/components/seat/SeatMap";
import {
  busTypeLabels,
  normalizePayload,
  regionLabels,
  tripTypeLabels,
} from "@/lib/fundingFormat";
import { getFundingPricePreview } from "@/lib/fundingApi";
import type { FundingPayload } from "@/types/funding";
import { REGIONS } from "@/types/funding";
import type { Seat } from "@/types/funding";
import { toTimeInput } from "@/lib/fundingFormat";

type FundingFormProps = {
  initialValue?: FundingPayload;
  mode: "create" | "edit";
  textOnly?: boolean;
  submitting?: boolean;
  onDirtyChange?: (dirty: boolean) => void;
  onSubmit: (payload: FundingPayload) => Promise<void>;
};

const MAX_PARTICIPANTS_BY_BUS_TYPE = {
  BUS_25: 23,
  BUS_45: 43,
} as const;

const MIN_DEPARTURE_OFFSET_DAYS = 14;

const sectionClass =
  "grid gap-5 rounded-xl border border-[#dbe7dc] bg-white p-5 shadow-[0_8px_24px_rgba(31,41,55,0.04)] sm:p-6";
const sectionTitleClass = "text-lg font-bold text-slate-950";
const labelClass = "grid gap-2 text-sm font-semibold text-slate-700";
const inputClass =
  "rounded-lg border border-[#dbe7dc] bg-white px-3 py-2.5 text-base outline-none transition focus:border-[#4f7a61] focus:ring-3 focus:ring-[#4f7a61]/10 disabled:bg-slate-50";

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
  onDirtyChange,
  onSubmit,
}: FundingFormProps) {
  const [payload, setPayload] = useState<FundingPayload>(
    initialValue ?? defaultPayload
  );
  const [originalPayload] = useState<FundingPayload>(
    initialValue ?? defaultPayload
  );
  const [error, setError] = useState("");
  const [pricePreview, setPricePreview] = useState<{
    key: string;
    totalPrice: number | null;
    error: string;
  }>({
    key: "",
    totalPrice: null,
    error: "",
  });

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
  const isDirty = useMemo(
    () => JSON.stringify(payload) !== JSON.stringify(originalPayload),
    [originalPayload, payload]
  );
  const minimumDepartureDateTime = useMemo(
    () =>
      toDatetimeLocalValue(
        startOfDay(addDays(new Date(), MIN_DEPARTURE_OFFSET_DAYS))
      ),
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
  const priceRequestKey = useMemo(
    () =>
      [
        payload.route.departureRegion,
        payload.route.arrivalRegion,
        payload.busType,
        payload.tripType,
      ].join(":"),
    [
      payload.busType,
      payload.route.arrivalRegion,
      payload.route.departureRegion,
      payload.tripType,
    ]
  );
  const hasSameRegion =
    payload.route.departureRegion === payload.route.arrivalRegion;
  const priceTotal =
    pricePreview.key === priceRequestKey ? pricePreview.totalPrice : null;
  const priceLoading =
    !hasSameRegion && pricePreview.key !== priceRequestKey;
  const priceError = hasSameRegion
    ? "출발 지역과 도착 지역이 같으면 요금을 계산할 수 없습니다."
    : pricePreview.key === priceRequestKey
      ? pricePreview.error
      : "";
  const priceSummary = useMemo(
    () => getPriceSummary(priceTotal, payload.minParticipants, maxParticipants),
    [maxParticipants, payload.minParticipants, priceTotal]
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

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

  useEffect(() => {
    let ignore = false;

    if (hasSameRegion) {
      return;
    }

    const timeoutId = window.setTimeout(async () => {
      try {
        const preview = await getFundingPricePreview({
          departureRegion: payload.route.departureRegion,
          arrivalRegion: payload.route.arrivalRegion,
          busType: payload.busType,
          tripType: payload.tripType,
        });

        if (!ignore) {
          setPricePreview({
            key: priceRequestKey,
            totalPrice: preview.totalPrice,
            error: "",
          });
        }
      } catch (err) {
        if (!ignore) {
          setPricePreview({
            key: priceRequestKey,
            totalPrice: null,
            error:
              err instanceof Error ? err.message : "요금을 계산하지 못했습니다.",
          });
        }
      }
    }, 250);

    return () => {
      ignore = true;
      window.clearTimeout(timeoutId);
    };
  }, [
    hasSameRegion,
    payload.busType,
    payload.route.arrivalRegion,
    payload.route.departureRegion,
    payload.tripType,
    priceRequestKey,
  ]);

  useEffect(() => {
    if (!isDirty || submitting) {
      return;
    }

    const confirmMessage =
      "변경사항이 저장되지 않을 수 있습니다. 페이지를 나가시겠습니까?";

    function handleBeforeUnload(event: BeforeUnloadEvent) {
      event.preventDefault();
      event.returnValue = "";
    }

    function handleDocumentClick(event: MouseEvent) {
      const target = event.target;

      if (!(target instanceof Element)) {
        return;
      }

      const link = target.closest("a[href]");

      if (!(link instanceof HTMLAnchorElement)) {
        return;
      }

      if (
        link.target === "_blank" ||
        link.hasAttribute("download") ||
        link.href === window.location.href
      ) {
        return;
      }

      if (!window.confirm(confirmMessage)) {
        event.preventDefault();
        event.stopPropagation();
      }
    }

    window.addEventListener("beforeunload", handleBeforeUnload);
    document.addEventListener("click", handleDocumentClick, true);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
      document.removeEventListener("click", handleDocumentClick, true);
    };
  }, [isDirty, submitting]);

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
    <form onSubmit={handleSubmit} className="grid gap-5">
      {textOnly && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-900">
          참여자가 있는 펀딩은 제목과 내용만 수정할 수 있습니다.
        </div>
      )}

      <section className={sectionClass}>
        <SectionHeader
          number="1"
          title="기본 정보"
          description="목록에서 가장 먼저 보이는 제목과 안내 문구입니다."
        />
        <label className={labelClass}>
          <span className="flex items-center justify-between">
            제목
            <span className="text-xs font-medium text-slate-400">
              {payload.title.length} / 50
            </span>
          </span>
          <input
            value={payload.title}
            onChange={(event) =>
              setPayload((current) => ({
                ...current,
                title: event.target.value,
              }))
            }
            className={inputClass}
            placeholder="예: 서울에서 부산 콘서트 같이 가요"
          />
        </label>
        <label className={labelClass}>
          <span className="flex items-center justify-between">
            내용
            <span className="text-xs font-medium text-slate-400">
              {payload.content.length} / 300
            </span>
          </span>
          <textarea
            value={payload.content}
            onChange={(event) =>
              setPayload((current) => ({
                ...current,
                content: event.target.value,
              }))
            }
            className={`${inputClass} min-h-32 resize-y`}
            placeholder="도착 예정 시간, 집결 위치, 탑승 안내를 적어주세요."
          />
        </label>
      </section>

      <fieldset disabled={routeLocked} className="grid gap-5 disabled:opacity-60">
        <section className={sectionClass}>
          <SectionHeader
            number="2"
            title="운행 조건"
            description="버스 종류와 확정에 필요한 최소 인원을 설정합니다."
          />
          <div className="grid gap-4 md:grid-cols-3 md:items-start">
            <label className="grid gap-3 rounded-lg border border-[#dbe7dc] bg-[#fbfcfb] p-4 text-sm font-semibold text-slate-700">
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
                className={inputClass}
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
            <label className="grid gap-3 rounded-lg border border-[#dbe7dc] bg-[#fbfcfb] p-4 text-sm font-semibold text-slate-700">
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
                className={inputClass}
              />
              <span className="min-h-4 text-xs font-medium text-slate-500">
                최대 참가자 {maxParticipants}명
              </span>
            </label>
            <label className="grid gap-3 rounded-lg border border-[#dbe7dc] bg-[#fbfcfb] p-4 text-sm font-semibold text-slate-700">
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
                className={inputClass}
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

        <section className={sectionClass}>
          <SectionHeader
            number="3"
            title="노선"
            description="출발일과 왕복 시간을 입력하고 지역을 선택합니다."
          />
          <div className="grid gap-4 md:grid-cols-2">
            <label className={labelClass}>
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
                className={inputClass}
              />
              <span
                className={`min-h-4 text-xs ${
                  departureTimeError ? "text-red-600" : "text-slate-500"
                }`}
              >
                {departureTimeError || "출발일은 현재 시점으로부터 14일 이후만 가능합니다."}
              </span>
            </label>
            {payload.tripType === "ROUND" && (
              <label className={labelClass}>
                복귀 출발 시간
                <input
                  type="time"
                  value={toTimeInput(payload.route.returnTime)}
                  onChange={(event) =>
                    setPayload((current) => ({
                      ...current,
                      route: {
                        ...current.route,
                        returnTime: event.target.value,
                      },
                    }))
                  }
                  className={inputClass}
                />
                <span
                  className={`min-h-4 text-xs ${
                    returnTimeError ? "text-red-600" : "text-slate-500"
                  }`}
                >
                  {returnTimeError || "가는 편과 같은 날짜의 시간만 선택합니다."}
                </span>
              </label>
            )}
          </div>
          <div className="grid items-center gap-3 md:grid-cols-[1fr_auto_1fr]">
            <div className="grid gap-4 rounded-lg border border-[#dbe7dc] bg-[#fbfcfb] p-4">
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
              <label className={labelClass}>
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
                  className={inputClass}
                  placeholder="예: 서울역"
                />
              </label>
            </div>
            <div className="flex items-center justify-center">
              <button
                type="button"
                onClick={() =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      departureRegion: current.route.arrivalRegion,
                      arrivalRegion: current.route.departureRegion,
                      departureAddress: current.route.arrivalAddress,
                      arrivalAddress: current.route.departureAddress,
                    },
                  }))
                }
                className="flex h-10 w-10 items-center justify-center rounded-full border border-[#dbe7dc] bg-white text-xl font-semibold text-slate-500 shadow-sm transition hover:border-[#adc7b6] hover:bg-[#eef5ea] hover:text-[#426f55]"
                aria-label="출발지와 도착지 바꾸기"
              >
                ↔
              </button>
            </div>
            <div className="grid gap-4 rounded-lg border border-[#dbe7dc] bg-[#fbfcfb] p-4">
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
              <label className={labelClass}>
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
                  className={inputClass}
                  placeholder="예: 부산아시아드주경기장"
                />
              </label>
            </div>
          </div>
        </section>

        <section className={sectionClass}>
          <SectionHeader
            number="4"
            title="예상 요금"
            description="1인 예상가는 방장 좌석을 포함한 기준이며, 방장에게는 별도 결제가 진행되지 않습니다."
          />
          {priceLoading ? (
            <p className="text-sm font-medium text-slate-500">요금을 계산하는 중입니다.</p>
          ) : priceSummary ? (
            <div className="grid gap-3 text-sm md:grid-cols-3">
              <div className="rounded-lg border border-[#dbe7dc] bg-white p-4">
                <p className="text-xs font-semibold text-slate-500">총 가격</p>
                <p className="mt-1 text-lg font-bold text-slate-950">
                  {formatWon(priceSummary.totalPrice)}
                </p>
              </div>
              <div className="rounded-lg border border-[#dbe7dc] bg-white p-4">
                <p className="text-xs font-semibold text-slate-500">
                  최소 예상가 ({maxParticipants}명 기준)
                </p>
                <p className="mt-1 text-lg font-bold text-[#426f55]">
                  {formatWon(priceSummary.minPrice)}
                </p>
              </div>
              <div className="rounded-lg border border-[#dbe7dc] bg-white p-4">
                <p className="text-xs font-semibold text-slate-500">
                  최대 예상가 ({payload.minParticipants}명 기준)
                </p>
                <p className="mt-1 text-lg font-bold text-slate-950">
                  {formatWon(priceSummary.maxPrice)}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-sm font-medium text-slate-500">
              {priceError || "선택한 노선의 요금을 계산할 수 없습니다."}
            </p>
          )}
        </section>

        <section className={sectionClass}>
          <SectionHeader
            number="5"
            title="방장 좌석"
            description="방장이 탑승할 좌석을 먼저 확보합니다."
          />
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

      <div className="grid gap-3 sm:grid-cols-[180px_1fr]">
        <button
          type="button"
          onClick={() => window.history.back()}
          className="h-12 rounded-lg border border-[#dbe7dc] bg-white px-5 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-[#eef5ea]"
        >
          취소
        </button>
        <button
          type="submit"
          disabled={submitting || !canSubmit}
          className="h-12 rounded-lg bg-[#4f7a61] px-5 text-sm font-bold text-white shadow-sm transition hover:bg-[#426f55] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? "처리 중" : mode === "create" ? "펀딩 만들기" : "수정 저장"}
        </button>
      </div>
    </form>
  );
}

function SectionHeader({
  number,
  title,
  description,
}: {
  number: string;
  title: string;
  description: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#4f7a61] text-sm font-bold text-white">
        {number}
      </span>
      <div>
        <h2 className={sectionTitleClass}>{title}</h2>
        <p className="mt-1 text-sm font-medium leading-6 text-slate-500">
          {description}
        </p>
      </div>
    </div>
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
    <label className={labelClass}>
      {label}
      <select
        value={value}
        onChange={(event) =>
          onChange(event.target.value as FundingPayload["route"]["departureRegion"])
        }
        className={inputClass}
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
    <div className="grid min-w-0 gap-3 rounded-xl border border-[#dbe7dc] bg-[#f8faf9] p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-bold text-slate-800">{title}</p>
        <p className="rounded-full bg-white px-2.5 py-1 text-sm font-bold text-slate-900 ring-1 ring-[#dbe7dc]">
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
            selectedSeatId={seats.find((s) => s.seatNumber === selectedSeatNumber)?.seatId ?? null}
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

function getPriceSummary(
  totalPrice: number | null,
  minParticipants: number,
  maxParticipants: number
) {
  if (!totalPrice || minParticipants < 1 || maxParticipants < 1) {
    return null;
  }

  return {
    totalPrice,
    minPrice: roundUpToHundred(totalPrice / (maxParticipants + 1)),
    maxPrice: roundUpToHundred(totalPrice / (minParticipants + 1)),
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

function startOfDay(date: Date) {
  const nextDate = new Date(date);
  nextDate.setHours(0, 0, 0, 0);
  return nextDate;
}

function toDatetimeLocalValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
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
  const time = toTimeInput(returnTime);

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
