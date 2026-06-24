"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import PasswordField from "@/components/ui/PasswordField";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch("/api/members/login", {
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

      const storage = keepLogin ? localStorage : sessionStorage;
      storage.setItem("accessToken", accessToken);
      router.push("/");
    } catch {
      setError("서버와 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
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
            <span className="cursor-pointer text-sm text-gray-700 underline">
              비밀번호 찾기
            </span>
          </div>

          {error && <p className="text-xs text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={loading}
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
          className="w-full rounded border border-gray-900 py-4 text-sm font-bold"
        >
          Kakao 로그인
        </button>

        <p className="mt-6 text-center text-sm text-gray-500">
          아직 계정이 없나요?{" "}
          <Link href="/signup" className="font-bold text-gray-900 underline">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
