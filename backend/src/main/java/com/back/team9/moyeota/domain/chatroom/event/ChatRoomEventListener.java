package com.back.team9.moyeota.domain.chatroom.event;

import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomEventListener {

    private final ChatRoomService chatRoomService;

    @EventListener
    public void createChatRoom(FundingCreatedEvent event) {
        try {
            chatRoomService.createRoomForFunding(event.funding());
        } catch (Exception e) {
            log.error(
                    "펀딩 생성 후 채팅방 자동 생성 실패 - fundingId: {}",
                    event.funding() != null ? event.funding().getFundingId() : null,
                    e
            );
            throw e;
        }
    }
}
