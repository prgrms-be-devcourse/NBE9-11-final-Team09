package com.back.team9.moyeota.domain.chatroom.dto;

import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoomStatus;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {

    private Long chatRoomId;
    private Long fundingId;
    private ChatRoomStatus status;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getChatroomId())
                .fundingId(chatRoom.getFunding().getFundingId())
                .status(chatRoom.getStatus())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}