import type { AdminPageResponse } from "@/types/admin";
import type { ReactNode } from "react";

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "활성",
  SUSPENDED: "정지",
  WITHDRAWN: "탈퇴",
  RECRUITING: "모집 중",
  CONFIRMED: "확정",
  COMPLETED: "완료",
  FAILED: "실패",
  CANCELLED: "취소",
  READY: "정산 대기",
  APPROVED: "승인",
  PENDING: "대기",
};

export function formatAdminDate(value: string | null, time = false) {
  if (!value) return "-";
  const normalized = value.includes("T") ? value : `${value}T00:00:00`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: "Asia/Seoul",
    ...(time ? { hour: "2-digit", minute: "2-digit", hour12: false } : {}),
  }).format(date);
}

export function formatAdminCurrency(value: number | string) {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? `${new Intl.NumberFormat("ko-KR").format(amount)}원`
    : "-";
}

export function AdminStatusBadge({ status }: { status: string }) {
  const tone = ["ACTIVE", "RECRUITING", "COMPLETED", "APPROVED"].includes(status)
    ? "bg-emerald-50 text-emerald-700 ring-emerald-600/20"
    : ["SUSPENDED", "FAILED", "CANCELLED", "WITHDRAWN"].includes(status)
      ? "bg-rose-50 text-rose-700 ring-rose-600/20"
      : "bg-amber-50 text-amber-700 ring-amber-600/20";

  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${tone}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}

export function AdminPagination<T>({
  page,
  loading,
  onChange,
}: {
  page: AdminPageResponse<T>;
  loading: boolean;
  onChange: (page: number) => void;
}) {
  if (page.totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-100 px-5 py-4 sm:px-6">
      <p className="text-xs text-slate-400">총 {page.totalElements.toLocaleString("ko-KR")}건</p>
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={page.first || loading}
          onClick={() => onChange(page.page - 1)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 disabled:opacity-30"
        >
          이전
        </button>
        <span className="min-w-16 text-center text-xs font-bold text-slate-500">
          {page.page + 1} / {page.totalPages}
        </span>
        <button
          type="button"
          disabled={page.last || loading}
          onClick={() => onChange(page.page + 1)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 disabled:opacity-30"
        >
          다음
        </button>
      </div>
    </div>
  );
}

export function AdminModal({
  title,
  description,
  children,
  onClose,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 px-4 py-8 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <div
        className="max-h-full w-full max-w-lg overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl sm:p-8"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-black text-slate-950">{title}</h2>
            {description && <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-500 hover:bg-slate-200"
          >
            ×
          </button>
        </div>
        <div className="mt-6">{children}</div>
      </div>
    </div>
  );
}

export function AdminLoading() {
  return (
    <div className="grid min-h-64 place-items-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-[#4f7a61]" />
    </div>
  );
}

export function AdminEmpty({ message }: { message: string }) {
  return (
    <div className="grid min-h-56 place-items-center text-center">
      <div>
        <div className="mx-auto h-12 w-12 rounded-2xl bg-slate-100" />
        <p className="mt-4 text-sm font-bold text-slate-600">{message}</p>
      </div>
    </div>
  );
}

export function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-5 border-b border-slate-100 py-3 last:border-0">
      <dt className="text-sm text-slate-400">{label}</dt>
      <dd className="text-right text-sm font-semibold text-slate-700">{value}</dd>
    </div>
  );
}
