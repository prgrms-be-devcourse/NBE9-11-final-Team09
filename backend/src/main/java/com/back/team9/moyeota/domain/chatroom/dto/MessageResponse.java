package com.back.team9.moyeota.domain.chatroom.dto;

import com.back.team9.moyeota.domain.chatroom.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private Long messageId;
    private Long chatRoomId;
    private boolean host;
    private String content;
    private LocalDateTime createdAt;

    public static MessageResponse from(
            Message message,
            Long hostId
    ) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .chatRoomId(message.getChatRoom().getChatroomId())
                .host(message.getMember().getMemberId().equals(hostId))
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}