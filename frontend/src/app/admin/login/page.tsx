"use client";

import {
  AdminApiError,
  getAdminAccessToken,
  loginAdmin,
  storeAdminSession,
} from "@/lib/admin-api";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function AdminLoginPage() {
  const router = useRouter();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (getAdminAccessToken()) router.replace("/admin");
  }, [router]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await loginAdmin(loginId.trim(), password);
      storeAdminSession(response, keepLogin);
      router.replace("/admin");
    } catch (requestError) {
      setError(
        requestError instanceof AdminApiError
          ? requestError.message
          : "관리자 로그인에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="relative grid min-h-screen overflow-hidden bg-[#101b29] lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative hidden overflow-hidden p-16 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -left-20 -top-20 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="absolute bottom-10 right-0 h-96 w-96 rounded-full bg-blue-500/10 blur-3xl" />
        <Link href="/" className="relative flex items-center gap-3 text-xl font-black">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-400 text-[#101b29]">
            M
          </span>
          모여타 Admin
        </Link>
        <div className="relative max-w-xl">
          <p className="text-sm font-bold tracking-[0.28em] text-cyan-300">
            OPERATIONS CENTER
          </p>
          <h1 className="mt-6 text-5xl font-black leading-tight tracking-tight">
            이동의 모든 순간을
            <br />안전하게 관리합니다.
          </h1>
          <p className="mt-6 max-w-lg text-base leading-7 text-slate-300">
            회원과 펀딩, 정산 현황을 한눈에 확인하고 서비스 운영에 필요한
            조치를 빠르게 수행하세요.
          </p>
        </div>
        <p className="relative text-xs text-slate-500">
          Authorized administrators only
        </p>
      </section>

      <section className="flex items-center justify-center bg-[#f6f8fb] px-6 py-12 sm:px-12">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden">
            <Link href="/" className="flex items-center gap-3 text-xl font-black">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-[#173452] text-white">
                M
              </span>
              모여타 Admin
            </Link>
          </div>
          <p className="text-sm font-bold text-[#2b6b88]">ADMIN SIGN IN</p>
          <h2 className="mt-2 text-3xl font-black tracking-tight text-slate-950">
            관리자 로그인
          </h2>
          <p className="mt-3 text-sm leading-6 text-slate-500">
            발급받은 관리자 계정으로 로그인해주세요.
          </p>

          <form onSubmit={handleSubmit} className="mt-9 space-y-5">
            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-700">
                관리자 아이디
              </span>
              <input
                value={loginId}
                onChange={(event) => setLoginId(event.target.value)}
                autoComplete="username"
                required
                placeholder="관리자 아이디 입력"
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3.5 text-sm outline-none transition focus:border-[#2b6b88] focus:ring-4 focus:ring-[#2b6b88]/10"
              />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-700">
                비밀번호
              </span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
                placeholder="비밀번호 입력"
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3.5 text-sm outline-none transition focus:border-[#2b6b88] focus:ring-4 focus:ring-[#2b6b88]/10"
              />
            </label>
            <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-600">
              <input
                type="checkbox"
                checked={keepLogin}
                onChange={(event) => setKeepLogin(event.target.checked)}
                className="h-4 w-4 accent-[#173452]"
              />
              관리자 로그인 유지
            </label>

            {error && (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-xl bg-[#173452] px-5 py-4 text-sm font-bold text-white transition hover:bg-[#10283f] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? "로그인 중" : "관리자 로그인"}
            </button>
          </form>

          <div className="mt-8 border-t border-slate-200 pt-6 text-center">
            <Link href="/login" className="text-sm font-semibold text-slate-500 hover:text-slate-900">
              일반 회원 로그인으로 돌아가기
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
