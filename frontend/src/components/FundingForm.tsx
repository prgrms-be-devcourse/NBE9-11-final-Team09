"use client";

import { useMemo, useState } from "react";
import {
  busTypeLabels,
  normalizePayload,
  regionLabels,
  tripTypeLabels,
} from "@/lib/fundingFormat";
import type { FundingPayload } from "@/types/funding";
import { REGIONS } from "@/types/funding";

type FundingFormProps = {
  initialValue?: FundingPayload;
  mode: "create" | "edit";
  textOnly?: boolean;
  submitting?: boolean;
  onSubmit: (payload: FundingPayload) => Promise<void>;
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
  const [error, setError] = useState("");

  const routeLocked = mode === "edit" && textOnly;
  const seatRequired = mode === "create" || !textOnly;
  const seatColumns = payload.busType === "BUS_25" ? ["A", "B", "C"] : ["A", "B", "C", "D"];
  const seatRows = payload.busType === "BUS_25" ? 8 : 11;

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

    if (seatRequired && !payload.hostOutboundSeatNumber.trim()) {
      return false;
    }

    if (
      seatRequired &&
      payload.tripType === "ROUND" &&
      !payload.hostReturnSeatNumber?.trim()
    ) {
      return false;
    }

    return payload.minParticipants > 0;
  }, [payload, routeLocked, seatRequired]);

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
          <div className="grid gap-4 md:grid-cols-3">
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              버스
              <select
                value={payload.busType}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    busType: event.target.value as FundingPayload["busType"],
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              >
                {Object.entries(busTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              최소 인원
              <input
                type="number"
                min={1}
                value={payload.minParticipants}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    minParticipants: Number(event.target.value),
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              />
            </label>
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              이동 방식
              <select
                value={payload.tripType}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    tripType: event.target.value as FundingPayload["tripType"],
                    hostReturnSeatNumber:
                      event.target.value === "ROUND" ? "" : null,
                    route: {
                      ...current.route,
                      returnTime:
                        event.target.value === "ROUND"
                          ? current.route.returnTime ?? ""
                          : null,
                    },
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 text-base outline-none focus:border-gray-900"
              >
                {Object.entries(tripTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
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
            </label>
            {payload.tripType === "ROUND" && (
              <label className="grid gap-2 text-sm font-medium text-gray-700">
                복귀 출발 시간
                <input
                  type="datetime-local"
                  value={payload.route.returnTime ?? ""}
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
              </label>
            )}
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              출발 지역
              <select
                value={payload.route.departureRegion}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      departureRegion: event.target
                        .value as FundingPayload["route"]["departureRegion"],
                    },
                  }))
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
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              도착 지역
              <select
                value={payload.route.arrivalRegion}
                onChange={(event) =>
                  setPayload((current) => ({
                    ...current,
                    route: {
                      ...current.route,
                      arrivalRegion: event.target
                        .value as FundingPayload["route"]["arrivalRegion"],
                    },
                  }))
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

        <section className="grid gap-4">
          <h2 className="text-lg font-semibold">방장 좌석</h2>
          <div className="grid gap-5 md:grid-cols-2">
            <SeatPicker
              title="가는 편 좌석"
              value={payload.hostOutboundSeatNumber}
              rows={seatRows}
              columns={seatColumns}
              onChange={(seatNumber) =>
                setPayload((current) => ({
                  ...current,
                  hostOutboundSeatNumber: seatNumber,
                }))
              }
            />
            {payload.tripType === "ROUND" && (
              <SeatPicker
                title="오는 편 좌석"
                value={payload.hostReturnSeatNumber ?? ""}
                rows={seatRows}
                columns={seatColumns}
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

function SeatPicker({
  title,
  value,
  rows,
  columns,
  onChange,
}: {
  title: string;
  value: string;
  rows: number;
  columns: string[];
  onChange: (seatNumber: string) => void;
}) {
  return (
    <div className="grid gap-3 rounded border border-gray-200 p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="rounded bg-gray-100 px-2 py-1 text-sm font-bold text-gray-900">
          {value || "미선택"}
        </p>
      </div>
      <div className="grid gap-2">
        {Array.from({ length: rows }, (_, index) => index + 1).map((row) => (
          <div
            key={row}
            className={`grid gap-2 ${columns.length === 3 ? "grid-cols-[1fr_1fr_12px_1fr]" : "grid-cols-[1fr_1fr_12px_1fr_1fr]"}`}
          >
            {columns.map((column, columnIndex) => {
              const seatNumber = `${row}${column}`;
              const selected = value === seatNumber;
              const needsAisle =
                (columns.length === 3 && columnIndex === 2) ||
                (columns.length === 4 && columnIndex === 2);

              return (
                <button
                  key={seatNumber}
                  type="button"
                  onClick={() => onChange(seatNumber)}
                  className={`h-10 rounded border text-sm font-semibold transition ${
                    selected
                      ? "border-gray-950 bg-gray-950 text-white"
                      : "border-gray-300 bg-white text-gray-700 hover:border-gray-700"
                  } ${needsAisle ? "col-start-auto" : ""}`}
                  style={needsAisle ? { gridColumnStart: columnIndex + 2 } : undefined}
                >
                  {seatNumber}
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
