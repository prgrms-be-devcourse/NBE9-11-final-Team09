package com.back.team9.moyeota.domain.chatroom.service;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.Message;
import com.back.team9.moyeota.domain.chatroom.repository.MessageRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.given;

import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceTest {
    @InjectMocks
    private ChatMessageService chatMessageService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomService chatRoomService;


    @Test
    @DisplayName("메시지 전송 성공")
    void sendMessage_유효한요청_메시지저장() {
        // given
        ChatMessageRequest request = new ChatMessageRequest();

        ReflectionTestUtils.setField(request, "chatRoomId", 1L);
        ReflectionTestUtils.setField(request, "senderId", 1L);
        ReflectionTestUtils.setField(request, "message", "안녕하세요");

        ChatRoom room = mock(ChatRoom.class);
        Member member = mock(Member.class);

        Message savedMessage = Message.builder()
                .messageId(1L)
                .chatRoom(room)
                .member(member)
                .content("안녕하세요")
                .createdAt(LocalDateTime.now())
                .build();

        given(chatRoomService.getRoom(1L)).willReturn(room);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

        // when
        Message result = chatMessageService.sendMessage(request);

        // then
        assertThat(result.getMessageId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("안녕하세요");

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("메시지 전송 실패 - 사용자 없음")
    void sendMessage_사용자없음_예외발생() {
        // given
        ChatMessageRequest request = new ChatMessageRequest();

        ReflectionTestUtils.setField(request, "chatRoomId", 1L);
        ReflectionTestUtils.setField(request, "senderId", 1L);
        ReflectionTestUtils.setField(request, "message", "안녕하세요");

        ChatRoom room = mock(ChatRoom.class);

        given(chatRoomService.getRoom(1L)).willReturn(room);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatMessageService.sendMessage(request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 채팅방 없음")
    void sendMessage_채팅방없음_예외발생() {
        // given
        ChatMessageRequest request = new ChatMessageRequest();

        ReflectionTestUtils.setField(request, "chatRoomId", 1L);
        ReflectionTestUtils.setField(request, "senderId", 1L);
        ReflectionTestUtils.setField(request, "message", "안녕하세요");

        given(chatRoomService.getRoom(1L))
                .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatMessageService.sendMessage(request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);

        verify(messageRepository, never()).save(any());
    }
}
