package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "ChatRoom", description = "WebSocket 기반 실시간 채팅방 REST API")
public interface ChatRoomControllerDocs {

    @Operation(
            summary = "채팅 메시지 목록 조회",
            description = "채팅방 ID로 전체 메시지 내역을 조회합니다. WebSocket 연결 전 초기 메시지 로드에 사용합니다."
    )
    ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @Parameter(description = "조회할 채팅방 ID", required = true) @PathVariable Long chatRoomId
    );

    @Operation(
            summary = "펀딩 ID로 채팅방 조회",
            description = "펀딩 ID로 해당 펀딩의 채팅방 정보를 조회합니다."
    )
    ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoomByFundingId(
            @Parameter(description = "조회할 펀딩 ID", required = true) @PathVariable Long fundingId
    );
}
