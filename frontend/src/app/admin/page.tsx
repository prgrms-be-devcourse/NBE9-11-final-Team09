import type { Metadata } from "next";
import AdminDashboard from "./admin-dashboard";

export const metadata: Metadata = {
  title: "관리자 대시보드 | 모여타",
  description: "모여타 서비스 운영 대시보드",
};

export default function AdminPage() {
  return <AdminDashboard />;
}
