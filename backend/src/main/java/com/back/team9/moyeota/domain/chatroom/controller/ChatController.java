package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.entity.Message;
import com.back.team9.moyeota.domain.chatroom.service.ChatMessageService;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid ChatMessageRequest request, Principal principal) {

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        Long memberId = (Long) auth.getPrincipal();

        Message saved = chatMessageService.sendMessage(request, memberId);

        Long hostId = chatRoomService.getHostId(request.getChatRoomId());
        // /sub/chatroom/{id} 로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/sub/chatroom/" + request.getChatRoomId(),
                MessageResponse.from(saved, hostId)
        );
    }
}
