import { ChatRoomResponse, ChatMessageResponse } from "@/types/chat";
import { authorizedRequest } from "@/lib/member-api";
import { ApiResponse } from "@/types/member";  // ← 여기서 가져오기!

export async function getChatRoomByFundingId(
    fundingId: number
): Promise<ChatRoomResponse> {
    const response = await authorizedRequest<ApiResponse<ChatRoomResponse>>(
        `/api/chatrooms/funding/${fundingId}`
    );
    return response.data;
}

export async function getChatMessages(
    chatRoomId: number
): Promise<ChatMessageResponse[]> {
    const response = await authorizedRequest<ApiResponse<ChatMessageResponse[]>>(
        `/api/chatrooms/${chatRoomId}/messages`
    );
    return response.data;
}