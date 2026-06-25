// lib/notificationApi.ts
import { getAccessToken, AuthenticationRequiredError } from "@/lib/member-api";
import { PageResponse } from "@/types/notification";
import { Notification } from "@/types/notification";

export const getNotifications = async (page = 0, size = 20) => {
  const token = getAccessToken();
  if (!token) throw new AuthenticationRequiredError();

  const query = new URLSearchParams({ page: String(page), size: String(size) });
  const response = await fetch(`/api/notifications?${query.toString()}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!response.ok) throw new Error("알림 조회 실패");
  
  const json = await response.json();
  return json.data; // ← data만 반환
};