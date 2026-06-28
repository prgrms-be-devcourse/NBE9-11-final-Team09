"use client";

import { useEffect, useState } from "react";
import { getNotifications } from "@/lib/notificationApi";
import { Notification } from "@/types/notification";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // 👇 어떤 알림이 열려있는지 관리
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
    <div className="max-w-3xl mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">이메일 발송 내역</h1>

      {loading ? (
        <div className="text-center py-10 text-gray-400">불러오는 중...</div>
      ) : notifications.length === 0 ? (
        <div className="text-center py-10 text-gray-400">알림이 없습니다.</div>
      ) : (
        <>
          <div className="space-y-4">
            {notifications.map((notification) => {
              const isOpen = openId === notification.notificationId;

              return (
                <div
                  key={notification.notificationId}
                  className="border rounded-lg p-4"
                >
                  {/* 제목 (클릭 가능) */}
                  <h2
                    className="font-semibold cursor-pointer hover:text-blue-600"
                    onClick={() =>
                      setOpenId(isOpen ? null : notification.notificationId)
                    }
                  >
                    {notification.title}
                  </h2>

                  {/* 내용 (토글) */}
                  {isOpen && (
                    <div
                      className="text-sm text-gray-600 mt-2"
                      dangerouslySetInnerHTML={{
                        __html: notification.content,
                      }}
                    />
                  )}

                  <p className="text-xs text-gray-400 mt-3">
                    {new Date(notification.emailSentAt).toLocaleString()}
                  </p>
                </div>
              );
            })}
          </div>

          {/* pagination 그대로 */}
          <div className="flex items-center justify-center gap-2 mt-8">
            <button
              onClick={() => setCurrentPage((p) => p - 1)}
              disabled={currentPage === 0}
              className="px-3 py-1 rounded border text-sm disabled:opacity-30 hover:bg-gray-100"
            >
              이전
            </button>

            {Array.from({ length: totalPages }, (_, i) => i).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`px-3 py-1 rounded border text-sm ${
                  currentPage === page
                    ? "bg-gray-900 text-white border-gray-900"
                    : "hover:bg-gray-100"
                }`}
              >
                {page + 1}
              </button>
            ))}

            <button
              onClick={() => setCurrentPage((p) => p + 1)}
              disabled={currentPage === totalPages - 1}
              className="px-3 py-1 rounded border text-sm disabled:opacity-30 hover:bg-gray-100"
            >
              다음
            </button>
          </div>
        </>
      )}
    </div>
  );
}