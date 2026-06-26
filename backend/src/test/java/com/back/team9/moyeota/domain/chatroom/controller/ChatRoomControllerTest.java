package com.back.team9.moyeota.domain.chatroom.controller;


import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.service.ChatMessageService;
import com.back.team9.moyeota.domain.chatroom.service.ChatRoomService;
import com.back.team9.moyeota.global.jwt.filter.JwtAuthenticationFilter;

import com.back.team9.moyeota.global.jwt.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ChatRoomController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;
    @MockitoBean
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("채팅 메시지 조회 성공")
    void 채팅메시지_조회_성공() throws Exception {

        Long chatRoomId = 1L;

        MessageResponse response1 = MessageResponse.builder()
                .messageId(1L)
                .chatRoomId(chatRoomId)
                .host(false)
                .content("안녕하세요")
                .createdAt(LocalDateTime.now())
                .build();

        MessageResponse response2 = MessageResponse.builder()
                .messageId(2L)
                .chatRoomId(chatRoomId)
                .host(true)
                .content("반갑습니다")
                .createdAt(LocalDateTime.now())
                .build();

        given(chatMessageService.getMessages(chatRoomId))
                .willReturn(List.of(response1, response2));

        mockMvc.perform(get("/chatrooms/{chatRoomId}/messages", chatRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("채팅 메시지 조회 성공"))
                .andExpect(jsonPath("$.data[0].messageId").value(1))
                .andExpect(jsonPath("$.data[0].content").value("안녕하세요"))
                .andExpect(jsonPath("$.data[0].host").value(false))
                .andExpect(jsonPath("$.data[1].messageId").value(2))
                .andExpect(jsonPath("$.data[1].content").value("반갑습니다"))
                .andExpect(jsonPath("$.data[1].host").value(true));

        verify(chatMessageService).getMessages(chatRoomId);
    }
}
