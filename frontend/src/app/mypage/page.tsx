import type { Metadata } from "next";
import MypageClient from "./mypage-client";

export const metadata: Metadata = {
  title: "마이페이지 | 모여타",
  description: "회원 정보와 모여타 이용 내역을 확인합니다.",
};

export default function MypagePage() {
  return <MypageClient />;
}
