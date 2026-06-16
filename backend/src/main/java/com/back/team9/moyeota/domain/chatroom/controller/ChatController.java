package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request) {

        // /sub/chatroom/{id} 로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/sub/chatroom/" + request.getChatRoomId(),
                request
        );
    }
}
