"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Bell, BusFront, ChevronDown, LogOut, UserRound } from "lucide-react";
import { createApiUrl } from "@/lib/api-url";

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
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let ignore = false;

    async function loadMe(accessToken: string) {
      try {
        const response = await fetch(createApiUrl("/api/members/me"), {
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
          clearAccessToken();
          setHasToken(false);
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

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
    };
  }, []);

  async function handleLogout() {
    const token = getAccessToken();

    try {
      if (token) {
        await fetch(createApiUrl("/api/members/logout"), {
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
      setMenuOpen(false);
      router.push("/");
      router.refresh();
    }
  }

  if (pathname.startsWith("/admin")) return null;

  return (
    <header className="sticky top-0 z-40 border-b border-[#dbe7dc] bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-5">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-xl font-black tracking-tight text-slate-950"
        >
          <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-[#4f7a61] text-white shadow-sm">
            <BusFront size={22} strokeWidth={2.5} />
          </span>
          <span>모여타</span>
        </Link>

        <nav className="flex items-center gap-3 text-sm">
          {member ? (
            <>
              <Link
                href="/notification"
                className="inline-flex h-10 w-10 cursor-pointer items-center justify-center rounded-full text-slate-700 transition hover:bg-[#eef5ea] hover:text-[#426f55]"
                aria-label="알림"
              >
                <Bell size={21} strokeWidth={2.2} />
              </Link>
              <div ref={menuRef} className="relative">
                <button
                  type="button"
                  onClick={() => setMenuOpen((open) => !open)}
                  className="inline-flex cursor-pointer items-center gap-2 rounded-full py-1 pl-1 pr-2 font-semibold text-slate-900 transition hover:bg-[#eef5ea]"
                  aria-expanded={menuOpen}
                  aria-haspopup="menu"
                >
                  <span className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-slate-100 text-slate-400 ring-1 ring-slate-200">
                    <UserRound size={22} strokeWidth={2.2} />
                  </span>
                  <span className="hidden max-w-32 truncate sm:inline">
                    {member.nickname}
                  </span>
                  <ChevronDown
                    size={16}
                    strokeWidth={2.4}
                    className={`text-slate-600 transition ${menuOpen ? "rotate-180" : ""}`}
                  />
                </button>

                {menuOpen && (
                  <div
                    role="menu"
                    className="absolute right-0 mt-2 w-44 overflow-hidden rounded-xl border border-[#dbe7dc] bg-white py-1.5 shadow-[0_12px_30px_rgba(31,41,55,0.12)]"
                  >
                    <Link
                      href="/mypage"
                      role="menuitem"
                      onClick={() => setMenuOpen(false)}
                      className="flex cursor-pointer items-center gap-2 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-[#f5f8f4]"
                    >
                      <UserRound size={16} strokeWidth={2.2} />
                      마이페이지
                    </Link>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={handleLogout}
                      className="flex w-full cursor-pointer items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50"
                    >
                      <LogOut size={16} strokeWidth={2.2} />
                      로그아웃
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : hasToken ? (
            <span className="inline-flex h-9 items-center rounded-full bg-[#eef5ea] px-4 text-sm font-semibold text-[#426f55]">
              로그인 확인 중
            </span>
          ) : (
            <Link
              href="/login"
              className="inline-flex h-10 cursor-pointer items-center rounded-full bg-[#4f7a61] px-5 font-semibold text-white shadow-sm transition hover:bg-[#426f55]"
            >
              로그인
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
