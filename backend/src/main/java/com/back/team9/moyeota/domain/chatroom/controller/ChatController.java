package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.entity.Message;
import com.back.team9.moyeota.domain.chatroom.model.ChatPrincipal;
import com.back.team9.moyeota.domain.chatroom.service.ChatMessageService;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload @Valid ChatMessageRequest request,
            Principal principal
    ) {

        Authentication auth = (Authentication) principal;
        ChatPrincipal chatPrincipal = (ChatPrincipal) auth.getPrincipal();

        Long memberId = chatPrincipal.getMemberId();

        Message saved = chatMessageService.sendMessage(request, memberId);
        Long hostId = chatRoomService.getHostId(request.getChatRoomId());

        messagingTemplate.convertAndSend(
                "/sub/chatroom/" + request.getChatRoomId(),
                MessageResponse.from(saved,hostId)
        );
    }
}
