export interface ChatRoomResponse {
    chatRoomId: number;
    fundingId: number;
    status: "ACTIVE" | "CLOSED";
    createdAt: string;
}

export interface ChatMessageResponse {
    messageId: number;
    chatRoomId: number;
    host: boolean;
    content: string;
    createdAt: string;
}

export interface ChatMessageRequest {
    chatRoomId: number;
    senderId: number;
    message: string;
}