"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getFundingList, type FundingListParams } from "@/lib/fundingApi";
import { useFundingLoggedIn } from "@/lib/fundingAuth";
import {
  formatMoney,
  getParticipantRatio,
  regionLabels,
  statusLabels,
} from "@/lib/fundingFormat";
import type {
  FundingListItem,
  FundingStatus,
  PageResponse,
} from "@/types/funding";
import { FUNDING_STATUSES, REGIONS } from "@/types/funding";

const CONFIRMATION_DAYS_BEFORE_DEPARTURE = 10;

const sortOptions = [
  { value: "departureDate,asc", label: "출발 빠른순" },
  { value: "departureDate,desc", label: "출발 늦은순" },
  { value: "createdAt,desc", label: "최근 생성순" },
  { value: "totalPrice,asc", label: "총액 낮은순" },
  { value: "totalPrice,desc", label: "총액 높은순" },
];

export default function FundingListPage() {
  const [params, setParams] = useState<FundingListParams>({
    statuses: ["RECRUITING", "CONFIRMED"],
    sort: "departureDate,asc",
    page: 0,
    size: 20,
  });
  const [page, setPage] = useState<PageResponse<FundingListItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const loggedIn = useFundingLoggedIn();

  useEffect(() => {
    let ignore = false;

    async function load() {
      setLoading(true);
      setError("");

      try {
        const data = await getFundingList(params);
        if (!ignore) {
          setPage(data);
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err instanceof Error ? err.message : "펀딩 목록을 불러오지 못했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      ignore = true;
    };
  }, [params]);

  function toggleStatus(status: FundingStatus) {
    setParams((current) => {
      const statuses = current.statuses ?? [];
      const selected = statuses.includes(status);

      if (selected && statuses.length === 1) {
        return current;
      }

      return {
        ...current,
        statuses: selected
          ? statuses.filter((item) => item !== status)
          : [...statuses, status],
        page: 0,
      };
    });
  }

  return (
    <main className="min-h-screen bg-gray-50 text-gray-950">
      <div className="mx-auto grid w-full max-w-6xl gap-8 px-5 py-8">
        <header className="flex flex-col gap-4 border-b border-gray-200 pb-6 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm font-semibold text-gray-500">모여타</p>
            <h1 className="mt-1 text-3xl font-bold">펀딩 찾기</h1>
          </div>
          <Link
            href={loggedIn ? "/fundings/new" : "/login"}
            className="inline-flex w-fit items-center justify-center rounded bg-gray-950 px-4 py-2 text-sm font-semibold text-white"
          >
            펀딩 만들기
          </Link>
        </header>

        <section className="grid gap-4 rounded border border-gray-200 bg-white p-4">
          <div className="grid gap-3 md:grid-cols-4">
            <FilterDate
              value={params.departureDate ?? ""}
              onChange={(value) =>
                setParams((current) => ({
                  ...current,
                  departureDate: value || undefined,
                  page: 0,
                }))
              }
            />
            <FilterRegion
              label="출발 지역"
              value={params.departureRegion ?? ""}
              onChange={(value) =>
                setParams((current) => ({
                  ...current,
                  departureRegion:
                    (value as FundingListParams["departureRegion"]) || "",
                  page: 0,
                }))
              }
            />
            <FilterRegion
              label="도착 지역"
              value={params.arrivalRegion ?? ""}
              onChange={(value) =>
                setParams((current) => ({
                  ...current,
                  arrivalRegion:
                    (value as FundingListParams["arrivalRegion"]) || "",
                  page: 0,
                }))
              }
            />
            <label className="grid gap-2 text-sm font-medium text-gray-700">
              정렬
              <select
                value={params.sort}
                onChange={(event) =>
                  setParams((current) => ({
                    ...current,
                    sort: event.target.value,
                    page: 0,
                  }))
                }
                className="rounded border border-gray-300 px-3 py-2 outline-none focus:border-gray-900"
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="flex flex-wrap gap-2">
            {FUNDING_STATUSES.map((status) => {
              const selected = params.statuses?.includes(status);

              return (
                <button
                  key={status}
                  type="button"
                  onClick={() => toggleStatus(status)}
                  className={`rounded border px-3 py-2 text-sm font-medium ${
                    selected
                      ? "border-gray-950 bg-gray-950 text-white"
                      : "border-gray-300 bg-white text-gray-700"
                  }`}
                >
                  {statusLabels[status]}
                </button>
              );
            })}
          </div>
        </section>

        {error && (
          <p className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </p>
        )}

        {loading ? (
          <p className="py-10 text-center text-sm text-gray-500">
            펀딩을 불러오는 중입니다.
          </p>
        ) : page?.content.length ? (
          <section className="grid gap-4">
            {page.content.map((funding) => (
              <FundingCard key={funding.fundingId} funding={funding} />
            ))}
          </section>
        ) : (
          <p className="py-10 text-center text-sm text-gray-500">
            조건에 맞는 펀딩이 없습니다.
          </p>
        )}

        {page && page.totalPages > 1 && (
          <nav className="flex items-center justify-center gap-3">
            <button
              type="button"
              disabled={page.first}
              onClick={() =>
                setParams((current) => ({
                  ...current,
                  page: Math.max(0, (current.page ?? 0) - 1),
                }))
              }
              className="rounded border border-gray-300 px-3 py-2 text-sm disabled:opacity-40"
            >
              이전
            </button>
            <span className="text-sm text-gray-600">
              {page.page + 1} / {page.totalPages}
            </span>
            <button
              type="button"
              disabled={page.last}
              onClick={() =>
                setParams((current) => ({
                  ...current,
                  page: (current.page ?? 0) + 1,
                }))
              }
              className="rounded border border-gray-300 px-3 py-2 text-sm disabled:opacity-40"
            >
              다음
            </button>
          </nav>
        )}
      </div>
    </main>
  );
}

function FilterDate({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="grid gap-2 text-sm font-medium text-gray-700">
      출발일
      <input
        type="date"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded border border-gray-300 px-3 py-2 outline-none focus:border-gray-900"
      />
    </label>
  );
}

function FilterRegion({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="grid gap-2 text-sm font-medium text-gray-700">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded border border-gray-300 px-3 py-2 outline-none focus:border-gray-900"
      >
        <option value="">전체</option>
        {REGIONS.map((region) => (
          <option key={region} value={region}>
            {regionLabels[region]}
          </option>
        ))}
      </select>
    </label>
  );
}

function FundingCard({ funding }: { funding: FundingListItem }) {
  const confirmation = getFundingConfirmationInfo(funding.departureTime);
  const addressRoute = formatAddressRoute(funding);
  const departureLabel = formatDepartureLabel(funding.departureTime);

  return (
    <Link
      href={`/fundings/${funding.fundingId}`}
      className="grid gap-5 rounded border border-gray-200 bg-white p-5 transition hover:border-gray-400 md:grid-cols-[1fr_340px]"
    >
      <div className="grid content-start gap-8">
        <div className="grid gap-7">
          <div className="flex flex-wrap gap-2 text-xs font-semibold">
            <span className="rounded bg-gray-100 px-2 py-1">
              {statusLabels[funding.status]}
            </span>
          </div>

          <div className="grid gap-1">
            <h2 className="text-xl font-bold">{funding.title}</h2>
            <p className="text-sm font-semibold text-gray-700">
              방장 {funding.hostNickname}
            </p>
          </div>

          <div className="grid gap-2">
            <p className="text-xl font-bold text-gray-950">{addressRoute}</p>
            <p className="text-lg font-bold text-gray-950">{departureLabel}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1 text-sm">
          <p className="text-gray-700">펀딩 확정일 {confirmation.label}</p>
          <p className="font-semibold text-red-600">
            {confirmation.remainingLabel}
          </p>
        </div>
      </div>

      <aside className="grid gap-4 rounded border border-gray-200 bg-gray-50 p-4">
        <div className="grid gap-1">
          <p className="text-xs font-semibold text-gray-500">현재 예상 1인 가격</p>
          <p className="text-2xl font-bold text-gray-950">
            {formatMoney(funding.currentPrice)}
          </p>
          <div className="grid gap-1 text-xs text-gray-600">
            <p>최소 {formatMoney(funding.minPrice)}</p>
            <p>최대 {formatMoney(funding.maxPrice)}</p>
            <p>총 금액 {formatMoney(funding.totalPrice)}</p>
          </div>
        </div>

        <div className="grid gap-2">
          <p className="text-sm font-semibold text-gray-900">
            현재 {funding.currentParticipants}명 · 확정{" "}
            {funding.minParticipants}명 · 최대 {funding.maxParticipants}명
          </p>
          <FundingProgress funding={funding} />
        </div>
      </aside>
    </Link>
  );
}

function FundingProgress({ funding }: { funding: FundingListItem }) {
  const currentRatio = getParticipantRatio(
    funding.currentParticipants,
    funding.maxParticipants
  );
  const minRatio = getParticipantRatio(
    funding.minParticipants,
    funding.maxParticipants
  );
  const confirmed = funding.currentParticipants >= funding.minParticipants;

  return (
    <div className="grid gap-2">
      <div className="relative h-3 rounded bg-gray-200">
        <div
          className={`h-full rounded ${confirmed ? "bg-gray-950" : "bg-gray-700"}`}
          style={{ width: `${currentRatio}%` }}
        />
        <div
          className="absolute top-[-5px] h-5 w-0.5 bg-emerald-600"
          style={{ left: `${minRatio}%` }}
        />
        <div
          className="absolute top-1/2 h-5 w-5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-gray-950 shadow"
          style={{ left: `${currentRatio}%` }}
        />
      </div>
      <div className="flex items-center justify-between gap-3 text-xs text-gray-600">
        <span>0명</span>
        <span className="font-semibold text-emerald-700">
          확정 {funding.minParticipants}명
        </span>
        <span>{funding.maxParticipants}명</span>
      </div>
    </div>
  );
}

function formatAddressRoute(funding: FundingListItem) {
  return `${funding.departureAddress ?? "-"} → ${funding.arrivalAddress ?? "-"}`;
}

function formatDepartureLabel(departureTime: string | null) {
  if (!departureTime) {
    return "출발일 미정";
  }

  const departure = new Date(departureTime);

  if (Number.isNaN(departure.getTime())) {
    return departureTime.replace("T", " ");
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(departure);
}

function getFundingConfirmationInfo(departureTime: string | null) {
  if (!departureTime) {
    return {
      label: "-",
      remainingLabel: "확정일 미정",
    };
  }

  const departure = new Date(departureTime);

  if (Number.isNaN(departure.getTime())) {
    return {
      label: "-",
      remainingLabel: "확정일 미정",
    };
  }

  const confirmationDate = new Date(departure);
  confirmationDate.setDate(
    confirmationDate.getDate() - CONFIRMATION_DAYS_BEFORE_DEPARTURE
  );

  const today = startOfDay(new Date());
  const confirmationDay = startOfDay(confirmationDate);
  const diffDays = Math.ceil(
    (confirmationDay.getTime() - today.getTime()) / 86_400_000
  );

  return {
    label: new Intl.DateTimeFormat("ko-KR", {
      month: "short",
      day: "numeric",
      weekday: "short",
    }).format(confirmationDate),
    remainingLabel: formatConfirmationRemaining(diffDays),
  };
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function formatConfirmationRemaining(diffDays: number) {
  if (diffDays > 0) {
    return `확정까지 ${diffDays}일`;
  }

  if (diffDays === 0) {
    return "오늘 확정";
  }

  return `확정일 ${Math.abs(diffDays)}일 지남`;
}
