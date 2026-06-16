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
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;

    //TODO:추후 익명으로 만들 예정
    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .chatRoomId(message.getChatRoom().getChatroomId())
                .senderId(message.getMember().getMemberId())
                .senderName(message.getMember().getNickname())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}