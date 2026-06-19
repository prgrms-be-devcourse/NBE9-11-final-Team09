"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;
const PHONE_REGEX = /^010-\d{4}-\d{4}$/;

export default function SignupPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [agreed, setAgreed] = useState(false);

  const [emailSent, setEmailSent] = useState(false);
  const [requestLoading, setRequestLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [requestError, setRequestError] = useState("");
  const [requestSuccess, setRequestSuccess] = useState("");
  const [submitError, setSubmitError] = useState("");

  function resetEmailVerification() {
    setEmailSent(false);
    setVerificationCode("");
    setRequestSuccess("");
    setRequestError("");
  }

  const passwordValid = PASSWORD_REGEX.test(password);
  const passwordsMatch = password === passwordConfirm;
  const phoneValid = PHONE_REGEX.test(phoneNumber);

  const canRequest =
    !!email && !!password && passwordValid &&
    !!passwordConfirm && passwordsMatch &&
    !!name && !!nickname &&
    !!phoneNumber && phoneValid;

  async function handleRequestVerification() {
    setRequestError("");
    setRequestSuccess("");
    setRequestLoading(true);

    try {
      const res = await fetch("/api/members/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, name, nickname, phoneNumber }),
      });

      if (!res.ok) {
        let errorMessage = "인증 요청에 실패했습니다.";
        try {
          const errorData = await res.json();
          errorMessage = errorData.message ?? errorMessage;
        } catch {}
        setRequestError(errorMessage);
        return;
      }

      setEmailSent(true);
      setRequestSuccess("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    } catch {
      setRequestError("서버와 연결할 수 없습니다.");
    } finally {
      setRequestLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!emailSent) {
      setSubmitError("이메일 인증을 먼저 요청해주세요.");
      return;
    }
    if (!verificationCode) {
      setSubmitError("인증 코드를 입력해주세요.");
      return;
    }
    if (!agreed) {
      setSubmitError("개인정보 수집 및 이용에 동의해주세요.");
      return;
    }

    setSubmitError("");
    setSubmitLoading(true);

    try {
      const res = await fetch("/api/members/email-verification/confirm", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, verificationCode }),
      });

      if (!res.ok) {
        let errorMessage = "인증에 실패했습니다.";
        try {
          const errorData = await res.json();
          errorMessage = errorData.message ?? errorMessage;
        } catch {}
        setSubmitError(errorMessage);
        return;
      }

      router.push("/login");
    } catch {
      setSubmitError("서버와 연결할 수 없습니다.");
    } finally {
      setSubmitLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-white flex justify-center px-6 py-16">
      <div className="w-full max-w-lg">
        <h1 className="text-3xl font-bold mb-2">회원가입</h1>
        <p className="text-sm text-gray-500 mb-10">서비스 이용을 위한 기본 정보를 입력해주세요.</p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6">
          {/* 이메일 + 인증 요청 */}
          <div>
            <label className="block text-sm font-bold mb-2">이메일</label>
            <div className="flex gap-2">
              <input
                type="email"
                placeholder="example@email.com"
                value={email}
                onChange={(e) => { setEmail(e.target.value); if (emailSent) resetEmailVerification(); }}
                className="flex-1 border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
              <button
                type="button"
                onClick={handleRequestVerification}
                disabled={requestLoading || !canRequest}
                title={!canRequest ? "아래 항목을 모두 올바르게 입력해주세요." : ""}
                className="border border-gray-900 text-gray-900 font-bold px-5 py-3 rounded text-sm whitespace-nowrap disabled:opacity-30 disabled:cursor-not-allowed"
              >
                {requestLoading ? "요청 중..." : "인증 요청"}
              </button>
            </div>
            {requestError && <p className="text-red-500 text-xs mt-1">{requestError}</p>}
            {requestSuccess && <p className="text-green-600 text-xs mt-1">{requestSuccess}</p>}
          </div>

          {/* 이메일 인증 코드 */}
          <div>
            <label className="block text-sm font-bold mb-2">이메일 인증 코드</label>
            <input
              type="text"
              placeholder="인증 코드 입력"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value)}
              disabled={!emailSent}
              className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-400"
            />
          </div>

          {/* 이름 */}
          <div>
            <label className="block text-sm font-bold mb-2">이름</label>
            <input
              type="text"
              placeholder="이름 입력"
              value={name}
              onChange={(e) => { setName(e.target.value); if (emailSent) resetEmailVerification(); }}
              className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
            />
          </div>

          {/* 비밀번호 + 비밀번호 확인 */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-bold mb-2">비밀번호</label>
              <input
                type="password"
                placeholder="비밀번호 입력"
                value={password}
                onChange={(e) => { setPassword(e.target.value); if (emailSent) resetEmailVerification(); }}
                className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
              {password && !passwordValid && (
                <p className="text-red-500 text-xs mt-1">영문, 숫자, 특수문자 포함 8자 이상</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-bold mb-2">비밀번호 확인</label>
              <input
                type="password"
                placeholder="비밀번호 재입력"
                value={passwordConfirm}
                onChange={(e) => { setPasswordConfirm(e.target.value); if (emailSent) resetEmailVerification(); }}
                className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
              {passwordConfirm && !passwordsMatch && (
                <p className="text-red-500 text-xs mt-1">비밀번호가 일치하지 않습니다.</p>
              )}
            </div>
          </div>

          {/* 닉네임 + 연락처 */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-bold mb-2">닉네임</label>
              <input
                type="text"
                placeholder="닉네임 입력"
                value={nickname}
                onChange={(e) => { setNickname(e.target.value); if (emailSent) resetEmailVerification(); }}
                className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
            </div>
            <div>
              <label className="block text-sm font-bold mb-2">연락처</label>
              <input
                type="tel"
                placeholder="010-0000-0000"
                value={phoneNumber}
                onChange={(e) => { setPhoneNumber(e.target.value); if (emailSent) resetEmailVerification(); }}
                className="w-full border border-gray-300 rounded px-4 py-3 text-sm outline-none focus:border-gray-600"
              />
              {phoneNumber && !phoneValid && (
                <p className="text-red-500 text-xs mt-1">010-0000-0000 형식으로 입력해주세요.</p>
              )}
            </div>
          </div>

          {/* 개인정보 동의 */}
          <div className="border border-gray-300 rounded p-4">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(e) => setAgreed(e.target.checked)}
                className="mt-0.5 shrink-0"
              />
              <div className="text-sm text-gray-700 leading-relaxed">
                개인정보 수집 및 이용에 동의합니다.<br />
                수집 항목: 이메일, 닉네임, 연락처<br />
                이용 목적: 회원 식별, 결제 안내, 긴급 공지, 출발 안내
              </div>
            </label>
          </div>

          {submitError && <p className="text-red-500 text-xs">{submitError}</p>}

          <button
            type="submit"
            disabled={submitLoading}
            className="w-full bg-gray-900 text-white py-4 rounded text-sm font-bold disabled:opacity-50"
          >
            {submitLoading ? "처리 중..." : "회원가입"}
          </button>
        </form>

        <p className="text-sm text-gray-500 text-center mt-6">
          이미 계정이 있나요?{" "}
          <Link href="/login" className="text-gray-900 font-bold underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
