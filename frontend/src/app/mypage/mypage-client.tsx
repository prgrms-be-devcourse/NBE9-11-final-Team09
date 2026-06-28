"use client";

import { cancelParticipation } from "@/lib/fundingApi";

import {
  AuthenticationRequiredError,
  clearAccessToken,
  getMyDashboard,
  getMyHistory,
  getMyParticipations,
  logoutMember,
  updateMyProfile,
  withdrawMember,
} from "@/lib/member-api";
import type {
  HistoryTab,
  MemberFunding,
  MemberHistoryPages,
  MemberParticipation,
  MemberPayment,
  MemberProfile,
  MyParticipation,
  PageResponse,
} from "@/types/member";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";

const PHONE_REGEX = /^010-\d{4}-\d{4}$/;

const EMPTY_PAGES: MemberHistoryPages = {
  participations: emptyPage<MemberParticipation>(),
  fundings: emptyPage<MemberFunding>(),
  payments: emptyPage<MemberPayment>(),
};

const TAB_LABELS: Record<HistoryTab, string> = {
  participations: "참여 내역",
  fundings: "모집 내역",
  payments: "결제 내역",
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "진행 중",
  PENDING: "대기 중",
  CANCELED: "취소",
  CANCELLED: "취소",
  COMPLETED: "완료",
  CONFIRMED: "확정",
  RECRUITING: "모집 중",
  FAILED: "실패",
  NO_SHOW: "미탑승",
  PAID: "결제 완료",
  REFUNDED: "환불 완료",
  DEPOSIT: "보증금",
  BALANCE: "잔액",
};

function emptyPage<T>(): PageResponse<T> {
  return {
    content: [],
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };
}

function formatDate(value: string, includeTime = false) {
  if (!value) return "-";

  const normalized = value.includes("T") ? value : `${value}T00:00:00`;
  const date = new Date(normalized);

  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    ...(includeTime
      ? { hour: "2-digit", minute: "2-digit", hour12: false }
      : {}),
  }).format(date);
}

function formatCurrency(value: number | string) {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? `${new Intl.NumberFormat("ko-KR").format(amount)}원`
    : "-";
}

function statusLabel(status: string) {
  return STATUS_LABELS[status] ?? status;
}

function statusTone(status: string) {
  if (["ACTIVE", "RECRUITING", "PAID"].includes(status)) {
    return "bg-emerald-50 text-emerald-700 ring-emerald-600/15";
  }
  if (["CONFIRMED", "COMPLETED"].includes(status)) {
    return "bg-blue-50 text-blue-700 ring-blue-600/15";
  }
  if (["CANCELED", "CANCELLED", "FAILED", "NO_SHOW"].includes(status)) {
    return "bg-rose-50 text-rose-700 ring-rose-600/15";
  }
  return "bg-amber-50 text-amber-700 ring-amber-600/15";
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${statusTone(status)}`}
    >
      {statusLabel(status)}
    </span>
  );
}

function LoadingView() {
  return (
    <main className="grid min-h-screen place-items-center bg-[#f7f8f5] px-6">
      <div className="text-center">
        <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-[#dfe5dc] border-t-[#235347]" />
        <p className="text-sm font-medium text-slate-600">
          마이페이지를 불러오고 있습니다.
        </p>
      </div>
    </main>
  );
}

export default function MypageClient() {
  const router = useRouter();
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [histories, setHistories] =
    useState<MemberHistoryPages>(EMPTY_PAGES);
  const [myParticipations, setMyParticipations] = useState<MyParticipation[]>([]);
  const [activeTab, setActiveTab] = useState<HistoryTab>("participations");
  const [nickname, setNickname] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState("");
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const moveToLogin = useCallback(() => {
    clearAccessToken();
    router.replace("/login");
  }, [router]);

  const handleRequestError = useCallback(
    (requestError: unknown, fallback: string) => {
      if (requestError instanceof AuthenticationRequiredError) {
        moveToLogin();
        return;
      }

      setError(requestError instanceof Error ? requestError.message : fallback);
    },
    [moveToLogin],
  );

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      try {
        const dashboard = await getMyDashboard();
        if (cancelled) return;

        setProfile(dashboard.profile);
        setNickname(dashboard.profile.nickname);
        setPhoneNumber(dashboard.profile.phoneNumber);
        setHistories(dashboard.histories);
      } catch (requestError) {
        if (!cancelled) {
          handleRequestError(requestError, "마이페이지 정보를 불러오지 못했습니다.");
        }
        return;
      } finally {
        if (!cancelled) setLoading(false);
      }

      try {
        const participations = await getMyParticipations();
        if (!cancelled) setMyParticipations(participations);
      } catch (requestError) {
        if (!cancelled) {
          console.warn("참여 내역 조회 실패:", requestError);
        }
      }
    }

    void loadDashboard();
    return () => {
      cancelled = true;
    };
  }, [handleRequestError]);

  const currentPage = histories[activeTab];

  const joinedDuration = useMemo(() => {
    if (!profile) return "";
    const joined = new Date(profile.createdAt);
    const now = new Date();
    const months = Math.max(
      1,
      (now.getFullYear() - joined.getFullYear()) * 12 +
        now.getMonth() -
        joined.getMonth() +
        1,
    );
  }, [profile]);

  async function handleUpdateProfile(event: React.FormEvent) {
    event.preventDefault();
    setFormError("");
    setSuccessMessage("");

    const trimmedNickname = nickname.trim();
    const trimmedPhoneNumber = phoneNumber.trim();

    if (!trimmedNickname) {
      setFormError("닉네임을 입력해주세요.");
      return;
    }
    if (!PHONE_REGEX.test(trimmedPhoneNumber)) {
      setFormError("전화번호는 010-0000-0000 형식으로 입력해주세요.");
      return;
    }

    setSaving(true);
    try {
      const updated = await updateMyProfile({
        nickname: trimmedNickname,
        phoneNumber: trimmedPhoneNumber,
      });
      setProfile((current) =>
        current
          ? {
              ...current,
              nickname: updated.nickname,
              phoneNumber: updated.phoneNumber,
            }
          : current,
      );
      setNickname(updated.nickname);
      setPhoneNumber(updated.phoneNumber);
      setSuccessMessage("회원정보가 수정되었습니다.");
    } catch (requestError) {
      if (requestError instanceof AuthenticationRequiredError) {
        moveToLogin();
      } else {
        setFormError(
          requestError instanceof Error
            ? requestError.message
            : "회원정보를 수정하지 못했습니다.",
        );
      }
    } finally {
      setSaving(false);
    }
  }

  async function moveHistoryPage(page: number) {
    setHistoryLoading(true);
    setError("");

    try {
      const result = await getMyHistory(activeTab, page);
      setHistories((current) => ({
        ...current,
        [activeTab]: result,
      }));
    } catch (requestError) {
      handleRequestError(requestError, "이용 내역을 불러오지 못했습니다.");
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logoutMember();
    } catch (requestError) {
      if (!(requestError instanceof AuthenticationRequiredError)) {
        clearAccessToken();
      }
    } finally {
      setLoggingOut(false);
      router.replace("/login");
    }
  }

  function closeWithdrawModal() {
    if (withdrawing) return;
    setWithdrawModalOpen(false);
    setWithdrawPassword("");
    setWithdrawError("");
  }

  async function handleWithdraw(event: React.FormEvent) {
    event.preventDefault();
    setWithdrawError("");

    if (!withdrawPassword) {
      setWithdrawError("비밀번호를 입력해주세요.");
      return;
    }

    setWithdrawing(true);
    try {
      await withdrawMember({ password: withdrawPassword });
      router.replace("/login");
    } catch (requestError) {
      if (requestError instanceof AuthenticationRequiredError) {
        moveToLogin();
        return;
      }

      setWithdrawError(
        requestError instanceof Error
          ? requestError.message
          : "회원탈퇴를 처리하지 못했습니다.",
      );
    } finally {
      setWithdrawing(false);
    }
  }
  if (loading) return <LoadingView />;

  if (!profile) {
    return (
      <main className="grid min-h-screen place-items-center bg-[#f7f8f5] px-6">
        <div className="max-w-md rounded-3xl bg-white p-10 text-center shadow-sm ring-1 ring-slate-200">
          <p className="text-lg font-bold text-slate-900">
            마이페이지를 열지 못했습니다.
          </p>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            {error || "잠시 후 다시 시도해주세요."}
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-6 rounded-xl bg-[#235347] px-5 py-3 text-sm font-bold text-white"
          >
            다시 시도
          </button>
        </div>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-[#f7f8f5] text-slate-900">
      <header className="border-b border-slate-200/80 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 sm:px-8">
          <Link href="/" className="flex items-center gap-3">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-[#235347] text-lg text-white">
              M
            </span>
            <span className="text-xl font-black tracking-tight">모여타</span>
          </Link>
          <nav className="flex items-center gap-2">
            <Link
              href="/"
              className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
            >
              홈으로
            </Link>
            <button
              type="button"
              onClick={handleLogout}
              disabled={loggingOut}
              className="rounded-lg px-3 py-2 text-sm font-medium text-slate-500 transition hover:bg-rose-50 hover:text-rose-700 disabled:opacity-50"
            >
              {loggingOut ? "로그아웃 중" : "로그아웃"}
            </button>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-5 py-10 sm:px-8 sm:py-14">
        <div className="mb-8">
          <p className="text-sm font-bold text-[#357465]">MY PAGE</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight sm:text-4xl">
            안녕하세요, {profile.nickname}님
          </h1>
        </div>

        {error && (
          <div className="mb-6 flex items-center justify-between rounded-2xl bg-rose-50 px-5 py-4 text-sm text-rose-700 ring-1 ring-rose-200">
            <span>{error}</span>
            <button type="button" onClick={() => setError("")}>
              닫기
            </button>
          </div>
        )}

        <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="space-y-5">
            <section className="overflow-hidden rounded-3xl bg-[#204d42] p-6 text-white shadow-sm">
              <div className="grid h-16 w-16 place-items-center rounded-2xl bg-white/15 text-2xl font-black ring-1 ring-white/20">
                {profile.nickname.slice(0, 1).toUpperCase()}
              </div>
              <h2 className="mt-5 text-xl font-bold">{profile.nickname}</h2>
              <p className="mt-1 truncate text-sm text-white/65">
                {profile.email}
              </p>
              <div className="mt-5 border-t border-white/15 pt-4">
                <p className="text-xs font-medium text-white/60">
                  {formatDate(profile.createdAt)} 가입
                </p>
                <p className="mt-1 text-sm font-semibold text-white/90">
                  {joinedDuration}
                </p>
              </div>
            </section>

            <section className="grid grid-cols-3 gap-2 rounded-3xl bg-white p-4 shadow-sm ring-1 ring-slate-200/80 lg:grid-cols-1">
              {(
                ["participations", "fundings", "payments"] as HistoryTab[]
              ).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setActiveTab(tab)}
                  className={`flex items-center justify-between rounded-2xl px-3 py-3 text-left transition ${
                    activeTab === tab
                      ? "bg-[#edf4f1] text-[#235347]"
                      : "text-slate-500 hover:bg-slate-50"
                  }`}
                >
                  <span className="text-xs font-semibold sm:text-sm">
                    {TAB_LABELS[tab]}
                  </span>
                  <span className="hidden rounded-lg bg-white px-2 py-1 text-xs font-bold shadow-sm lg:inline">
                    {histories[tab].totalElements}
                  </span>
                </button>
              ))}
            </section>
          </aside>

          <div className="min-w-0 space-y-6">
            <ProfileSection
              profile={profile}
              nickname={nickname}
              phoneNumber={phoneNumber}
              saving={saving}
              formError={formError}
              successMessage={successMessage}
              onNicknameChange={(value) => {
                setNickname(value);
                setFormError("");
                setSuccessMessage("");
              }}
              onPhoneNumberChange={(value) => {
                setPhoneNumber(value);
                setFormError("");
                setSuccessMessage("");
              }}
              onSubmit={handleUpdateProfile}
            />

            <HistorySection
              activeTab={activeTab}
              page={currentPage}
              loading={historyLoading}
              myParticipations={myParticipations}
              onTabChange={setActiveTab}
              onPageChange={moveHistoryPage}
              onParticipationCanceled={async () => {
                const data = await getMyParticipations();
                setMyParticipations(data);
              }}
            />

            <WithdrawSection
              onOpen={() => {
                setWithdrawModalOpen(true);
                setWithdrawError("");
              }}
            />
          </div>
        </div>
      </main>

      {withdrawModalOpen && (
        <WithdrawModal
          password={withdrawPassword}
          error={withdrawError}
          processing={withdrawing}
          onPasswordChange={(value) => {
            setWithdrawPassword(value);
            setWithdrawError("");
          }}
          onClose={closeWithdrawModal}
          onSubmit={handleWithdraw}
        />
      )}
    </div>
  );
}

function WithdrawSection({ onOpen }: { onOpen: () => void }) {
  return (
    <section className="rounded-3xl border border-rose-200 bg-rose-50/60 p-6 sm:p-8">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-center">
        <div>
          <p className="text-xs font-bold tracking-widest text-rose-600">
            ACCOUNT
          </p>
          <h2 className="mt-2 text-xl font-black text-slate-900">회원탈퇴</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            탈퇴하면 계정을 다시 사용할 수 없습니다. 참여 중인 펀딩과 결제
            내역을 먼저 확인해주세요.
          </p>
        </div>
        <button
          type="button"
          onClick={onOpen}
          className="shrink-0 rounded-xl border border-rose-300 bg-white px-5 py-3 text-sm font-bold text-rose-700 transition hover:bg-rose-600 hover:text-white"
        >
          회원탈퇴
        </button>
      </div>
    </section>
  );
}

interface WithdrawModalProps {
  password: string;
  error: string;
  processing: boolean;
  onPasswordChange: (value: string) => void;
  onClose: () => void;
  onSubmit: (event: React.FormEvent) => void;
}

function WithdrawModal({
  password,
  error,
  processing,
  onPasswordChange,
  onClose,
  onSubmit,
}: WithdrawModalProps) {
  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 px-5 py-8 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="withdraw-title"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl sm:p-8"
        onClick={(event) => event.stopPropagation()}
      >
        <p className="text-xs font-bold tracking-widest text-rose-600">
          WITHDRAW ACCOUNT
        </p>
        <h2 id="withdraw-title" className="mt-2 text-2xl font-black text-slate-950">
          정말 탈퇴하시겠어요?
        </h2>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          본인 확인을 위해 현재 비밀번호를 입력해주세요. 탈퇴 후에는 계정을
          복구할 수 없습니다.
        </p>

        <form onSubmit={onSubmit} className="mt-6">
          <label className="block">
            <span className="mb-2 block text-sm font-bold text-slate-700">
              비밀번호
            </span>
            <input
              type="password"
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              autoComplete="current-password"
              autoFocus
              placeholder="현재 비밀번호 입력"
              className="w-full rounded-xl border border-slate-200 px-4 py-3.5 text-sm outline-none transition focus:border-rose-400 focus:ring-4 focus:ring-rose-100"
            />
          </label>

          {error && (
            <p className="mt-3 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
              {error}
            </p>
          )}

          <div className="mt-6 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={processing}
              className="rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold text-slate-600 disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={!password || processing}
              className="rounded-xl bg-rose-600 px-4 py-3 text-sm font-bold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {processing ? "탈퇴 처리 중" : "회원탈퇴"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
interface ProfileSectionProps {
  profile: MemberProfile;
  nickname: string;
  phoneNumber: string;
  saving: boolean;
  formError: string;
  successMessage: string;
  onNicknameChange: (value: string) => void;
  onPhoneNumberChange: (value: string) => void;
  onSubmit: (event: React.FormEvent) => void;
}

function ProfileSection({
  profile,
  nickname,
  phoneNumber,
  saving,
  formError,
  successMessage,
  onNicknameChange,
  onPhoneNumberChange,
  onSubmit,
}: ProfileSectionProps) {
  return (
    <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/80 sm:p-8">
      <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-bold tracking-widest text-[#357465]">
            PROFILE
          </p>
          <h2 className="mt-2 text-xl font-black">내 정보</h2>
        </div>
        <p className="text-xs text-slate-400">
          이름과 이메일은 변경할 수 없습니다.
        </p>
      </div>

      <form onSubmit={onSubmit} className="mt-7 grid gap-5 sm:grid-cols-2">
        <ReadOnlyField label="이름" value={profile.name} />
        <ReadOnlyField label="이메일" value={profile.email} />

        <label className="block">
          <span className="mb-2 block text-sm font-bold text-slate-700">
            닉네임
          </span>
          <input
            value={nickname}
            onChange={(event) => onNicknameChange(event.target.value)}
            maxLength={20}
            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#357465] focus:ring-4 focus:ring-[#357465]/10"
          />
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-bold text-slate-700">
            전화번호
          </span>
          <input
            type="tel"
            value={phoneNumber}
            onChange={(event) => onPhoneNumberChange(event.target.value)}
            placeholder="010-0000-0000"
            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#357465] focus:ring-4 focus:ring-[#357465]/10"
          />
        </label>

        <div className="flex flex-col gap-3 sm:col-span-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-h-5 text-sm">
            {formError && <p className="text-rose-600">{formError}</p>}
            {successMessage && (
              <p className="text-emerald-700">{successMessage}</p>
            )}
          </div>
          <button
            type="submit"
            disabled={saving}
            className="rounded-xl bg-[#235347] px-6 py-3 text-sm font-bold text-white transition hover:bg-[#193e35] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? "저장 중" : "변경사항 저장"}
          </button>
        </div>
      </form>
    </section>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-slate-700">
        {label}
      </span>
      <input
        value={value}
        readOnly
        className="w-full cursor-not-allowed rounded-xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm text-slate-500 outline-none"
      />
    </label>
  );
}

interface HistorySectionProps {
  activeTab: HistoryTab;
  page: MemberHistoryPages[HistoryTab];
  loading: boolean;
  myParticipations: MyParticipation[];
  onTabChange: (tab: HistoryTab) => void;
  onPageChange: (page: number) => void;
  onParticipationCanceled: () => Promise<void>;
}

function HistorySection({
                          activeTab,
                          page,
                          loading,
                          myParticipations,
                          onTabChange,
                          onPageChange,
                          onParticipationCanceled,
                        }: HistorySectionProps) {
  const isEmpty =
      activeTab === "participations"
          ? myParticipations.length === 0
          : page.content.length === 0;

  return (
      <section className="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/80">
        <div className="border-b border-slate-100 px-6 pt-6 sm:px-8 sm:pt-8">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="text-xs font-bold tracking-widest text-[#357465]">
                ACTIVITY
              </p>
              <h2 className="mt-2 text-xl font-black">이용 내역</h2>
            </div>
            <p className="text-sm text-slate-400">
              총{" "}
              {(activeTab === "participations"
                      ? myParticipations.length
                      : page.totalElements
              ).toLocaleString("ko-KR")}
              건
            </p>
          </div>

          <div className="mt-6 flex gap-6 overflow-x-auto">
            {(Object.keys(TAB_LABELS) as HistoryTab[]).map((tab) => (
                <button
                    key={tab}
                    type="button"
                    onClick={() => onTabChange(tab)}
                    className={`whitespace-nowrap border-b-2 pb-4 text-sm font-bold transition ${
                        activeTab === tab
                            ? "border-[#235347] text-[#235347]"
                            : "border-transparent text-slate-400 hover:text-slate-700"
                    }`}
                >
                  {TAB_LABELS[tab]}
                </button>
            ))}
          </div>
        </div>

        <div className="relative min-h-64 px-6 py-6 sm:px-8">
          {loading && (
              <div className="absolute inset-0 z-10 grid place-items-center bg-white/75 backdrop-blur-[1px]">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#dfe5dc] border-t-[#235347]" />
              </div>
          )}

          {isEmpty ? (
              <EmptyHistory tab={activeTab} />
          ) : (
              <div className="space-y-3">
                {activeTab === "participations" &&
                    myParticipations.map((item) => (
                        <MyParticipationItem
                            key={item.participationId}
                            item={item}
                            onCanceled={onParticipationCanceled}
                        />
                    ))}

                {activeTab === "fundings" &&
                    (page.content as MemberFunding[]).map((item) => (
                        <FundingItem key={item.fundingId} item={item} />
                    ))}

                {activeTab === "payments" &&
                    (page.content as MemberPayment[]).map((item) => (
                        <PaymentItem key={item.paymentId} item={item} />
                    ))}
              </div>
          )}
        </div>

        {activeTab !== "participations" && page.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 border-t border-slate-100 px-6 py-5">
              <button
                  type="button"
                  disabled={page.first || loading}
                  onClick={() => onPageChange(page.page - 1)}
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 disabled:cursor-not-allowed disabled:opacity-30"
              >
                이전
              </button>
              <span className="min-w-20 text-center text-sm font-semibold text-slate-600">
            {page.page + 1} / {page.totalPages}
          </span>
              <button
                  type="button"
                  disabled={page.last || loading}
                  onClick={() => onPageChange(page.page + 1)}
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 disabled:cursor-not-allowed disabled:opacity-30"
              >
                다음
              </button>
            </div>
        )}
      </section>
  );
}

function MyParticipationItem({
                               item,
                               onCanceled,
                             }: {
  item: MyParticipation;
  onCanceled: () => Promise<void>;
}) {
  const router = useRouter();
  const [cancelModal, setCancelModal] = useState(false);
  const [canceling, setCanceling] = useState(false);
  const [cancelSuccess, setCancelSuccess] = useState(false);
  const [error, setError] = useState("");

  const departureTime = new Date(item.departureTime);
  const refundDeadline = new Date(departureTime);
  refundDeadline.setDate(refundDeadline.getDate() - 10);
  refundDeadline.setHours(0, 0, 0, 0);

  const cancelDeadline = new Date(departureTime);
  cancelDeadline.setDate(cancelDeadline.getDate() - 7);
  cancelDeadline.setHours(0, 0, 0, 0);

  const now = new Date();
  const canCancel = now < cancelDeadline;
  const canRefund = now < refundDeadline;
  const canShowCancel =
      item.status === "ACTIVE" &&
      item.paymentStatus === "ACTIVE" &&
      canCancel;

  const canShowBalancePayment =
      item.status === "ACTIVE" &&
      item.paymentStatus === "ACTIVE" &&
      item.balanceAmount > 0;

  function formatDeadlineDate(date: Date) {
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(date.getDate()).padStart(2, "0")}`;
  }

  function handleBalancePayment() {
    const seatInfo = item.returnSeatNumber
        ? `${item.outboundSeatNumber} / ${item.returnSeatNumber}`
        : item.outboundSeatNumber;
    sessionStorage.setItem(
        `balanceContext_${item.participationId}`,
        JSON.stringify({ fundingId: item.fundingId, seatInfo, amount: item.balanceAmount })
    );
    router.push(`/payment/balance/${item.participationId}`);
  }

  async function handleCancel() {
    setCanceling(true);
    setError("");
    try {
      await cancelParticipation(item.participationId);
      setCancelModal(false);
      setCancelSuccess(true);
      await onCanceled();
    } catch (err) {
      setError(err instanceof Error ? err.message : "참여 취소에 실패했습니다.");
    } finally {
      setCanceling(false);
    }
  }

  return (
      <>
        <article className="rounded-2xl border border-slate-100 p-4 transition hover:border-slate-200 hover:bg-slate-50/70 sm:p-5">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div className="min-w-0 flex-1">
              <h3 className="truncate font-bold text-slate-900">{item.fundingTitle}</h3>
              <p className="mt-1 text-sm text-slate-500">{item.routeInfo}</p>
              <p className="mt-1 text-xs text-slate-400">
                좌석:{" "}
                {item.returnSeatNumber
                    ? `가는편 ${item.outboundSeatNumber} / 오는편 ${item.returnSeatNumber}`
                    : item.outboundSeatNumber}
              </p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <StatusBadge status={item.status} />
                {item.paymentStatus !== "CANCELED" && (
                    <StatusBadge status={item.paymentStatus} />
                )}
                <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${
                    item.canBoard
                        ? "bg-emerald-50 text-emerald-700 ring-emerald-600/15"
                        : "bg-slate-50 text-slate-500 ring-slate-200"
                }`}>
                                {item.canBoard ? "탑승 가능" : "탑승 불가"}
                            </span>
              </div>
              {error && <p className="mt-2 text-xs text-rose-600">{error}</p>}
            </div>
            <div className="flex shrink-0 flex-col gap-2">
              {canShowBalancePayment && (
                  <button
                      type="button"
                      onClick={handleBalancePayment}
                      className="rounded-xl border border-blue-300 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-600 hover:bg-blue-100"
                  >
                    잔액결제
                  </button>
              )}
              {canShowCancel && (
                  <button
                      type="button"
                      onClick={() => setCancelModal(true)}
                      className="rounded-xl border border-red-300 bg-red-50 px-4 py-2 text-sm font-semibold text-red-600 hover:bg-red-100"
                  >
                    참여 취소
                  </button>
              )}
            </div>
          </div>
        </article>

        {cancelModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
              <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
                <h2 className="text-base font-bold text-gray-900 mb-3">참여를 취소하시겠습니까?</h2>
                <p className="text-sm text-gray-600 mb-1">
                  {canRefund ? (
                      "취소 시 보증금이 전액 환불됩니다."
                  ) : (
                      <>
                        <span className="font-semibold">{formatDeadlineDate(refundDeadline)}</span>{" "}
                        자정 이후 취소되어 보증금은 환불되지 않습니다.
                      </>
                  )}
                </p>
                <p className="text-sm text-gray-600 mb-1">
                  취소 후에는{" "}
                  <span className="font-semibold">{formatDeadlineDate(cancelDeadline)}</span>{" "}
                  자정까지 다시 참여할 수 있습니다.
                </p>
                {canRefund && (
                    <p className="text-xs text-gray-400 mt-2">
                      ※ {formatDeadlineDate(refundDeadline)} 자정 이후부터는 보증금 환불이 불가능합니다.
                    </p>
                )}
                <div className="flex gap-2 mt-5">
                  <button
                      type="button"
                      onClick={() => setCancelModal(false)}
                      className="flex-1 rounded border border-gray-300 py-2 text-sm font-semibold text-gray-700"
                  >
                    닫기
                  </button>
                  <button
                      type="button"
                      onClick={handleCancel}
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
                    className="rounded bg-gray-950 px-6 py-2 text-sm font-semibold text-white"
                >
                  확인
                </button>
              </div>
            </div>
        )}
      </>
  );
}

function EmptyHistory({ tab }: { tab: HistoryTab }) {
  return (
    <div className="grid min-h-52 place-items-center text-center">
      <div>
        <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-slate-100 text-xl text-slate-400">
          -
        </div>
        <p className="mt-4 text-sm font-bold text-slate-700">
          아직 {TAB_LABELS[tab]}이 없습니다.
        </p>
        <p className="mt-1 text-xs text-slate-400">
          모여타에서 새로운 이동을 시작해보세요.
        </p>
      </div>
    </div>
  );
}

function FundingItem({ item }: { item: MemberFunding }) {
  const progress = Math.min(
      100,
      Math.round((item.currentParticipants / item.maxParticipants) * 100),
  );

  return (
      <article
          className="rounded-2xl border border-slate-100 p-4 transition hover:border-slate-200 hover:bg-slate-50/70 sm:p-5">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={item.status}/>
              <span className="text-xs font-medium text-slate-400">
              출발일 {formatDate(item.departureDate)}
            </span>
            </div>
            <h3 className="mt-3 truncate font-bold text-slate-900">
              {item.fundingTitle}
            </h3>
            <div className="mt-3 flex items-center gap-3">
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                <div
                    className="h-full rounded-full bg-[#4f8d7e]"
                    style={{width: `${progress}%`}}
                />
              </div>
              <span className="text-xs font-bold text-slate-600">
              {item.currentParticipants}/{item.maxParticipants}명
            </span>
            </div>
          </div>
          <p className="shrink-0 text-xs text-slate-400">
            {formatDate(item.createdAt)} 개설
          </p>
        </div>
      </article>
  );
}

function PaymentItem({ item }: { item: MemberPayment }) {
  return (
      <article className="rounded-2xl border border-slate-100 p-4 transition hover:border-slate-200 hover:bg-slate-50/70 sm:p-5">
        <div className="flex items-center justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={item.status} />
              <span className="text-xs font-bold text-[#357465]">
              {statusLabel(item.type)}
            </span>
            </div>
            <h3 className="mt-3 truncate font-bold text-slate-900">
              {item.fundingTitle}
            </h3>
            <p className="mt-1 text-xs text-slate-400">
              {formatDate(item.createdAt, true)}
            </p>
          </div>
          <p className="shrink-0 text-lg font-black text-slate-900">
            {formatCurrency(item.amount)}
          </p>
        </div>
      </article>
  );
}

