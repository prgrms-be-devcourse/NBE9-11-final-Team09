"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { deleteFunding, getFunding } from "@/lib/fundingApi";
import {
  busTypeLabels,
  formatDateTime,
  formatMoney,
  getParticipantRatio,
  regionLabels,
  statusLabels,
  tripTypeLabels,
} from "@/lib/fundingFormat";
import type { FundingDetail, Pathinfo } from "@/types/funding";

export default function FundingDetailPage() {
  const params = useParams<{ fundingId: string }>();
  const router = useRouter();
  const fundingId = Number(params.fundingId);
  const [funding, setFunding] = useState<FundingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");

  const isHost = Boolean(funding?.isHost);
  const isJoined = Boolean(funding?.isJoined);

  useEffect(() => {
    let ignore = false;

    async function load() {
      setLoading(true);
      setError("");

      try {
        const data = await getFunding(fundingId);
        if (!ignore) {
          setFunding(data);
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err instanceof Error ? err.message : "펀딩 상세를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    if (Number.isFinite(fundingId)) {
      load();
    }

    return () => {
      ignore = true;
    };
  }, [fundingId]);

  async function handleDelete() {
    if (!window.confirm("이 펀딩을 취소하시겠습니까?")) {
      return;
    }

    setDeleting(true);
    setError("");

    try {
      await deleteFunding(fundingId);
      router.push("/fundings");
    } catch (err) {
      setError(err instanceof Error ? err.message : "펀딩 취소에 실패했습니다.");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10 text-center text-sm text-gray-500">
        펀딩 상세를 불러오는 중입니다.
      </main>
    );
  }

  if (!funding) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10">
        <div className="mx-auto max-w-3xl rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error || "펀딩을 찾을 수 없습니다."}
        </div>
      </main>
    );
  }

  const outbound = funding.pathinfos.find(
    (path) => path.direction === "OUTBOUND"
  );
  const returned = funding.pathinfos.find((path) => path.direction === "RETURN");
  const ratio = getParticipantRatio(
    funding.currentParticipants,
    funding.maxParticipants
  );

  return (
    <main className="min-h-screen bg-gray-50 text-gray-950">
      <div className="mx-auto grid w-full max-w-5xl gap-8 px-5 py-8">
        <Link href="/fundings" className="w-fit text-sm font-medium text-gray-600">
          목록으로
        </Link>

        <section className="grid gap-6 rounded border border-gray-200 bg-white p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <div className="flex flex-wrap gap-2 text-xs font-semibold">
                <span className="rounded bg-gray-100 px-2 py-1">
                  {statusLabels[funding.status]}
                </span>
                <span className="rounded bg-gray-100 px-2 py-1">
                  {busTypeLabels[funding.busType]}
                </span>
                <span className="rounded bg-gray-100 px-2 py-1">
                  {tripTypeLabels[funding.tripType]}
                </span>
                {isHost && (
                  <span className="rounded bg-gray-950 px-2 py-1 text-white">
                    방장
                  </span>
                )}
                {isJoined && !isHost && (
                  <span className="rounded bg-emerald-100 px-2 py-1 text-emerald-800">
                    참가중
                  </span>
                )}
              </div>
              <h1 className="mt-4 text-3xl font-bold">{funding.title}</h1>
              <p className="mt-2 text-sm text-gray-600">
                방장 {funding.hostNickname}
              </p>
            </div>

            <div className="flex gap-2">
              {isHost && (
                <>
                  <Link
                    href={`/fundings/${funding.fundingId}/edit`}
                    className="rounded border border-gray-300 px-4 py-2 text-sm font-semibold"
                  >
                    수정
                  </Link>
                  <button
                    type="button"
                    onClick={handleDelete}
                    disabled={deleting}
                    className="rounded border border-red-300 px-4 py-2 text-sm font-semibold text-red-700 disabled:opacity-50"
                  >
                    {deleting ? "취소 중" : "삭제"}
                  </button>
                </>
              )}
              {!isHost && !isJoined && funding.status === "RECRUITING" && (
                <Link
                  href={`/funding/${funding.fundingId}/seats`}
                  className="rounded bg-gray-950 px-4 py-2 text-sm font-semibold text-white"
                >
                  참여하기
                </Link>
              )}
            </div>
          </div>

          {error && (
              <p className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {error}
              </p>
          )}

          {funding.status === "RECRUITING" && !isJoined && (
              <div className="flex justify-end">
                <button
                    type="button"
                    onClick={() => router.push(`/funding/${fundingId}/seats`)}
                    className="rounded bg-gray-950 px-6 py-3 text-sm font-semibold text-white hover:bg-gray-800"
                >
                  원하는 좌석 선택하기 →
                </button>
              </div>
          )}
          <div className="grid gap-4 md:grid-cols-4">
            <Summary label="현재 인원" value={`${funding.currentParticipants}명`} />
            <Summary label="최소 인원" value={`${funding.minParticipants}명`} />
            <Summary label="최대 인원" value={`${funding.maxParticipants}명`} />
            <Summary label="총 대절 금액" value={formatMoney(funding.totalPrice)} />
          </div>

          <div className="grid gap-2">
            <div className="flex items-center justify-between text-sm text-gray-600">
              <span>모집 현황</span>
              <span>{ratio}%</span>
            </div>
            <div className="h-3 overflow-hidden rounded bg-gray-100">
              <div className="h-full bg-gray-950" style={{ width: `${ratio}%` }} />
            </div>
          </div>

          <div className="grid gap-3 rounded border border-gray-200 p-4">
            <h2 className="text-lg font-semibold">노선</h2>
            {outbound && <RouteLine title="가는 편" pathinfo={outbound} />}
            {returned && <RouteLine title="오는 편" pathinfo={returned} />}
          </div>

          <div className="grid gap-3 rounded border border-gray-200 p-4">
            <h2 className="text-lg font-semibold">가격</h2>
            <div className="grid gap-3 md:grid-cols-3">
              <Summary label="최소 예상가" value={formatMoney(funding.minPrice)} />
              <Summary label="최대 예상가" value={formatMoney(funding.maxPrice)} />
              <Summary
                label="현재 기준 예상가"
                value={
                  funding.currentParticipants >= funding.minParticipants
                    ? formatMoney(
                        Math.ceil(
                          Number(funding.totalPrice) / funding.currentParticipants
                        )
                      )
                    : "최소 인원 모집 후 표시"
                }
              />
            </div>
          </div>

          {funding.content && (
            <div className="grid gap-3 rounded border border-gray-200 p-4">
              <h2 className="text-lg font-semibold">안내</h2>
              <p className="whitespace-pre-wrap text-sm leading-6 text-gray-700">
                {funding.content}
              </p>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border border-gray-200 p-4">
      <p className="text-xs font-semibold text-gray-500">{label}</p>
      <p className="mt-2 text-lg font-bold">{value}</p>
    </div>
  );
}

function RouteLine({ title, pathinfo }: { title: string; pathinfo: Pathinfo }) {
  return (
    <div className="grid gap-1 border-t border-gray-100 pt-3 first:border-t-0 first:pt-0">
      <p className="text-sm font-semibold">{title}</p>
      <p className="text-sm text-gray-600">
        {formatDateTime(pathinfo.departureTime)}
      </p>
      <p className="text-sm text-gray-800">
        {regionLabels[pathinfo.departureRegion]} {pathinfo.departureAddress} →{" "}
        {regionLabels[pathinfo.arrivalRegion]} {pathinfo.arrivalAddress}
      </p>
    </div>
  );
}
