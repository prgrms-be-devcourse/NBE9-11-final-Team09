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
        <div className="flex flex-col h-screen max-w-2xl mx-auto">

            {/* 상단 헤더 */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-white">
                <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">💬 문의</span>
                    <span className="text-sm text-gray-500">| {fundingTitle}</span>
                </div>
                <button
                    type="button"
                    onClick={() => router.back()}
                    className="text-sm text-gray-500"
                >
                    이전으로
                </button>
            </div>

            {!isConnected && (
                <div className="text-center text-xs text-gray-400 py-1 bg-gray-50">
                    연결 중...
                </div>
            )}

            <div className="flex-1 overflow-y-auto px-4 py-4 bg-gray-50">
                {messages.map((message) => (
                    <MessageBubble key={message.messageId} message={message} />
                ))}
                <div ref={bottomRef} />
            </div>

            <MessageInput onSend={sendMessage} disabled={!isConnected} />

        </div>
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
            <div className="flex items-center justify-center h-screen text-sm text-gray-500">
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