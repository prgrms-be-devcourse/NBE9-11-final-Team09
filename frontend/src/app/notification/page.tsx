"use client";

import { useEffect, useState } from "react";
import { getNotifications } from "@/lib/notificationApi";
import { Notification } from "@/types/notification";
import DOMPurify from "isomorphic-dompurify";
import { Bell, ChevronDown, MailOpen } from "lucide-react";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [openId, setOpenId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchNotifications() {
      setLoading(true);
      try {
        const pageResponse = await getNotifications(currentPage, 20);
        setNotifications(pageResponse.content ?? []);
        setTotalPages(pageResponse.totalPages);
      } catch (error) {
        console.error("알림 조회 실패:", error);
      } finally {
        setLoading(false);
      }
    }
    fetchNotifications();
  }, [currentPage]);

  return (
    <main className="min-h-screen bg-[#f3f7f1] px-5 py-8 text-slate-900 [font-family:Pretendard,'Noto_Sans_KR','Segoe_UI',system-ui,sans-serif]">
      <div className="mx-auto grid w-full max-w-4xl gap-5">
        <section className="rounded-3xl bg-[#426f55] p-7 text-white shadow-sm">
          <div className="flex items-start gap-4">
            <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/20">
              <Bell size={24} strokeWidth={2.2} />
            </span>
            <div>
              <p className="text-xs font-bold tracking-widest text-white/60">
                NOTIFICATIONS
              </p>
              <h1 className="mt-2 text-3xl font-black tracking-tight">
                알림 내역
              </h1>
              <p className="mt-2 text-sm font-medium leading-6 text-white/70">
                펀딩 상태 변경과 결제 안내 메일 발송 내역을 확인할 수 있습니다.
              </p>
            </div>
          </div>
        </section>

        <section className="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/80">
          <div className="border-b border-slate-100 px-6 py-5 sm:px-8">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-bold tracking-widest text-[#357465]">
                  EMAIL HISTORY
                </p>
                <h2 className="mt-2 text-xl font-black">이메일 발송 내역</h2>
              </div>
              <span className="rounded-full bg-[#eef5ea] px-3 py-1 text-xs font-bold text-[#426f55]">
                총 {notifications.length.toLocaleString("ko-KR")}건
              </span>
            </div>
          </div>

          <div className="min-h-72 px-6 py-6 sm:px-8">
            {loading ? (
              <div className="grid min-h-56 place-items-center text-center">
                <div>
                  <div className="mx-auto mb-4 h-9 w-9 animate-spin rounded-full border-4 border-[#dfe5dc] border-t-[#4f7a61]" />
                  <p className="text-sm font-semibold text-slate-500">
                    알림을 불러오는 중입니다.
                  </p>
                </div>
              </div>
            ) : notifications.length === 0 ? (
              <div className="grid min-h-56 place-items-center text-center">
                <div>
                  <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-slate-100 text-slate-400">
                    <MailOpen size={24} strokeWidth={2.2} />
                  </div>
                  <p className="mt-4 text-sm font-bold text-slate-700">
                    아직 알림이 없습니다.
                  </p>
                  <p className="mt-1 text-xs text-slate-400">
                    새로운 펀딩 알림이 생기면 이곳에 표시됩니다.
                  </p>
                </div>
              </div>
            ) : (
              <>
                <div className="grid gap-3">
                  {notifications.map((notification) => {
                    const isOpen = openId === notification.notificationId;

                    return (
                      <article
                        key={notification.notificationId}
                        className="rounded-2xl border border-slate-100 bg-white p-4 transition hover:border-[#dbe7dc] hover:bg-[#f8faf9] sm:p-5"
                      >
                        <button
                          type="button"
                          className="flex w-full cursor-pointer items-start justify-between gap-4 text-left"
                          onClick={() =>
                            setOpenId(isOpen ? null : notification.notificationId)
                          }
                        >
                          <span className="min-w-0">
                            <span className="block truncate text-base font-bold text-slate-950">
                              {notification.title}
                            </span>
                            <span className="mt-1 block text-xs font-medium text-slate-400">
                              {formatNotificationDate(notification.emailSentAt)}
                            </span>
                          </span>
                          <span
                            className={`mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-full bg-[#eef5ea] text-[#426f55] transition ${
                              isOpen ? "rotate-180" : ""
                            }`}
                          >
                            <ChevronDown size={18} strokeWidth={2.4} />
                          </span>
                        </button>

                        {isOpen && (
                          <div
                            className="mt-4 rounded-2xl bg-[#f8faf9] px-4 py-4 text-sm font-medium leading-7 text-slate-600 ring-1 ring-[#dbe7dc] [&_a]:font-semibold [&_a]:text-[#426f55] [&_p]:my-2"
                            dangerouslySetInnerHTML={{
                              __html: DOMPurify.sanitize(notification.content),
                            }}
                          />
                        )}
                      </article>
                    );
                  })}
                </div>

                <div className="mt-8 flex flex-wrap items-center justify-center gap-2">
                  <button
                    type="button"
                    onClick={() => setCurrentPage((p) => p - 1)}
                    disabled={currentPage === 0}
                    className="h-10 cursor-pointer rounded-xl border border-[#dbe7dc] bg-white px-4 text-sm font-semibold text-slate-600 hover:bg-[#eef5ea] disabled:cursor-not-allowed disabled:opacity-30"
                  >
                    이전
                  </button>

                  {Array.from({ length: totalPages }, (_, i) => i).map((page) => (
                    <button
                      key={page}
                      type="button"
                      onClick={() => setCurrentPage(page)}
                      className={`h-10 min-w-10 cursor-pointer rounded-xl border px-3 text-sm font-bold transition ${
                        currentPage === page
                          ? "border-[#4f7a61] bg-[#4f7a61] text-white"
                          : "border-[#dbe7dc] bg-white text-slate-600 hover:bg-[#eef5ea]"
                      }`}
                    >
                      {page + 1}
                    </button>
                  ))}

                  <button
                    type="button"
                    onClick={() => setCurrentPage((p) => p + 1)}
                    disabled={currentPage === totalPages - 1}
                    className="h-10 cursor-pointer rounded-xl border border-[#dbe7dc] bg-white px-4 text-sm font-semibold text-slate-600 hover:bg-[#eef5ea] disabled:cursor-not-allowed disabled:opacity-30"
                  >
                    다음
                  </button>
                </div>
              </>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}

function formatNotificationDate(value: string) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}
