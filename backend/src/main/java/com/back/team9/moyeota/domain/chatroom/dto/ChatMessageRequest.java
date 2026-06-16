package com.back.team9.moyeota.domain.chatroom.dto;

import lombok.Getter;

@Getter
public class ChatMessageRequest {
    private Long chatRoomId;
    private Long senderId;
    private String message;
}
