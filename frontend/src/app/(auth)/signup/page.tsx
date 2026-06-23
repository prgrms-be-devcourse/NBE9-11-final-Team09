"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;
const PHONE_REGEX = /^010-\d{4}-\d{4}$/;

async function readErrorMessage(response: Response, fallback: string) {
  try {
    const errorData = await response.json();
    return errorData.message ?? errorData.msg ?? fallback;
  } catch {
    return fallback;
  }
}

function PasswordVisibilityIcon({ visible }: { visible: boolean }) {
  if (visible) {
    return (
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className="h-5 w-5"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    );
  }

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
    >
      <path d="M3 3l18 18" />
      <path d="M10.6 10.6A3 3 0 0 0 13.4 13.4" />
      <path d="M9.9 4.2A10.8 10.8 0 0 1 12 4c6.5 0 10 8 10 8a18.2 18.2 0 0 1-3.1 4.4" />
      <path d="M6.1 6.1C3.5 8 2 12 2 12s3.5 8 10 8a10.7 10.7 0 0 0 5.9-1.9" />
    </svg>
  );
}

type PasswordFieldProps = {
  label: string;
  placeholder: string;
  value: string;
  visible: boolean;
  disabled: boolean;
  onChange: (value: string) => void;
  onToggleVisible: () => void;
};

function PasswordField({
  label,
  placeholder,
  value,
  visible,
  disabled,
  onChange,
  onToggleVisible,
}: PasswordFieldProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-bold">{label}</label>
      <div className="relative">
        <input
          type={visible ? "text" : "password"}
          placeholder={placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={disabled}
          className="w-full rounded border border-gray-300 px-4 py-3 pr-12 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
        />
        <button
          type="button"
          onClick={onToggleVisible}
          disabled={disabled}
          className="absolute inset-y-0 right-0 flex w-12 items-center justify-center text-gray-500 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label={visible ? `${label} 숨기기` : `${label} 보기`}
        >
          <PasswordVisibilityIcon visible={visible} />
        </button>
      </div>
    </div>
  );
}

export default function SignupPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [agreed, setAgreed] = useState(false);

  const [signupRequested, setSignupRequested] = useState(false);
  const [emailSent, setEmailSent] = useState(false);
  const [signupLoading, setSignupLoading] = useState(false);
  const [requestLoading, setRequestLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [stepError, setStepError] = useState("");
  const [stepSuccess, setStepSuccess] = useState("");

  const emailValid = EMAIL_REGEX.test(email.trim());
  const passwordValid = PASSWORD_REGEX.test(password);
  const passwordsMatch = password === passwordConfirm;
  const phoneValid = PHONE_REGEX.test(phoneNumber);

  const canSaveSignup =
    emailValid &&
    passwordValid &&
    !!passwordConfirm &&
    passwordsMatch &&
    !!name.trim() &&
    !!nickname.trim() &&
    phoneValid &&
    agreed;

  function clearMessage() {
    setStepError("");
    setStepSuccess("");
  }

  function editSignupInformation() {
    setSignupRequested(false);
    setEmailSent(false);
    setVerificationCode("");
    clearMessage();
  }

  async function handleSignupRequest() {
    clearMessage();

    if (!canSaveSignup) {
      setStepError("회원정보를 올바르게 입력하고 개인정보 수집에 동의해주세요.");
      return;
    }

    setSignupLoading(true);
    try {
      const response = await fetch("/api/members/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
          password,
          name: name.trim(),
          nickname: nickname.trim(),
          phoneNumber: phoneNumber.trim(),
        }),
      });

      if (!response.ok) {
        setStepError(
          await readErrorMessage(
            response,
            "회원가입 정보를 저장하지 못했습니다.",
          ),
        );
        return;
      }

      setSignupRequested(true);
      setStepSuccess(
        "회원가입 정보가 저장되었습니다. 인증 메일을 요청해주세요.",
      );
    } catch {
      setStepError("서버와 연결할 수 없습니다.");
    } finally {
      setSignupLoading(false);
    }
  }

  async function handleRequestVerification() {
    clearMessage();

    if (!signupRequested) {
      setStepError("회원가입 정보를 먼저 저장해주세요.");
      return;
    }

    setRequestLoading(true);
    try {
      const response = await fetch(
        "/api/members/email-verification/request",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email.trim() }),
        },
      );

      if (!response.ok) {
        setStepError(
          await readErrorMessage(response, "인증 메일을 발송하지 못했습니다."),
        );
        return;
      }

      setEmailSent(true);
      setVerificationCode("");
      setStepSuccess("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    } catch {
      setStepError("서버와 연결할 수 없습니다.");
    } finally {
      setRequestLoading(false);
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    clearMessage();

    if (!emailSent) {
      setStepError("이메일 인증을 먼저 요청해주세요.");
      return;
    }

    if (!verificationCode.trim()) {
      setStepError("인증 코드를 입력해주세요.");
      return;
    }

    setSubmitLoading(true);
    try {
      const response = await fetch(
        "/api/members/email-verification/confirm",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: email.trim(),
            verificationCode: verificationCode.trim(),
          }),
        },
      );

      if (!response.ok) {
        setStepError(
          await readErrorMessage(response, "이메일 인증에 실패했습니다."),
        );
        return;
      }

      router.push("/login");
    } catch {
      setStepError("서버와 연결할 수 없습니다.");
    } finally {
      setSubmitLoading(false);
    }
  }

  const fieldsDisabled = signupRequested || signupLoading;

  return (
    <div className="flex min-h-screen justify-center bg-white px-6 py-16">
      <div className="w-full max-w-lg">
        <h1 className="mb-2 text-3xl font-bold">회원가입</h1>
        <p className="mb-10 text-sm text-gray-500">
          회원정보 저장 후 이메일 인증을 진행해주세요.
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6">
          <div>
            <div className="mb-2 flex items-center justify-between">
              <label className="block text-sm font-bold">이메일</label>
              {signupRequested && (
                <button
                  type="button"
                  onClick={editSignupInformation}
                  disabled={requestLoading || submitLoading}
                  className="text-xs font-bold text-gray-500 underline disabled:opacity-40"
                >
                  정보 수정
                </button>
              )}
            </div>
            <input
              type="email"
              placeholder="example@email.com"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                clearMessage();
              }}
              disabled={fieldsDisabled}
              className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
            />
            {email && !emailValid && (
              <p className="mt-1 text-xs text-red-500">
                올바른 이메일 형식으로 입력해주세요.
              </p>
            )}
          </div>

          <div>
            <label className="mb-2 block text-sm font-bold">이름</label>
            <input
              type="text"
              placeholder="이름 입력"
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                clearMessage();
              }}
              disabled={fieldsDisabled}
              className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <PasswordField
                label="비밀번호"
                placeholder="비밀번호 입력"
                value={password}
                visible={showPassword}
                disabled={fieldsDisabled}
                onChange={(value) => {
                  setPassword(value);
                  clearMessage();
                }}
                onToggleVisible={() => setShowPassword((current) => !current)}
              />
              {password && !passwordValid && (
                <p className="mt-1 text-xs text-red-500">
                  영문, 숫자, 특수문자 포함 8자 이상
                </p>
              )}
            </div>
            <div>
              <PasswordField
                label="비밀번호 확인"
                placeholder="비밀번호 재입력"
                value={passwordConfirm}
                visible={showPasswordConfirm}
                disabled={fieldsDisabled}
                onChange={(value) => {
                  setPasswordConfirm(value);
                  clearMessage();
                }}
                onToggleVisible={() =>
                  setShowPasswordConfirm((current) => !current)
                }
              />
              {passwordConfirm && !passwordsMatch && (
                <p className="mt-1 text-xs text-red-500">
                  비밀번호가 일치하지 않습니다.
                </p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-2 block text-sm font-bold">닉네임</label>
              <input
                type="text"
                placeholder="닉네임 입력"
                value={nickname}
                onChange={(event) => {
                  setNickname(event.target.value);
                  clearMessage();
                }}
                disabled={fieldsDisabled}
                className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-bold">연락처</label>
              <input
                type="tel"
                placeholder="010-0000-0000"
                value={phoneNumber}
                onChange={(event) => {
                  setPhoneNumber(event.target.value);
                  clearMessage();
                }}
                disabled={fieldsDisabled}
                className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-gray-600 disabled:bg-gray-50 disabled:text-gray-500"
              />
              {phoneNumber && !phoneValid && (
                <p className="mt-1 text-xs text-red-500">
                  010-0000-0000 형식으로 입력해주세요.
                </p>
              )}
            </div>
          </div>

          <div className="rounded border border-gray-300 p-4">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(event) => {
                  setAgreed(event.target.checked);
                  clearMessage();
                }}
                disabled={fieldsDisabled}
                className="mt-0.5 shrink-0"
              />
              <span className="text-sm leading-relaxed text-gray-700">
                개인정보 수집 및 이용에 동의합니다.
                <br />
                수집 항목: 이메일, 닉네임, 연락처
                <br />
                이용 목적: 회원 식별, 결제 안내, 긴급 공지, 출발 안내
              </span>
            </label>
          </div>

          {!signupRequested && (
            <button
              type="button"
              onClick={handleSignupRequest}
              disabled={!canSaveSignup || signupLoading}
              className="w-full rounded bg-gray-900 py-4 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-30"
            >
              {signupLoading ? "저장 중..." : "회원가입 정보 저장"}
            </button>
          )}

          {signupRequested && (
            <section className="rounded-2xl border border-gray-200 bg-gray-50 p-5">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                <div>
                  <p className="text-sm font-bold">이메일 인증</p>
                  <p className="mt-1 text-xs text-gray-500">{email}</p>
                </div>
                <button
                  type="button"
                  onClick={handleRequestVerification}
                  disabled={requestLoading || submitLoading}
                  className="rounded border border-gray-900 bg-white px-5 py-3 text-sm font-bold text-gray-900 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {requestLoading
                    ? "요청 중..."
                    : emailSent
                      ? "인증 코드 재전송"
                      : "인증 메일 요청"}
                </button>
              </div>

              <label className="mt-5 block">
                <span className="mb-2 block text-sm font-bold">
                  이메일 인증 코드
                </span>
                <input
                  type="text"
                  placeholder="인증 코드 입력"
                  value={verificationCode}
                  onChange={(event) => {
                    setVerificationCode(event.target.value);
                    clearMessage();
                  }}
                  disabled={!emailSent || submitLoading}
                  maxLength={6}
                  className="w-full rounded border border-gray-300 bg-white px-4 py-3 text-sm uppercase outline-none focus:border-gray-600 disabled:bg-gray-100 disabled:text-gray-400"
                />
              </label>
            </section>
          )}

          {stepError && <p className="text-sm text-red-500">{stepError}</p>}
          {stepSuccess && (
            <p className="text-sm text-green-600">{stepSuccess}</p>
          )}

          {emailSent && (
            <button
              type="submit"
              disabled={!verificationCode.trim() || submitLoading}
              className="w-full rounded bg-gray-900 py-4 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              {submitLoading ? "인증 중..." : "인증 완료 및 회원가입"}
            </button>
          )}
        </form>

        <p className="mt-6 text-center text-sm text-gray-500">
          이미 계정이 있나요?{" "}
          <Link href="/login" className="font-bold text-gray-900 underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
