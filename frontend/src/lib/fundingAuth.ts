"use client";

import { useSyncExternalStore } from "react";

export function getFundingAccessToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return (
    localStorage.getItem("accessToken") ??
    sessionStorage.getItem("accessToken")
  );
}

export function getFundingMemberId() {
  const token = getFundingAccessToken();

  if (!token) {
    return null;
  }

  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = JSON.parse(window.atob(normalized)) as { sub?: string };
    return decoded.sub ? Number(decoded.sub) : null;
  } catch {
    return null;
  }
}

function subscribe() {
  return () => {};
}

function getSnapshot() {
  return Boolean(getFundingAccessToken());
}

function getServerSnapshot() {
  return false;
}

export function useFundingLoggedIn() {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
}
