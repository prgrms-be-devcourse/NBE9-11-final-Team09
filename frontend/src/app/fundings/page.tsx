"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  BusFront,
  CalendarDays,
  ChevronRight,
  CreditCard,
  Flag,
  RotateCcw,
  UserRound,
  UsersRound,
} from "lucide-react";
import busGreenBanner from "@/assets/images/bus_green.png";
import { getFundingList, type FundingListParams } from "@/lib/fundingApi";
import { useFundingLoggedIn } from "@/lib/fundingAuth";
import {
  formatMoney,
  getParticipantRatio,
  regionLabels,
  statusLabels,
  tripTypeLabels,
} from "@/lib/fundingFormat";
import type {
  FundingListItem,
  FundingStatus,
  PageResponse,
  TripType,
} from "@/types/funding";
import { FUNDING_FILTER_STATUSES, REGIONS } from "@/types/funding";

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
  const [selectedTripTypes, setSelectedTripTypes] = useState<TripType[]>([
    "ROUND",
    "ONE_WAY",
  ]);
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

  function resetFilters() {
    setParams({
      statuses: ["RECRUITING", "CONFIRMED"],
      sort: "departureDate,asc",
      page: 0,
      size: 20,
    });
    setSelectedTripTypes(["ROUND", "ONE_WAY"]);
  }

  return (
    <main className="min-h-screen bg-[#f3f7f1] text-slate-900 [font-family:Pretendard,'Noto_Sans_KR','Segoe_UI',system-ui,sans-serif]">
      <section className="bg-[#f3f7f1]">
        <div className="mx-auto w-full max-w-[76rem] px-5 py-3">
          <Image
            src={busGreenBanner}
            alt="같이 가면 버스가 더 저렴해요"
            priority
            className="h-auto w-full rounded-xl object-cover shadow-[0_8px_22px_rgba(31,41,55,0.04)]"
          />
        </div>
      </section>

      <div className="mx-auto grid w-full max-w-7xl gap-4 px-5 pb-4 pt-0">
        <section className="rounded-lg border border-[#dbe7dc] bg-white p-4 shadow-[0_8px_24px_rgba(31,41,55,0.035)]">
          <div className="grid gap-3 xl:grid-cols-[190px_1fr_1fr_minmax(430px,auto)_170px]">
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
            <StatusFilter
              selectedStatuses={params.statuses ?? []}
              onToggle={toggleStatus}
              onAll={() =>
                setParams((current) => ({
                  ...current,
                  statuses: [...FUNDING_FILTER_STATUSES],
                  page: 0,
                }))
              }
            />
            <label className="grid gap-1.5 text-xs font-bold text-slate-800">
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
                className="h-10 rounded-lg border border-[#dbe7dc] bg-white px-3 text-sm font-medium outline-none focus:border-[#4f7a61]"
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-2">
            <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <TripTypeFilters
                selectedTripTypes={selectedTripTypes}
                onToggle={(tripType) =>
                  setSelectedTripTypes((current) => {
                    const tripTypes = current;
                    const selected = tripTypes.includes(tripType);

                    if (selected && tripTypes.length === 1) {
                      return tripTypes;
                    }

                    return selected
                      ? tripTypes.filter((item) => item !== tripType)
                      : [...tripTypes, tripType];
                  })
                }
              />
              <button
                type="button"
                onClick={resetFilters}
                className="inline-flex h-9 w-fit cursor-pointer items-center gap-2 rounded-lg border border-[#dbe7dc] bg-white px-4 text-sm font-medium text-slate-700 hover:border-[#adc7b6]"
              >
                <RotateCcw size={17} strokeWidth={2.3} />
                초기화
              </button>
            </div>
          </div>
        </section>

        {error && (
          <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
            {error}
          </p>
        )}

        <div className="grid gap-4 pb-4 lg:grid-cols-[1fr_286px]">
          <div className="grid gap-2.5">
            {loading ? (
              <p className="rounded-lg border border-slate-200 bg-white py-16 text-center text-sm font-semibold text-slate-500">
                펀딩을 불러오는 중입니다.
              </p>
            ) : page?.content.length ? (
              <section className="grid gap-2.5">
                {page.content.map((funding) => (
                  <FundingCard key={funding.fundingId} funding={funding} />
                ))}
              </section>
            ) : (
              <p className="rounded-lg border border-slate-200 bg-white py-16 text-center text-sm font-semibold text-slate-500">
                조건에 맞는 펀딩이 없습니다.
              </p>
            )}

            {page && page.totalPages > 1 && (
              <nav className="flex items-center justify-center gap-3 pt-2">
                <button
                  type="button"
                  disabled={page.first}
                  onClick={() =>
                    setParams((current) => ({
                      ...current,
                      page: Math.max(0, (current.page ?? 0) - 1),
                    }))
                  }
                  className="h-10 cursor-pointer rounded-lg border border-[#dfe6e2] bg-white px-4 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  이전
                </button>
                <span className="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-[#3b6478] shadow-sm">
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
                  className="h-10 cursor-pointer rounded-lg border border-[#dfe6e2] bg-white px-4 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  다음
                </button>
              </nav>
            )}
          </div>

          <FundingGuide loggedIn={loggedIn} />
        </div>
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
    <label className="grid gap-1.5 text-xs font-bold text-slate-800">
      출발일
      <input
        type="date"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 rounded-lg border border-[#dbe7dc] bg-white px-3 text-sm font-medium outline-none focus:border-[#4f7a61]"
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
    <label className="grid gap-1.5 text-xs font-bold text-slate-800">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 rounded-lg border border-[#dbe7dc] bg-white px-3 text-sm font-medium outline-none focus:border-[#4f7a61]"
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

function StatusFilter({
  selectedStatuses,
  onToggle,
  onAll,
}: {
  selectedStatuses: FundingStatus[];
  onToggle: (status: FundingStatus) => void;
  onAll: () => void;
}) {
  const allSelected = selectedStatuses.length === FUNDING_FILTER_STATUSES.length;

  return (
    <div className="grid gap-1.5 text-xs font-bold text-slate-800">
      상태
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={onAll}
          className={`h-10 cursor-pointer rounded-lg border px-3 text-sm font-bold transition ${
            allSelected
              ? "border-[#4f7a61] bg-[#4f7a61] text-white shadow-sm"
              : "border-[#dbe7dc] bg-white text-slate-700 hover:border-[#adc7b6]"
          }`}
        >
          전체
        </button>
        {FUNDING_FILTER_STATUSES.map((status) => {
          const selected = selectedStatuses.includes(status);

          return (
            <button
              key={status}
              type="button"
              onClick={() => onToggle(status)}
              className={`h-10 cursor-pointer rounded-lg border px-3 text-sm font-bold transition ${
                selected
                  ? "border-[#4f7a61] bg-[#4f7a61] text-white shadow-sm"
                  : "border-[#dbe7dc] bg-white text-slate-700 hover:border-[#adc7b6]"
              }`}
            >
              {statusLabels[status]}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function TripTypeFilters({
  selectedTripTypes,
  onToggle,
}: {
  selectedTripTypes: TripType[];
  onToggle: (tripType: TripType) => void;
}) {
  return (
    <div className="flex flex-wrap gap-8 text-sm font-medium text-slate-700">
      <label className="inline-flex cursor-pointer items-center gap-2">
        <input
          type="checkbox"
          checked={selectedTripTypes.includes("ROUND")}
          onChange={() => onToggle("ROUND")}
          className="h-4 w-4 cursor-pointer accent-[#4f7a61]"
        />
        왕복
      </label>
      <label className="inline-flex cursor-pointer items-center gap-2">
        <input
          type="checkbox"
          checked={selectedTripTypes.includes("ONE_WAY")}
          onChange={() => onToggle("ONE_WAY")}
          className="h-4 w-4 cursor-pointer accent-[#4f7a61]"
        />
        편도
      </label>
    </div>
  );
}

function FundingCard({ funding }: { funding: FundingListItem }) {
  const timeline = getFundingTimelineInfo(funding.departureTime, funding.status);
  const addressRoute = formatAddressRoute(funding);
  const departureLabel = formatDepartureLabel(funding.departureTime);
  const currentPriceAvailable = funding.currentPrice != null;
  const displayPrice = currentPriceAvailable
    ? funding.currentPrice
    : funding.maxPrice;
  const displayPriceLabel = currentPriceAvailable
    ? "현재 예상 1인 가격"
    : "최소 인원 달성 시 1인 가격";
  const accent = getFundingAccent();

  return (
    <article
      className="grid rounded-lg border border-[#dbe7dc] bg-white shadow-[0_6px_18px_rgba(31,41,55,0.03)] transition hover:border-[#c5d8c9] hover:shadow-[0_10px_24px_rgba(31,41,55,0.055)] lg:h-[180px] lg:grid-cols-[minmax(0,1fr)_370px_180px]"
    >
      <div
        className="grid min-w-0 content-center gap-2.5 border-b border-[#eef4ef] px-4 py-4 lg:border-b-0 lg:border-r"
      >
        <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
          <span
            className={`inline-flex h-8 w-8 items-center justify-center rounded-full ${accent.softClass} ${accent.textClass}`}
          >
            <FundingTypeIcon />
          </span>
          <Badge className={getStatusBadgeClass(funding.status)}>
            {statusLabels[funding.status]}
          </Badge>
          <Badge className={getTripTypeBadgeClass(funding.tripType)}>
            {tripTypeLabels[funding.tripType]}
          </Badge>
        </div>

        <div className="grid min-w-0 gap-0.5">
          <h2 className="truncate text-xl font-semibold leading-7 text-slate-900">
            {addressRoute}
          </h2>
          <p className="truncate text-sm font-semibold text-slate-600">
            {funding.title}
          </p>
        </div>

        <div className="grid min-w-0 gap-1 overflow-hidden whitespace-nowrap text-sm font-medium leading-5 text-slate-600">
          <p className="inline-flex min-w-0 items-center gap-1.5 overflow-hidden">
            <CalendarDays
              size={16}
              className="shrink-0 text-slate-500"
              strokeWidth={2.2}
            />
            <span className="truncate">{departureLabel} 출발</span>
          </p>
          <p className="inline-flex min-w-0 items-center gap-1.5 overflow-hidden">
            <UserRound
              size={16}
              className="shrink-0 text-slate-400"
              strokeWidth={2.2}
            />
            <span className="min-w-0 truncate">
              {funding.hostNickname}
            </span>
          </p>
        </div>
      </div>

      <div className="grid min-w-0 content-center gap-3.5 border-b border-[#eef4ef] px-4 py-4 text-center lg:border-b-0 lg:border-r">
        {timeline && (
          <div
            className={`mx-auto inline-grid max-w-full grid-cols-[auto_auto_auto] items-center overflow-hidden rounded-full text-sm font-semibold ${timeline.badgeClass}`}
          >
            <span className="whitespace-nowrap px-2.5 py-1">
              {timeline.labelTitle}
            </span>
            <span className={`h-4 w-px ${timeline.dividerClass}`} />
            <span className="whitespace-nowrap px-2.5 py-1">
              {timeline.label}
            </span>
          </div>
        )}

        <div className="grid gap-2.5">
          <div className="grid grid-cols-3 gap-2 text-center text-xs font-medium text-slate-500">
            <ParticipantStat label="현재" value={`${funding.currentParticipants}명`} />
            <ParticipantStat label="확정" value={`${funding.minParticipants}명`} />
            <ParticipantStat label="최대" value={`${funding.maxParticipants}명`} />
          </div>
          <FundingProgress funding={funding} accent={accent} />
        </div>
      </div>

      <aside className="grid min-w-0 content-center px-4 py-4">
        <div className="grid gap-3">
          <div>
            <p className="whitespace-nowrap text-xs font-semibold leading-5 text-slate-600">
              {displayPriceLabel}
            </p>
            <p className="mt-1 text-[22px] font-semibold text-[#426f55]">
              {formatMoney(displayPrice)}
            </p>
          </div>
          <Link
            href={`/fundings/${funding.fundingId}`}
            className="inline-flex h-9 cursor-pointer items-center justify-center gap-1 rounded-md border border-[#dbe7dc] bg-white text-sm font-semibold text-slate-800 hover:border-[#adc7b6] hover:bg-[#f8faf9]"
          >
            상세보기
            <ChevronRight size={16} strokeWidth={2.4} />
          </Link>
        </div>
      </aside>
    </article>
  );
}

function FundingGuide({ loggedIn }: { loggedIn: boolean }) {
  return (
    <aside className="h-fit break-keep rounded-lg border border-[#dbe7dc] bg-white p-5 shadow-[0_6px_18px_rgba(31,41,55,0.03)] lg:sticky lg:top-4 lg:mb-4 lg:max-h-[calc(100vh-2rem)] lg:overflow-auto">
      <Link
        href={loggedIn ? "/fundings/new" : "/login"}
        className="inline-flex h-11 w-full cursor-pointer items-center justify-center rounded-lg bg-[#4f7a61] text-sm font-semibold text-white shadow-sm hover:bg-[#426f55]"
      >
        + 펀딩 만들기
      </Link>

      <div className="mt-5 border-t border-[#dbe7dc] pt-5">
      <h2 className="text-lg font-bold text-slate-950">이용 방법</h2>
      <div className="mt-5 grid gap-4">
        <GuideStep number="1" title="원하는 펀딩 참여" icon={<UsersRound size={22} />}>
          마음에 드는 노선을 선택하고 좌석을 예약하세요.
        </GuideStep>
        <GuideStep number="2" title="최소 인원 달성" icon={<Flag size={22} />}>
          펀딩 확정일에 최소 인원을 넘으면 자동으로 확정됩니다.
        </GuideStep>
        <GuideStep number="3" title="결제 진행" icon={<CreditCard size={22} />}>
          안내에 따라 보증금과 잔금 결제를 진행합니다.
        </GuideStep>
        <GuideStep number="4" title="버스 탑승" icon={<BusFront size={22} />}>
          정해진 시간과 장소에서 함께 이동을 시작해요.
        </GuideStep>
      </div>

      <div className="mt-6 rounded-lg bg-[#eef5ea] p-4">
        <p className="font-semibold text-[#735f32]">
          함께할수록 더 저렴해져요
        </p>
        <p className="mt-2 text-sm font-medium leading-6 text-slate-600">
          최소 인원 달성 후에도 함께할수록 1인 금액이 낮아집니다.
        </p>
      </div>
      </div>
    </aside>
  );
}

function GuideStep({
  number,
  title,
  icon,
  children,
}: {
  number: string;
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-[38px_34px_1fr] gap-3">
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#eef5ea] text-sm font-semibold text-[#426f55]">
        {number}
      </span>
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#e8f1e7] text-[#4f7a61]">
        {icon}
      </span>
      <div>
        <p className="font-semibold leading-5 text-slate-900">{title}</p>
        <p className="mt-1 text-sm font-medium leading-5 text-slate-500">
          {children}
        </p>
      </div>
    </div>
  );
}

function FundingProgress({
  funding,
  accent,
}: {
  funding: FundingListItem;
  accent: FundingAccent;
}) {
  const currentRatio = getParticipantRatio(
    funding.currentParticipants,
    funding.maxParticipants
  );
  const minRatio = getParticipantRatio(
    funding.minParticipants,
    funding.maxParticipants
  );

  return (
    <div className="grid gap-1.5">
      <div className="relative h-2 rounded-full bg-slate-200">
        <div
          className={`h-full rounded-full ${accent.barClass}`}
          style={{ width: `${currentRatio}%` }}
        />
        <div
          className="absolute top-1/2 h-4 w-0.5 -translate-y-1/2 rounded bg-slate-500"
          style={{ left: `${minRatio}%` }}
        />
      </div>
    </div>
  );
}

function ParticipantStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-r border-slate-100 last:border-r-0">
      <p>{label}</p>
      <p className="mt-1 text-sm font-bold text-slate-950">{value}</p>
    </div>
  );
}

function Badge({
  className,
  children,
}: {
  className: string;
  children: React.ReactNode;
}) {
  return (
    <span className={`rounded-md px-2.5 py-1 ${className}`}>{children}</span>
  );
}

type FundingAccent = {
  softClass: string;
  textClass: string;
  barClass: string;
};

function getFundingAccent(): FundingAccent {
  return {
    softClass: "bg-[#eef5ea]",
    textClass: "text-[#426f55]",
    barClass: "bg-[#7ba46f]",
  };
}

function getStatusBadgeClass(status: FundingStatus) {
  if (status === "RECRUITING") {
    return "bg-[#e8f1e7] text-[#426f55]";
  }

  if (status === "CONFIRMED") {
    return "bg-[#edf4e9] text-[#4f7a61]";
  }

  if (status === "CLOSED") {
    return "bg-[#eef0ef] text-[#5f6d68]";
  }

  if (status === "CANCELLED" || status === "FAILED") {
    return "bg-[#f8eeee] text-[#9a4a4a]";
  }

  return "bg-[#eef0ef] text-[#5f6d68]";
}

function getTripTypeBadgeClass(tripType: FundingListItem["tripType"]) {
  if (tripType === "ROUND") {
    return "bg-[#e8f1e7] text-[#426f55]";
  }

  return "bg-[#eef3f6] text-[#4f6675]";
}

function FundingTypeIcon() {
  return <BusFront size={19} strokeWidth={2.2} />;
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

  return `${formatKoreanShortDate(departure)} ${formatKoreanMeridiemTime(departure)}`;
}

function getFundingTimelineInfo(
  departureTime: string | null,
  status: FundingStatus
) {
  if (["COMPLETED", "FAILED", "CANCELLED"].includes(status)) {
    return null;
  }

  if (!departureTime) {
    return {
      labelTitle: "펀딩 확정일",
      label: "-",
      tone: "confirm" as const,
      badgeClass: "bg-[#e8f1e7] text-[#245c43]",
      dividerClass: "bg-[#b8d3c0]",
    };
  }

  const departure = new Date(departureTime);

  if (Number.isNaN(departure.getTime())) {
    return {
      labelTitle: "펀딩 확정일",
      label: "-",
      tone: "confirm" as const,
      badgeClass: "bg-[#e8f1e7] text-[#245c43]",
      dividerClass: "bg-[#b8d3c0]",
    };
  }

  const confirmationDate = new Date(departure);
  confirmationDate.setDate(
    confirmationDate.getDate() - CONFIRMATION_DAYS_BEFORE_DEPARTURE
  );
  confirmationDate.setHours(0, 0, 0, 0);

  const recruitmentCloseDate = new Date(departure);
  recruitmentCloseDate.setDate(recruitmentCloseDate.getDate() - 1);

  const confirmationDay = startOfDay(confirmationDate);
  const diffDays = Math.ceil(
    (confirmationDay.getTime() - startOfDay(new Date()).getTime()) / 86_400_000
  );

  const showRecruitmentClose =
    status === "CONFIRMED" || status === "CLOSED" || diffDays < 0;
  const labelDate = showRecruitmentClose
    ? recruitmentCloseDate
    : confirmationDate;

  return {
    labelTitle: showRecruitmentClose ? "모집 마감일" : "펀딩 확정일",
    label: formatTimelineDateTime(labelDate),
    tone: showRecruitmentClose ? ("close" as const) : ("confirm" as const),
    badgeClass: showRecruitmentClose
      ? "bg-[#f8eeee] text-[#9a4a4a]"
      : "bg-[#e8f1e7] text-[#245c43]",
    dividerClass: showRecruitmentClose ? "bg-[#e5bcbc]" : "bg-[#b8d3c0]",
  };
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function formatTimelineDateTime(date: Date) {
  return `${formatKoreanShortDate(date)} ${formatKoreanMeridiemTime(date)}`;
}

function formatKoreanShortDate(date: Date) {
  const dateLabel = new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    weekday: "short",
  }).format(date);

  return dateLabel;
}

function formatKoreanMeridiemTime(date: Date) {
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const meridiem = hours < 12 ? "오전" : "오후";
  const displayHours = hours === 0 ? 0 : hours > 12 ? hours - 12 : hours;
  const hourLabel = String(displayHours).padStart(2, "0");

  return `${meridiem} ${hourLabel}시 ${minutes}분`;
}
