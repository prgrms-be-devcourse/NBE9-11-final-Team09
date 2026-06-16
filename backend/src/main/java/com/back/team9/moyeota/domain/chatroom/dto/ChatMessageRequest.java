package com.back.team9.moyeota.domain.chatroom.dto;

import lombok.Getter;

@Getter

// TODO: JWT 인증 적용 시 제거
// 현재는 클라이언트가 senderId를 전달하지만,
// 추후에는 WebSocket 인증 정보를 통해 사용자 ID를 추출한다.
public class ChatMessageRequest {
    private Long chatRoomId;
    private Long senderId;
    private String message;
}
