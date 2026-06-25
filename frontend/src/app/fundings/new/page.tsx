"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import FundingForm from "@/components/FundingForm";
import { createFunding } from "@/lib/fundingApi";
import { useFundingLoggedIn } from "@/lib/fundingAuth";
import type { FundingPayload } from "@/types/funding";

export default function FundingCreatePage() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [formDirty, setFormDirty] = useState(false);
  const loggedIn = useFundingLoggedIn();

  function handleLeave(event: React.MouseEvent<HTMLAnchorElement>) {
    if (
      formDirty &&
      !submitting &&
      !window.confirm("변경사항이 저장되지 않을 수 있습니다. 페이지를 나가시겠습니까?")
    ) {
      event.preventDefault();
    }
  }

  async function handleSubmit(payload: FundingPayload) {
    setSubmitting(true);

    try {
      const response = await createFunding(payload);
      setFormDirty(false);
      router.push(`/fundings/${response.fundingId}`);
    } finally {
      setSubmitting(false);
    }
  }

  if (!loggedIn) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10">
        <div className="mx-auto grid max-w-xl gap-4 rounded border border-gray-200 bg-white p-6">
          <h1 className="text-2xl font-bold">로그인이 필요합니다</h1>
          <p className="text-sm text-gray-600">
            펀딩 생성은 로그인한 사용자만 사용할 수 있습니다.
          </p>
          <Link
            href="/login"
            className="w-fit rounded bg-gray-950 px-4 py-2 text-sm font-semibold text-white"
          >
            로그인하기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-gray-50 text-gray-950">
      <div className="mx-auto grid w-full max-w-4xl gap-6 px-5 py-8">
        <Link
          href="/fundings"
          onClick={handleLeave}
          className="w-fit text-sm font-medium text-gray-600"
        >
          목록으로
        </Link>
        <section className="rounded border border-gray-200 bg-white p-6">
          <h1 className="text-3xl font-bold">펀딩 만들기</h1>
          <p className="mt-2 text-sm text-gray-600">
            노선과 방장 좌석을 함께 설정해주세요.
          </p>
          <div className="mt-8">
            <FundingForm
              mode="create"
              submitting={submitting}
              onDirtyChange={setFormDirty}
              onSubmit={handleSubmit}
            />
          </div>
        </section>
      </div>
    </main>
  );
}
