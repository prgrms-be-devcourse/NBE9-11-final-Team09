import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { ChatMessageResponse, ChatMessageRequest } from "@/types/chat";
import { getChatMessages } from "@/lib/chatApi";
import { getAccessToken } from "@/lib/member-api";

interface UseChatProps {
    chatRoomId: number;
    memberId: number;
}

interface UseChatReturn {
    messages: ChatMessageResponse[];
    sendMessage: (content: string) => void;
    isConnected: boolean;
}

export function useChat({ chatRoomId, memberId }: UseChatProps): UseChatReturn {
    const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        let ignore = false;

        getChatMessages(chatRoomId)
            .then((msgs) => {
                if (!ignore) setMessages(msgs);
            })
            .catch((error) => {
                console.error("채팅 메시지 조회 실패:", error);
            });

        const accessToken = getAccessToken();

        if (!accessToken) {
            return () => {
                ignore = true;
            };
        }

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${process.env.NEXT_PUBLIC_API_URL}/ws`),
            connectHeaders: {
                Authorization: `Bearer ${accessToken}`,
            },

            onConnect: () => {
                setIsConnected(true);

                client.subscribe(`/sub/chatroom/${chatRoomId}`, (frame) => {
                    const received: ChatMessageResponse = JSON.parse(frame.body);
                    setMessages((prev) => [...prev, received]);
                });
            },

            onDisconnect: () => {
                setIsConnected(false);
            },

            onStompError: (frame) => {
                console.error("STOMP 에러:", frame);
                setIsConnected(false);
            },

            onWebSocketError: (error) => {
                console.error("WebSocket 에러:", error);
                setIsConnected(false);
            },
        });

        client.activate();
        clientRef.current = client;

        return () => {
            ignore = true;
            client.deactivate();
            clientRef.current = null;
        };
    }, [chatRoomId]);

    const sendMessage = (content: string) => {
        const trimmedContent = content.trim();

        if (!trimmedContent) return;
        if (!clientRef.current?.connected) return;

        const request: ChatMessageRequest = {
            chatRoomId,
            senderId: memberId,
            message: trimmedContent,
        };

        clientRef.current.publish({
            destination: "/pub/chat.send",
            body: JSON.stringify(request),
        });
    };

    return { messages, sendMessage, isConnected };
}
