"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useFundingLoggedIn } from "@/lib/fundingAuth";
import { deleteFunding, getFunding, cancelParticipation } from "@/lib/fundingApi";
import { getChatRoomByFundingId } from "@/lib/chatApi";
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
    const [copied, setCopied] = useState(false);
    const [error, setError] = useState("");
    const [canceling, setCanceling] = useState(false);
    const [cancelModal, setCancelModal] = useState(false);
    const [cancelSuccess, setCancelSuccess] = useState(false);

    const isHost = Boolean(funding?.isHost);
    const isLoggedIn = useFundingLoggedIn();
    const isJoined = Boolean(funding?.isJoined);
    const isCanceled = Boolean(funding?.isCanceled);
    const isRecruiting = funding?.status === "RECRUITING";
    const canJoinFunding =
        funding?.status === "RECRUITING" || funding?.status === "CONFIRMED";
    const myParticipationId = funding?.myParticipationId ?? null;

    const outboundPathinfo = funding?.pathinfos.find((p) => p.direction === "OUTBOUND");
    const departureTime = outboundPathinfo ? new Date(outboundPathinfo.departureTime) : null;

    const refundDeadline = departureTime
        ? (() => {
            const d = new Date(departureTime);
            d.setDate(d.getDate() - 10);
            d.setHours(0, 0, 0, 0);
            return d;
        })()
        : null;

    const cancelDeadline = departureTime
        ? (() => {
            const d = new Date(departureTime);
            d.setDate(d.getDate() - 7);
            d.setHours(0, 0, 0, 0);
            return d;
        })()
        : null;
    const now = new Date();
    const canCancel = cancelDeadline ? now < cancelDeadline : false;
    const canRefund = refundDeadline ? now < refundDeadline : false;

    const canShowCancel =
        isJoined &&
        canCancel &&
        funding?.myPaymentStatus !== "NO_SHOW";

    function formatDate(date: Date) {
        return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(date.getDate()).padStart(2, "0")}`;
    }

    useEffect(() => {
        let ignore = false;

        window.scrollTo({ top: 0, left: 0 });

        async function load() {
            setLoading(true);
            setError("");
            try {
                const data = await getFunding(fundingId);
                if (!ignore) setFunding(data);
            } catch (err) {
                if (!ignore) {
                    setError(err instanceof Error ? err.message : "펀딩 상세를 불러오지 못했습니다.");
                }
            } finally {
                if (!ignore) setLoading(false);
            }
        }

        if (Number.isFinite(fundingId)) load();

        return () => { ignore = true; };
    }, [fundingId]);

    async function handleDelete() {
        if (!window.confirm("이 펀딩을 취소하시겠습니까?")) return;
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

    async function handleCancelParticipation() {
        if (!myParticipationId) return;
        setCanceling(true);
        setError("");
        try {
            await cancelParticipation(myParticipationId);
            setCancelModal(false);
            setCancelSuccess(true);
            const data = await getFunding(fundingId);
            setFunding(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "참여 취소에 실패했습니다.");
        } finally {
            setCanceling(false);
        }
    }

    async function handleChatClick() {
        setError("");
        try {
            const chatRoom = await getChatRoomByFundingId(fundingId);
            router.push(`/chat/${chatRoom.chatRoomId}?title=${encodeURIComponent(funding?.title ?? "채팅방")}`);
        } catch (err) {
            setError(err instanceof Error ? err.message : "채팅방으로 이동하지 못했습니다.");
        }
    }

    async function handleShare() {
        const url = window.location.href;
        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(url);
            } else {
                const textarea = document.createElement("textarea");
                textarea.value = url;
                textarea.setAttribute("readonly", "");
                textarea.style.position = "fixed";
                textarea.style.left = "-9999px";
                document.body.appendChild(textarea);
                textarea.select();
                document.execCommand("copy");
                document.body.removeChild(textarea);
            }
            setCopied(true);
            window.setTimeout(() => setCopied(false), 1800);
        } catch {
            setError("공유 링크를 복사하지 못했습니다.");
        }
    }

    if (loading) {
        return (
            <main className="min-h-screen bg-[#f3f7f1] px-5 py-10 text-center text-sm font-medium text-slate-500">
                펀딩 상세를 불러오는 중입니다.
            </main>
        );
    }

    if (!funding) {
        return (
            <main className="min-h-screen bg-[#f3f7f1] px-5 py-10">
                <div className="mx-auto max-w-3xl rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                    {error || "펀딩을 찾을 수 없습니다."}
                </div>
            </main>
        );
    }

    const outbound = funding.pathinfos.find((path) => path.direction === "OUTBOUND");
    const returned = funding.pathinfos.find((path) => path.direction === "RETURN");
    const ratio = getParticipantRatio(funding.currentParticipants, funding.maxParticipants);
    const fixedPriceAvailable =
        isFixedPriceStatus(funding.status) && funding.finalPrice != null;
    const currentExpectedPrice =
        funding.currentParticipants >= funding.minParticipants
            ? roundUpToHundred(Number(funding.totalPrice) / (funding.currentParticipants + 1))
            : null;

    return (
        <main className="min-h-screen bg-[#f3f7f1] text-slate-950">
            <div className="mx-auto grid w-full max-w-6xl gap-5 px-5 py-6">
                <div className="flex items-center justify-between gap-3">
                    <Link href="/fundings" className="w-fit text-sm font-semibold text-slate-600 hover:text-[#426f55]">
                        목록으로
                    </Link>
                    <div className="flex items-center gap-2">
                        {isHost && isRecruiting && (
                            <>
                                <Link
                                    href={`/fundings/${funding.fundingId}/edit`}
                                    className="rounded-lg border border-[#dbe7dc] bg-white px-3 py-2 text-sm font-semibold text-slate-800 shadow-sm hover:bg-[#eef5ea]"
                                >
                                    수정
                                </Link>
                                <button
                                    type="button"
                                    onClick={handleDelete}
                                    disabled={deleting}
                                    className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 shadow-sm hover:bg-red-100 disabled:opacity-50"
                                >
                                    {deleting ? "취소 중" : "삭제"}
                                </button>
                            </>
                        )}
                        <button
                            type="button"
                            onClick={handleShare}
                            className="rounded-lg border border-[#dbe7dc] bg-white px-3 py-2 text-sm font-semibold text-slate-800 shadow-sm hover:bg-[#eef5ea]"
                        >
                            {copied ? "링크 복사됨" : "공유"}
                        </button>
                    </div>
                </div>

                <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
                    <section className="rounded-xl border border-[#dbe7dc] bg-white p-6 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">
                        <div className="grid gap-6">
                            <div className="flex flex-wrap gap-2 text-xs font-semibold">
                                <span className={`rounded-full px-2.5 py-1 ${getDetailStatusBadgeClass(funding.status)}`}>{statusLabels[funding.status]}</span>
                                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-slate-700">{busTypeLabels[funding.busType]}</span>
                                <span className="rounded-full bg-[#e8eef7] px-2.5 py-1 text-[#3f6695]">{tripTypeLabels[funding.tripType]}</span>
                                {isHost && <span className="rounded-full bg-[#4f7a61] px-2.5 py-1 text-white">방장</span>}
                                {isJoined && !isHost && <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-emerald-800">참가중</span>}
                                {isCanceled && <span className="rounded-full bg-red-100 px-2.5 py-1 text-red-600">취소한 펀딩</span>}
                            </div>

                            <div className="mt-5">
                                <div>
                                    <h1 className="text-3xl font-bold tracking-tight text-slate-950">
                                        {funding.title}
                                    </h1>
                                    <p className="mt-2 text-sm font-semibold text-slate-500">
                                        방장 {funding.hostNickname}
                                    </p>
                                </div>
                            </div>

                            {funding.content && (
                                <p className="whitespace-pre-wrap rounded-xl bg-[#f8faf9] px-5 py-4 text-sm font-medium leading-7 text-slate-700 ring-1 ring-[#dbe7dc]">
                                    {funding.content}
                                </p>
                            )}

                            <div className="rounded-xl border border-[#dbe7dc] bg-[#f8faf9] p-5">
                                <div className="grid gap-4 md:grid-cols-2">
                                    {outbound && <RouteLine title="가는 편" pathinfo={outbound} />}
                                    {returned && <RouteLine title="오는 편" pathinfo={returned} />}
                                </div>
                            </div>

                            {error && (
                                <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                                    {error}
                                </p>
                            )}

                            <div>
                                <div className="flex items-center justify-between text-sm font-semibold text-slate-600">
                                    <span>모집 현황</span>
                                    <span>{ratio}%</span>
                                </div>
                                <div className="mt-3 h-3 overflow-hidden rounded-full bg-[#e3ebe3]">
                                    <div className="h-full rounded-full bg-[#4f7a61]" style={{ width: `${ratio}%` }} />
                                </div>
                                <div className="mt-4 grid gap-3 md:grid-cols-3">
                                    <Summary label="현재 인원" value={`${funding.currentParticipants}명`} />
                                    <Summary label="최소 인원" value={`${funding.minParticipants}명`} />
                                    <Summary label="최대 인원" value={`${funding.maxParticipants}명`} />
                                </div>
                            </div>

                        </div>
                    </section>

                    <aside className="h-fit rounded-xl border border-[#dbe7dc] bg-white p-5 shadow-[0_10px_28px_rgba(31,41,55,0.06)] lg:sticky lg:top-20">
                        <p className="text-sm font-bold text-slate-950">펀딩 참여</p>
                        <p className="mt-1 text-sm font-medium leading-6 text-slate-500">
                            좌석을 선택하고 예약을 진행합니다.
                        </p>

                        <FundingPricePanel
                            fixedPriceAvailable={fixedPriceAvailable}
                            finalPrice={funding.finalPrice}
                            minPrice={funding.minPrice}
                            maxPrice={funding.maxPrice}
                            currentExpectedPrice={currentExpectedPrice}
                        />

                        <div className="mt-5 grid gap-2">
                            {!isHost && canJoinFunding && (
                                <>
                                    {!isJoined && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                if (!isLoggedIn) { router.push("/login"); return; }
                                                router.push(`/funding/${funding.fundingId}/seats`);
                                            }}
                                            className="h-11 rounded-lg bg-[#4f7a61] px-4 text-sm font-bold text-white shadow-sm hover:bg-[#426f55]"
                                        >
                                            {isCanceled ? "다시 참여하기" : "참여하기"}
                                        </button>
                                    )}

                                    {canShowCancel && (
                                        <button
                                            type="button"
                                            onClick={() => setCancelModal(true)}
                                            className="h-11 rounded-lg border border-red-200 bg-red-50 px-4 text-sm font-semibold text-red-600 hover:bg-red-100"
                                        >
                                            참여 취소
                                        </button>
                                    )}
                                </>
                            )}

                            {isLoggedIn && (
                                <button
                                    type="button"
                                    onClick={handleChatClick}
                                    className="h-11 rounded-lg border border-[#dbe7dc] bg-white px-4 text-sm font-semibold text-slate-800 hover:bg-[#eef5ea]"
                                >
                                    주최자에게 문의하기
                                </button>
                            )}
                        </div>
                    </aside>
                </div>
            </div>

            {cancelModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
                        <h2 className="text-base font-bold text-gray-900 mb-3">참여를 취소하시겠습니까?</h2>
                        <p className="text-sm text-gray-600 mb-1">
                            {canRefund ? (
                                "취소 시 보증금이 전액 환불됩니다."
                            ) : (
                                <>
                                    <span className="font-semibold">{formatDate(refundDeadline!)}</span>{" "}
                                    자정 이후 취소되어 보증금은 환불되지 않습니다.
                                </>
                            )}
                        </p>
                        {cancelDeadline && (
                            <p className="text-sm text-gray-600 mb-1">
                                취소 후에는 <span className="font-semibold">{formatDate(cancelDeadline)}</span> 자정까지 다시 참여할 수 있습니다.
                            </p>
                        )}
                        {canRefund && refundDeadline && (
                            <p className="text-xs text-gray-400 mt-2">
                                ※ {formatDate(refundDeadline)} 자정 이후부터는 보증금 환불이 불가능합니다.
                            </p>
                        )}
                        <div className="flex gap-2 mt-5">
                            <button
                                type="button"
                                onClick={() => setCancelModal(false)}
                                className="flex-1 rounded-lg border border-[#dbe7dc] py-2 text-sm font-semibold text-slate-700 hover:bg-[#eef5ea]"
                            >
                                닫기
                            </button>
                            <button
                                type="button"
                                onClick={handleCancelParticipation}
                                disabled={canceling}
                                className="flex-1 rounded bg-red-500 py-2 text-sm font-semibold text-white hover:bg-red-600 disabled:opacity-50"
                            >
                                {canceling ? "취소 중..." : "확인"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {cancelSuccess && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl text-center">
                        <p className="text-base font-bold text-gray-900 mb-4">참여가 취소되었습니다.</p>
                        <button
                            type="button"
                            onClick={() => setCancelSuccess(false)}
                            className="rounded-lg bg-[#4f7a61] px-6 py-2 text-sm font-semibold text-white hover:bg-[#426f55]"
                        >
                            확인
                        </button>
                    </div>
                </div>
            )}
        </main>
    );
}

function Summary({ label, value }: { label: string; value: string }) {
    return (
        <div className="rounded-lg border border-[#dbe7dc] bg-white p-4">
            <p className="text-xs font-semibold text-slate-500">{label}</p>
            <p className="mt-2 text-lg font-bold text-slate-950">{value}</p>
        </div>
    );
}

function FundingPricePanel({
    fixedPriceAvailable,
    finalPrice,
    minPrice,
    maxPrice,
    currentExpectedPrice,
}: {
    fixedPriceAvailable: boolean;
    finalPrice: number | null;
    minPrice: number;
    maxPrice: number;
    currentExpectedPrice: number | null;
}) {
    if (fixedPriceAvailable) {
        return (
            <div className="mt-5 rounded-xl bg-[#4f7a61] p-4 ring-1 ring-[#426f55]">
                <p className="px-3 text-sm font-bold text-white">확정 1인 가격</p>
                <p className="mt-1 text-2xl font-bold text-white">
                    {finalPrice != null ? formatMoney(finalPrice) : "-"}
                </p>
                <p className="mt-2 text-xs font-medium leading-5 text-white/75">
                    펀딩 확정 이후에는 이 금액을 기준으로 결제가 진행됩니다.
                </p>
            </div>
        );
    }

    return (
        <div className="mt-5 rounded-xl bg-[#4f7a61] p-4 ring-1 ring-[#426f55]">
            <p className="px-3 text-sm font-bold text-white">가격 정보</p>
            <div className="mt-3 grid gap-2">
            <PriceRow label="최소 예상가" value={formatMoney(minPrice)} />
            <PriceRow label="최대 예상가" value={formatMoney(maxPrice)} />
            <PriceRow
                label="현재 기준 예상가"
                value={
                    currentExpectedPrice != null
                        ? formatMoney(currentExpectedPrice)
                        : "최소 인원 모집 후 표시"
                }
            />
            </div>
        </div>
    );
}

function PriceRow({ label, value }: { label: string; value: string }) {
    return (
        <div className="rounded-lg bg-white px-3 py-2.5 ring-1 ring-[#e3ebe3]">
            <p className="whitespace-nowrap text-xs font-semibold text-slate-500">
                {label}
            </p>
            <p className="mt-1 break-keep text-base font-bold leading-6 text-slate-950">
                {value}
            </p>
        </div>
    );
}

function roundUpToHundred(value: number) {
    return Math.ceil(value / 100) * 100;
}

function isFixedPriceStatus(status: FundingDetail["status"]) {
    return status === "CONFIRMED" || status === "CLOSED" || status === "COMPLETED";
}

function getDetailStatusBadgeClass(status: FundingDetail["status"]) {
    if (status === "RECRUITING") {
        return "bg-emerald-100 text-emerald-700";
    }

    if (status === "CONFIRMED") {
        return "bg-sky-100 text-sky-700";
    }

    if (status === "CLOSED") {
        return "bg-amber-100 text-amber-700";
    }

    if (status === "COMPLETED") {
        return "bg-violet-100 text-violet-700";
    }

    if (status === "CANCELLED" || status === "FAILED") {
        return "bg-rose-100 text-rose-700";
    }

    return "bg-slate-100 text-slate-700";
}

function RouteLine({ title, pathinfo }: { title: string; pathinfo: Pathinfo }) {
    return (
        <div className="grid gap-1 border-t border-[#dbe7dc] pt-3 first:border-t-0 first:pt-0">
            <p className="text-sm font-semibold">{title}</p>
            <p className="text-sm text-slate-600">{formatDateTime(pathinfo.departureTime)}</p>
            <p className="text-sm text-slate-800">
                {regionLabels[pathinfo.departureRegion]} {pathinfo.departureAddress} →{" "}
                {regionLabels[pathinfo.arrivalRegion]} {pathinfo.arrivalAddress}
            </p>
        </div>
    );
}
