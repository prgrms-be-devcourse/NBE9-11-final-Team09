package com.back.team9.moyeota.domain.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;

@Getter


public class ChatMessageRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
    private String message;
}
