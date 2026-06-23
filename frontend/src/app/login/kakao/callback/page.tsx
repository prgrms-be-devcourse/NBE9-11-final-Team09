"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import {
  ApiRequestError,
  kakaoAuthorizationCodeLogin,
  storeAccessToken,
} from "@/lib/member-api";

const KAKAO_KEEP_LOGIN_KEY = "kakaoKeepLogin";

function getRedirectUri() {
  if (typeof window === "undefined") {
    return "";
  }

  return `${window.location.origin}/login/kakao/callback`;
}

function getKeepLogin() {
  if (typeof window === "undefined") {
    return false;
  }

  return window.sessionStorage.getItem(KAKAO_KEEP_LOGIN_KEY) === "true";
}

function clearKakaoLoginState() {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(KAKAO_KEEP_LOGIN_KEY);
}

function KakaoCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState("");

  useEffect(() => {
    const code = searchParams.get("code");
    const kakaoError = searchParams.get("error");
    const kakaoErrorDescription = searchParams.get("error_description");

    if (kakaoError) {
      setError(
        kakaoErrorDescription ?? "카카오 로그인이 취소되었거나 실패했습니다.",
      );
      clearKakaoLoginState();
      return;
    }

    if (!code) {
      setError("카카오 인가 코드가 없습니다.");
      clearKakaoLoginState();
      return;
    }

    const authorizationCode = code;
    let ignore = false;

    async function loginWithCode() {
      try {
        const response = await kakaoAuthorizationCodeLogin({
          code: authorizationCode,
          redirectUri: getRedirectUri(),
        });

        if (ignore) {
          return;
        }

        storeAccessToken(response.accessToken, getKeepLogin());
        clearKakaoLoginState();
        router.replace("/");
        router.refresh();
      } catch (loginError) {
        if (ignore) {
          return;
        }

        clearKakaoLoginState();

        if (loginError instanceof ApiRequestError) {
          setError(loginError.message);
          return;
        }

        setError("카카오 로그인 처리 중 오류가 발생했습니다.");
      }
    }

    void loginWithCode();

    return () => {
      ignore = true;
    };
  }, [router, searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-white px-6 py-16">
      <div className="w-full max-w-md rounded-2xl border border-gray-200 p-8 text-center shadow-sm">
        {error ? (
          <>
            <h1 className="text-2xl font-bold text-gray-950">
              카카오 로그인 실패
            </h1>
            <p className="mt-4 text-sm text-red-500">{error}</p>
            <Link
              href="/login"
              className="mt-8 inline-flex rounded bg-gray-950 px-5 py-3 text-sm font-bold text-white"
            >
              로그인으로 돌아가기
            </Link>
          </>
        ) : (
          <>
            <h1 className="text-2xl font-bold text-gray-950">
              카카오 로그인 처리 중
            </h1>
            <p className="mt-4 text-sm text-gray-500">
              잠시만 기다려주세요.
            </p>
          </>
        )}
      </div>
    </div>
  );
}

export default function KakaoCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-white text-sm text-gray-500">
          카카오 로그인 처리 중...
        </div>
      }
    >
      <KakaoCallbackContent />
    </Suspense>
  );
}

