package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.service.ChatMessageService;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatrooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
            @PathVariable Long fundingId
    ){
        ChatRoomResponse response = chatRoomService.createRoom(fundingId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "채팅방 생성 성공",
                        response
                )
        );
    }
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(@PathVariable Long chatRoomId) {
        List<MessageResponse> response = chatMessageService.getMessages(chatRoomId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "채팅 메시지 조회 성공",
                        response
                )
        );
    }
    @GetMapping("/funding/{fundingId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoomByFundingId(
            @PathVariable Long fundingId
    ) {
        ChatRoomResponse response = chatRoomService.getRoomResponse(fundingId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "채팅방 조회 성공",
                        response
                )
        );
    }
}
