"use client";

import {
  AdminAuthenticationError,
  clearAdminSession,
  getAdminAccessToken,
  logoutAdmin,
} from "@/lib/admin-api";
import type { AdminSection } from "@/types/admin";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, useSyncExternalStore } from "react";
import FundingsPanel from "./_components/fundings-panel";
import MembersPanel from "./_components/members-panel";
import OverviewPanel from "./_components/overview-panel";
import SettlementsPanel from "./_components/settlements-panel";

const EMPTY_SUBSCRIBE = () => () => {};
const GET_CLIENT_SNAPSHOT = () => true;
const GET_SERVER_SNAPSHOT = () => false;

const NAV_ITEMS: { id: AdminSection; label: string; short: string }[] = [
  { id: "overview", label: "대시보드", short: "D" },
  { id: "members", label: "회원 관리", short: "M" },
  { id: "fundings", label: "펀딩 관리", short: "F" },
  { id: "settlements", label: "정산 관리", short: "S" },
];

export default function AdminDashboard() {
  const router = useRouter();
  const [section, setSection] = useState<AdminSection>("overview");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [notice, setNotice] = useState<{ message: string; error: boolean } | null>(null);
  const mounted = useSyncExternalStore(
    EMPTY_SUBSCRIBE,
    GET_CLIENT_SNAPSHOT,
    GET_SERVER_SNAPSHOT,
  );

  const moveToLogin = useCallback(() => {
    clearAdminSession();
    router.replace("/admin/login");
  }, [router]);

  useEffect(() => {
    if (!getAdminAccessToken()) {
      moveToLogin();
    }
  }, [moveToLogin]);

  const handleError = useCallback(
    (error: unknown) => {
      if (error instanceof AdminAuthenticationError) {
        moveToLogin();
        return;
      }
      setNotice({
        message: error instanceof Error ? error.message : "요청 처리 중 오류가 발생했습니다.",
        error: true,
      });
    },
    [moveToLogin],
  );

  function showSuccess(message: string) {
    setNotice({ message, error: false });
  }

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logoutAdmin();
    } catch {
      clearAdminSession();
    } finally {
      setLoggingOut(false);
      router.replace("/admin/login");
    }
  }

  if (!mounted || !getAdminAccessToken()) {
    return null;
  }

  return (
    <div className="min-h-screen bg-[#f4f7fa] text-slate-900">
      {sidebarOpen && (
        <button
          type="button"
          aria-label="메뉴 닫기"
          className="fixed inset-0 z-30 bg-slate-950/40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-[#13263b] text-white transition-transform lg:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <Link href="/admin" className="flex h-20 items-center gap-3 border-b border-white/10 px-6">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300 font-black text-[#13263b]">M</span>
          <div>
            <p className="font-black">모여타</p>
            <p className="text-[10px] font-bold tracking-[0.22em] text-slate-400">ADMIN</p>
          </div>
        </Link>
        <nav className="flex-1 space-y-2 px-4 py-7">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => {
                setSection(item.id);
                setSidebarOpen(false);
              }}
              className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition ${
                section === item.id
                  ? "bg-white text-[#13263b] shadow-lg shadow-black/10"
                  : "text-slate-300 hover:bg-white/10 hover:text-white"
              }`}
            >
              <span className={`grid h-7 w-7 place-items-center rounded-lg text-xs ${section === item.id ? "bg-[#dff5f7] text-[#245c70]" : "bg-white/10"}`}>
                {item.short}
              </span>
              {item.label}
            </button>
          ))}
        </nav>
        <div className="border-t border-white/10 p-4">
          <div className="rounded-xl bg-white/5 p-4">
            <p className="text-xs text-slate-400">로그인 관리자</p>
            <p className="mt-1 truncate text-sm font-bold">Administrator</p>
            <p className="mt-1 text-[11px] text-cyan-300">ADMIN</p>
          </div>
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-20 items-center justify-between border-b border-slate-200 bg-white/90 px-5 backdrop-blur sm:px-8">
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={() => setSidebarOpen(true)}
              className="grid h-10 w-10 place-items-center rounded-xl border border-slate-200 text-lg lg:hidden"
            >
              ≡
            </button>
            <div>
              <h1 className="text-lg font-black">{NAV_ITEMS.find((item) => item.id === section)?.label}</h1>
              <p className="hidden text-xs text-slate-400 sm:block">서비스 운영 현황을 확인하고 관리합니다.</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            disabled={loggingOut}
            className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold text-slate-600 hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700 disabled:opacity-50"
          >
            {loggingOut ? "처리 중" : "로그아웃"}
          </button>
        </header>

        <main className="p-5 sm:p-8">
          {notice && (
            <div className={`mb-6 flex items-center justify-between rounded-2xl px-5 py-4 text-sm font-medium ring-1 ${notice.error ? "bg-rose-50 text-rose-700 ring-rose-200" : "bg-emerald-50 text-emerald-700 ring-emerald-200"}`}>
              <span>{notice.message}</span>
              <button type="button" onClick={() => setNotice(null)}>닫기</button>
            </div>
          )}
          {section === "overview" && <OverviewPanel onError={handleError} />}
          {section === "members" && <MembersPanel onError={handleError} onSuccess={showSuccess} />}
          {section === "fundings" && <FundingsPanel onError={handleError} onSuccess={showSuccess} />}
          {section === "settlements" && <SettlementsPanel onError={handleError} />}
        </main>
      </div>
    </div>
  );
}
