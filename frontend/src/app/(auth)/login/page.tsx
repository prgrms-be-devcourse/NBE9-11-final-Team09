"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
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
        body: JSON.stringify({ email, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.message ?? "이메일 또는 비밀번호를 확인해주세요.");
        return;
      }

      const storage = keepLogin ? localStorage : sessionStorage;
      storage.setItem("accessToken", data.data.accessToken);
      router.push("/");
    } catch {
      setError("서버와 연결할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-white flex justify-center px-6 py-16">
      <div className="w-full max-w-lg">
        <h1 className="text-3xl font-bold mb-2">로그인</h1>
        <p className="text-sm text-gray-500 mb-10">가입한 이메일과 비밀번호를 입력해주세요.</p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6">
          {/* 이메일 */}
          <div>
            <label className="block text-sm font-bold mb-2">이메일</label>
            <input
              type="email"
              placeholder="example@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-sm font-bold mb-2">비밀번호</label>
            <input
              type="password"
              placeholder="비밀번호 입력"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
            />
          </div>

          {/* 로그인 상태 유지 + 비밀번호 찾기 */}
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 cursor-pointer text-sm text-gray-700">
              <input
                type="checkbox"
                checked={keepLogin}
                onChange={(e) => setKeepLogin(e.target.checked)}
              />
              로그인 상태 유지
            </label>
            <span className="text-sm text-gray-700 underline cursor-pointer">비밀번호 찾기</span>
          </div>

          {error && <p className="text-red-500 text-xs">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-gray-900 text-white py-4 rounded text-sm font-bold disabled:opacity-50"
          >
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </form>

        {/* 구분선 */}
        <div className="flex items-center gap-4 my-6">
          <div className="flex-1 border-t border-gray-200" />
          <span className="text-sm text-gray-400">또는</span>
          <div className="flex-1 border-t border-gray-200" />
        </div>

        {/* Kakao 로그인 */}
        <button className="w-full border border-gray-900 py-4 rounded text-sm font-bold">
          Kakao 로그인
        </button>

        <p className="text-sm text-gray-500 text-center mt-6">
          아직 계정이 없나요?{" "}
          <Link href="/signup" className="text-gray-900 font-bold underline">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
