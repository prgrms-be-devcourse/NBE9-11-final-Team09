"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type ApiResponse<T> = {
  resultCode: string;
  msg: string;
  data: T;
};

type MemberInfo = {
  memberId: number;
  email: string;
  name: string;
  nickname: string;
};

function getAccessToken() {
  if (typeof window === "undefined") return null;

  return (
    localStorage.getItem("accessToken") ??
    sessionStorage.getItem("accessToken")
  );
}

function clearAccessToken() {
  localStorage.removeItem("accessToken");
  sessionStorage.removeItem("accessToken");
}

export default function AppHeader() {
  const router = useRouter();
  const pathname = usePathname();

  const [member, setMember] = useState<MemberInfo | null>(null);
  const [hasToken, setHasToken] = useState(false);

  useEffect(() => {
    let ignore = false;

    async function loadMe(accessToken: string) {
      try {
        const response = await fetch("/api/members/me", {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (!response.ok) {
          if (response.status === 401 && !ignore) {
            clearAccessToken();
            setHasToken(false);
            setMember(null);
          }
          throw new Error("failed");
        }

        const body = (await response.json()) as ApiResponse<MemberInfo>;

        if (!ignore) {
          setMember(body.data);
        }
      } catch {
        if (!ignore) setMember(null);
      }
    }

    async function syncAuthState() {
      const token = getAccessToken();

      await Promise.resolve();

      if (ignore) return;

      setHasToken(Boolean(token));

      if (token) {
        await loadMe(token);
      } else {
        setMember(null);
      }
    }

    syncAuthState();

    return () => {
      ignore = true;
    };
  }, [pathname]);

  async function handleLogout() {
    const token = getAccessToken();

    try {
      if (token) {
        await fetch("/api/members/logout", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      }
    } finally {
      clearAccessToken();
      setMember(null);
      setHasToken(false);
      router.push("/");
      router.refresh();
    }
  }

  const isLoggedIn = !!member;

  return (
    <header className="sticky top-0 z-40 border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-5">
        <Link href="/" className="text-lg font-black tracking-tight text-gray-950">
          모여타
        </Link>

        <nav className="flex items-center gap-2 text-sm sm:gap-3">
          {/* 마이페이지 */}
          {isLoggedIn && (
            <Link
              href="/mypage"
              className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
            >
              마이페이지
            </Link>
          )}

          {/* 🔔 알림 (완전 분리) */}
          {isLoggedIn && (
            <Link
              href="/notification"
              className="rounded border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
              aria-label="알림"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-5 w-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M14 21h-4m8-4V11a6 6 0 10-12 0v6l-2 2h16l-2-2z"
                />
              </svg>
            </Link>
          )}

          {/* 닉네임 (완전 분리) */}
          {member && (
            <span className="font-semibold text-gray-700">
              {member.nickname}
            </span>
          )}

          {/* 로그아웃 / 로그인 */}
          {member ? (
            <button
              type="button"
              onClick={handleLogout}
              className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
            >
              로그아웃
            </button>
          ) : hasToken ? (
            <button
              type="button"
              onClick={handleLogout}
              className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
            >
              로그아웃
            </button>
          ) : (
            <>
              <Link
                href="/login"
                className="rounded bg-gray-950 px-3 py-2 font-semibold text-white"
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
              >
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}