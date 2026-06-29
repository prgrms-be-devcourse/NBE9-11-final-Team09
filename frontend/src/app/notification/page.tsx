"use client";

import { useEffect, useState } from "react";
import { getNotifications } from "@/lib/notificationApi";
import { Notification } from "@/types/notification";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

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
    <main className="min-h-screen bg-[#f3f7f1] px-5 py-8">
      <div className="mx-auto max-w-3xl">
        <h1 className="mb-4 text-2xl font-bold text-slate-950">이메일 발송 내역</h1>

      {loading ? (
        <div className="rounded-xl border border-[#dbe7dc] bg-white py-10 text-center text-slate-400 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">불러오는 중...</div>
      ) : notifications.length === 0 ? (
        <div className="rounded-xl border border-[#dbe7dc] bg-white py-10 text-center text-slate-400 shadow-[0_10px_28px_rgba(31,41,55,0.06)]">알림이 없습니다.</div>
      ) : (
        <>
          <div className="space-y-4">
            {notifications.map((notification) => (
              <div key={notification.notificationId} className="rounded-xl border border-[#dbe7dc] bg-white p-4 shadow-[0_10px_28px_rgba(31,41,55,0.04)]">
                <h2 className="font-semibold text-slate-950">{notification.title}</h2>
                <p className="mt-2 text-sm leading-6 text-slate-600">{notification.content}</p>
                <p className="mt-3 text-xs text-slate-400">
                  {new Date(notification.emailSentAt).toLocaleString()}
                </p>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-center gap-2 mt-8">
            <button
              onClick={() => setCurrentPage((p) => p - 1)}
              disabled={currentPage === 0}
              className="rounded-lg border border-[#dbe7dc] bg-white px-3 py-1 text-sm font-semibold text-slate-700 hover:bg-[#eef5ea] disabled:opacity-30"
            >
              이전
            </button>
            {Array.from({ length: totalPages }, (_, i) => i).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`rounded-lg border px-3 py-1 text-sm font-semibold ${
                  currentPage === page ? "border-[#4f7a61] bg-[#4f7a61] text-white" : "border-[#dbe7dc] bg-white text-slate-700 hover:bg-[#eef5ea]"
                }`}
              >
                {page + 1}
              </button>
            ))}
            <button
              onClick={() => setCurrentPage((p) => p + 1)}
              disabled={currentPage === totalPages - 1}
              className="rounded-lg border border-[#dbe7dc] bg-white px-3 py-1 text-sm font-semibold text-slate-700 hover:bg-[#eef5ea] disabled:opacity-30"
            >
              다음
            </button>
          </div>
        </>
      )}
      </div>
    </main>
  );
}
