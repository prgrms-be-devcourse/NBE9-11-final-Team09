import type { Metadata } from "next";
import AppHeader from "@/components/layout/AppHeader";
import "./globals.css";

export const metadata: Metadata = {
  title: "모여타",
  description: "버스 대절 매칭 플랫폼",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full">
      <body className="min-h-full flex flex-col">
        <AppHeader />
        <div className="flex-1">{children}</div>
      </body>
    </html>
  );
}
