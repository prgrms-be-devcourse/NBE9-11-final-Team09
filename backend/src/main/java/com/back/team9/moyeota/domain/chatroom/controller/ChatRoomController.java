package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatrooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

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
}
