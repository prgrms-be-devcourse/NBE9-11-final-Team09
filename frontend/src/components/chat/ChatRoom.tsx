"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useChat } from "@/hooks/useChat";
import { getMyProfile } from "@/lib/member-api";
import MessageBubble from "@/components/chat/MessageBubble";
import MessageInput from "@/components/chat/MessageInput";

interface ChatRoomProps {
    chatRoomId: number;
    fundingTitle: string;
}

interface ChatRoomInnerProps {
    chatRoomId: number;
    fundingTitle: string;
    memberId: number;
}

function ChatRoomInner({ chatRoomId, fundingTitle, memberId }: ChatRoomInnerProps) {
    const router = useRouter();
    const bottomRef = useRef<HTMLDivElement>(null);

    const { messages, sendMessage, isConnected } = useChat({
        chatRoomId,
        memberId,
    });

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    return (
        <main className="min-h-[calc(100dvh-64px)] bg-[#f3f7f1] px-4 py-4">
        <div className="mx-auto flex h-[calc(100dvh-96px)] max-w-3xl min-h-[520px] flex-col overflow-hidden rounded-3xl border border-[#dbe7dc] bg-white shadow-[0_14px_36px_rgba(31,41,55,0.08)]">

            <div className="flex shrink-0 items-center justify-between gap-4 border-b border-[#dbe7dc] bg-white px-5 py-4">
                <div className="min-w-0">
                    <p className="text-xs font-bold tracking-widest text-[#357465]">CHAT</p>
                    <div className="mt-1 flex min-w-0 items-center gap-2">
                        <span className="text-base font-black text-slate-950">문의</span>
                        <span className="truncate text-sm font-semibold text-slate-500">
                            {fundingTitle}
                        </span>
                    </div>
                </div>
                <button
                    type="button"
                    onClick={() => router.back()}
                    className="shrink-0 rounded-xl border border-[#dbe7dc] bg-white px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-[#eef5ea] hover:text-[#426f55]"
                >
                    이전으로
                </button>
            </div>

            {!isConnected && (
                <div className="shrink-0 border-b border-[#dbe7dc] bg-[#eef5ea] py-2 text-center text-xs font-semibold text-[#426f55]">
                    연결 중...
                </div>
            )}

            <div className="min-h-0 flex-1 overflow-y-auto bg-[#f8faf9] px-5 py-5">
                {messages.map((message) => (
                    <MessageBubble key={message.messageId} message={message} />
                ))}
                <div ref={bottomRef} />
            </div>

            <MessageInput onSend={sendMessage} disabled={!isConnected} />

        </div>
        </main>
    );
}

export default function ChatRoom({ chatRoomId, fundingTitle }: ChatRoomProps) {
    const [memberId, setMemberId] = useState<number | null>(null);

    useEffect(() => {
        getMyProfile()
            .then((profile) => setMemberId(profile.memberId))
            .catch((error) => console.error("유저 정보 조회 실패:", error));
    }, []);

    if (memberId === null) {
        return (
            <div className="flex min-h-[calc(100dvh-64px)] items-center justify-center bg-[#f3f7f1] text-sm font-medium text-slate-500">
                사용자 정보를 불러오는 중입니다...
            </div>
        );
    }

    return (
        <ChatRoomInner
            chatRoomId={chatRoomId}
            fundingTitle={fundingTitle}
            memberId={memberId}
        />
    );
}
