package com.back.team9.moyeota.domain.chatroom.controller;

import com.back.team9.moyeota.domain.chatroom.controller.ChatRoomController;
import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoomStatus;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("채팅방 생성에 성공한다")
    void 채팅방_생성_성공() throws Exception {

        Long fundingId = 1L;

        ChatRoomResponse response = ChatRoomResponse.builder()
                .chatRoomId(1L)
                .fundingId(fundingId)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        given(chatRoomService.createRoom(fundingId))
                .willReturn(response);

        mockMvc.perform(post("/chatrooms/{fundingId}", fundingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("채팅방 생성 성공"))
                .andExpect(jsonPath("$.data.chatRoomId").value(1))
                .andExpect(jsonPath("$.data.fundingId").value(1));

        verify(chatRoomService).createRoom(fundingId);
    }
}
