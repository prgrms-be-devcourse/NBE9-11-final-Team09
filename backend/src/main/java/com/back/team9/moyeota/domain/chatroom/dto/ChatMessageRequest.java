package com.back.team9.moyeota.domain.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NonNull;

@Getter

// TODO: JWT 인증 적용 시 제거
// 현재는 클라이언트가 senderId를 전달하지만,
// 추후에는 WebSocket 인증 정보를 통해 사용자 ID를 추출한다.
public class ChatMessageRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotNull(message = "송신자 ID는 필수입니다.")
    private Long senderId;

    @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다.")
    private String message;
}
