package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.entity.Message;
import com.back.team9.moyeota.domain.chatroom.service.ChatMessageService;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request) {

        Message saved = chatMessageService.sendMessage(request);

        // /sub/chatroom/{id} 로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/sub/chatroom/" + request.getChatRoomId(),
                MessageResponse.from(saved)
        );
    }
}
