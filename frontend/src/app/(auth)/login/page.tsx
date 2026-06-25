"use client";

import Link from "next/link";
import Script from "next/script";
import { useRouter } from "next/navigation";
import { useState } from "react";
import PasswordField from "@/components/ui/PasswordField";
import { storeAccessToken } from "@/lib/member-api";
import { createApiUrl } from "@/lib/api-url";

type KakaoSdk = {
  isInitialized: () => boolean;
  init: (appKey: string) => void;
  Auth: {
    authorize: (options: {
      redirectUri: string;
      scope?: string;
      state?: string;
    }) => void;
  };
};

declare global {
  interface Window {
    Kakao?: KakaoSdk;
  }
}

const KAKAO_SDK_URL = "https://t1.kakaocdn.net/kakao_js_sdk/2.7.4/kakao.min.js";
const KAKAO_SCOPE = "profile_nickname,account_email";
const KAKAO_KEEP_LOGIN_KEY = "kakaoKeepLogin";

function getKakaoJavascriptKey() {
  return process.env.NEXT_PUBLIC_KAKAO_JAVASCRIPT_KEY;
}

function getKakaoRedirectUri() {
  if (typeof window === "undefined") {
    return "";
  }

  return `${window.location.origin}/login/kakao/callback`;
}

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [kakaoLoading, setKakaoLoading] = useState(false);
  const [kakaoReady, setKakaoReady] = useState(false);

  function initializeKakaoSdk() {
    const javascriptKey = getKakaoJavascriptKey();
    const kakao = window.Kakao;

    if (!javascriptKey || !kakao) {
      return false;
    }

    if (!kakao.isInitialized()) {
      kakao.init(javascriptKey);
    }

    return true;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch(createApiUrl("/api/members/login"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      if (!res.ok) {
        let errorMessage = "이메일 또는 비밀번호를 확인해주세요.";
        try {
          const errorData = await res.json();
          errorMessage = errorData.message ?? errorData.msg ?? errorMessage;
        } catch {}
        setError(errorMessage);
        return;
      }

      const data = await res.json();
      const accessToken = data?.data?.accessToken;
      if (!accessToken) {
        setError("로그인 응답이 올바르지 않습니다.");
        return;
      }

      storeAccessToken(accessToken, keepLogin);
      router.push("/");
    } catch {
      setError("서버와 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }

  function handleKakaoLogin() {
    const javascriptKey = getKakaoJavascriptKey();

    setError("");

    if (!javascriptKey) {
      setError("카카오 JavaScript 키가 설정되지 않았습니다.");
      return;
    }

    setKakaoLoading(true);

    if (!initializeKakaoSdk()) {
      setError("카카오 로그인 준비가 완료되지 않았습니다.");
      setKakaoLoading(false);
      return;
    }

    sessionStorage.setItem(KAKAO_KEEP_LOGIN_KEY, String(keepLogin));

    window.Kakao?.Auth.authorize({
      redirectUri: getKakaoRedirectUri(),
      scope: KAKAO_SCOPE,
    });
  }

  return (
    <>
      <Script
        src={KAKAO_SDK_URL}
        strategy="afterInteractive"
        onLoad={() => {
          if (initializeKakaoSdk()) {
            setKakaoReady(true);
          }
        }}
        onError={() => {
          setKakaoReady(false);
          setError("카카오 SDK를 불러오지 못했습니다.");
        }}
      />

      <div className="flex min-h-screen justify-center bg-white px-6 py-16">
        <div className="w-full max-w-lg">
          <h1 className="mb-2 text-3xl font-bold">로그인</h1>
          <p className="mb-10 text-sm text-gray-500">
            가입한 이메일과 비밀번호를 입력해주세요.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-6">
            <div>
              <label className="mb-2 block text-sm font-bold">이메일</label>
              <input
                type="email"
                placeholder="example@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
            </div>

            <PasswordField
              label="비밀번호"
              placeholder="비밀번호 입력"
              value={password}
              visible={showPassword}
              required
              onChange={setPassword}
              onToggleVisible={() => setShowPassword((current) => !current)}
            />

            <div className="flex items-center justify-between">
              <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={keepLogin}
                  onChange={(e) => setKeepLogin(e.target.checked)}
                />
                로그인 상태 유지
              </label>
            </div>

            {error && <p className="text-xs text-red-500">{error}</p>}

            <button
              type="submit"
              disabled={loading || kakaoLoading}
              className="w-full rounded bg-gray-900 py-4 text-sm font-bold text-white disabled:opacity-50"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <div className="my-6 flex items-center gap-4">
            <div className="flex-1 border-t border-gray-200" />
            <span className="text-sm text-gray-400">또는</span>
            <div className="flex-1 border-t border-gray-200" />
          </div>

          <button
            type="button"
            onClick={handleKakaoLogin}
            disabled={loading || kakaoLoading || !kakaoReady}
            className="flex h-[52px] w-full items-center justify-center gap-3 rounded-[12px] bg-[#FEE500] text-[15px] font-semibold text-black/85 transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-60"
            aria-label="카카오 로그인"
          >
            <svg
              aria-hidden="true"
              viewBox="0 0 24 22"
              className="h-[18px] w-[20px] shrink-0 fill-black"
            >
              <path d="M12 0C5.373 0 0 4.182 0 9.342c0 3.352 2.267 6.29 5.67 7.936l-.884 3.252c-.078.288.25.519.5.353l3.871-2.562c.915.236 1.87.363 2.843.363 6.627 0 12-4.182 12-9.342S18.627 0 12 0Z" />
            </svg>
            <span>{kakaoLoading ? "카카오 로그인 중..." : "카카오 로그인"}</span>
          </button>

          {!getKakaoJavascriptKey() && (
            <p className="mt-3 text-center text-xs text-red-500">
              NEXT_PUBLIC_KAKAO_JAVASCRIPT_KEY 설정이 필요합니다.
            </p>
          )}

          <p className="mt-6 text-center text-sm text-gray-500">
            아직 계정이 없나요?{" "}
            <Link href="/signup" className="font-bold text-gray-900 underline">
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </>
  );
}