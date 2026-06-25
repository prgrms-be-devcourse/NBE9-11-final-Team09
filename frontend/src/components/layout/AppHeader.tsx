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
  if (typeof window === "undefined") {
    return null;
  }

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
        if (!ignore) {
          setMember(null);
        }
      }
    }

    async function syncAuthState() {
      const token = getAccessToken();

      await Promise.resolve();

      if (ignore) {
        return;
      }

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

  return (
    <header className="sticky top-0 z-40 border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-5">
        <Link href="/" className="text-lg font-black tracking-tight text-gray-950">
          모여타
        </Link>

        <nav className="flex items-center gap-2 text-sm sm:gap-3">
          {member ? (
            <>
              <span className="hidden rounded-full bg-gray-100 px-3 py-2 font-semibold text-gray-700 sm:inline-flex">
                {member.nickname}님
              </span>
              <Link
                href="/mypage"
                className="rounded bg-gray-950 px-3 py-2 font-semibold text-white"
              >
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
              >
                로그아웃
              </button>
            </>
          ) : hasToken ? (
            <>
              <Link
                href="/mypage"
                className="rounded bg-gray-950 px-3 py-2 font-semibold text-white"
              >
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded border border-gray-300 px-3 py-2 font-semibold text-gray-800 hover:bg-gray-50"
              >
                로그아웃
              </button>
            </>
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
