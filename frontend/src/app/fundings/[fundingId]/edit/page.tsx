"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import FundingForm from "@/components/FundingForm";
import { fromDetailToPayload } from "@/lib/fundingFormat";
import { getFunding, getSeatLayout, updateFunding } from "@/lib/fundingApi";
import type { FundingDetail, FundingPayload } from "@/types/funding";

async function createInitialPayload(detail: FundingDetail) {
  const payload = fromDetailToPayload(detail);

  if (detail.currentParticipants > 0) {
    return payload;
  }

  try {
    const layouts = await Promise.all(
      detail.pathinfos.map(async (pathinfo) => ({
        direction: pathinfo.direction,
        layout: await getSeatLayout(pathinfo.pathinfoId),
      }))
    );

    const outboundSeat = layouts
      .find((item) => item.direction === "OUTBOUND")
      ?.layout.seats.find((seat) => seat.status === "BOOKED");
    const returnSeat = layouts
      .find((item) => item.direction === "RETURN")
      ?.layout.seats.find((seat) => seat.status === "BOOKED");

    return {
      ...payload,
      hostOutboundSeatNumber:
        outboundSeat?.seatNumber ?? payload.hostOutboundSeatNumber,
      hostReturnSeatNumber:
        detail.tripType === "ROUND"
          ? returnSeat?.seatNumber ?? payload.hostReturnSeatNumber ?? ""
          : null,
    };
  } catch {
    return payload;
  }
}

export default function FundingEditPage() {
  const params = useParams<{ fundingId: string }>();
  const router = useRouter();
  const fundingId = Number(params.fundingId);
  const [funding, setFunding] = useState<FundingDetail | null>(null);
  const [initialPayload, setInitialPayload] = useState<FundingPayload | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [formDirty, setFormDirty] = useState(false);
  const [error, setError] = useState("");

  const isHost = Boolean(funding?.isHost);
  const textOnly = Boolean(funding && funding.currentParticipants > 0);

  function handleLeave(event: React.MouseEvent<HTMLAnchorElement>) {
    if (
      formDirty &&
      !submitting &&
      !window.confirm("변경사항이 저장되지 않을 수 있습니다. 페이지를 나가시겠습니까?")
    ) {
      event.preventDefault();
    }
  }

  useEffect(() => {
    let ignore = false;

    async function load() {
      setLoading(true);
      setError("");

      try {
        const data = await getFunding(fundingId);
        const payload = await createInitialPayload(data);

        if (!ignore) {
          setFunding(data);
          setInitialPayload(payload);
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err instanceof Error ? err.message : "펀딩 정보를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    if (Number.isFinite(fundingId)) {
      load();
    }

    return () => {
      ignore = true;
    };
  }, [fundingId]);

  async function handleSubmit(payload: FundingPayload) {
    setSubmitting(true);

    try {
      await updateFunding(fundingId, payload);
      setFormDirty(false);
      router.push(`/fundings/${fundingId}`);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10 text-center text-sm text-gray-500">
        수정 화면을 준비하는 중입니다.
      </main>
    );
  }

  if (error || !funding || !initialPayload) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10">
        <div className="mx-auto max-w-xl rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error || "펀딩 정보를 찾을 수 없습니다."}
        </div>
      </main>
    );
  }

  if (!isHost) {
    return (
      <main className="min-h-screen bg-gray-50 px-5 py-10">
        <div className="mx-auto grid max-w-xl gap-4 rounded border border-gray-200 bg-white p-6">
          <h1 className="text-2xl font-bold">수정 권한이 없습니다</h1>
          <p className="text-sm text-gray-600">
            펀딩을 만든 방장만 수정할 수 있습니다.
          </p>
          <Link
            href={`/fundings/${fundingId}`}
            className="w-fit rounded border border-gray-300 px-4 py-2 text-sm font-semibold"
          >
            상세로 돌아가기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-gray-50 text-gray-950">
      <div className="mx-auto grid w-full max-w-4xl gap-6 px-5 py-8">
        <Link
          href={`/fundings/${fundingId}`}
          onClick={handleLeave}
          className="w-fit text-sm font-medium text-gray-600"
        >
          상세로
        </Link>
        <section className="rounded border border-gray-200 bg-white p-6">
          <h1 className="text-3xl font-bold">펀딩 수정</h1>
          <p className="mt-2 text-sm text-gray-600">
            참여자가 없으면 전체 정보를 수정할 수 있고, 참여자가 있으면 제목과
            내용만 수정할 수 있습니다.
          </p>
          <div className="mt-8">
            <FundingForm
              mode="edit"
              textOnly={textOnly}
              initialValue={initialPayload}
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
