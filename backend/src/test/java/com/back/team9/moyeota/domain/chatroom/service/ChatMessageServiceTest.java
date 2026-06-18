package com.back.team9.moyeota.domain.chatroom.service;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
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
import java.util.List;
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

        given(chatRoomService.getRoomById(1L))
                .willReturn(room);

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        given(messageRepository.save(any(Message.class)))
                .willReturn(savedMessage);

        // when
        Message result = chatMessageService.sendMessage(request);

        // then
        assertThat(result.getMessageId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("안녕하세요");

        verify(chatRoomService).getRoomById(1L);
        verify(memberRepository).findById(1L);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("메시지 전송 실패 - 사용자 없음")
    void sendMessage_사용자없음_예외발생() {

        ChatMessageRequest request = new ChatMessageRequest();

        ReflectionTestUtils.setField(request, "chatRoomId", 1L);
        ReflectionTestUtils.setField(request, "senderId", 1L);
        ReflectionTestUtils.setField(request, "message", "안녕하세요");

        given(memberRepository.findById(1L))
                .willReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatMessageService.sendMessage(request)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 채팅방 없음")
    void sendMessage_채팅방없음_예외발생() {

        ChatMessageRequest request = new ChatMessageRequest();

        ReflectionTestUtils.setField(request, "chatRoomId", 1L);
        ReflectionTestUtils.setField(request, "senderId", 1L);
        ReflectionTestUtils.setField(request, "message", "안녕하세요");

        given(chatRoomService.getRoomById(1L))
                .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatMessageService.sendMessage(request)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 조회 성공")
    void getMessages_채팅방존재_메시지목록반환() {
        // given
        Long chatRoomId = 1L;
        Long hostId = 10L;

        Member member = mock(Member.class);
        ChatRoom chatRoom = mock(ChatRoom.class);

        Message message1 = Message.builder()
                .messageId(1L)
                .chatRoom(chatRoom)
                .member(member)
                .content("안녕하세요")
                .createdAt(LocalDateTime.now())
                .build();

        Message message2 = Message.builder()
                .messageId(2L)
                .chatRoom(chatRoom)
                .member(member)
                .content("반갑습니다")
                .createdAt(LocalDateTime.now())
                .build();

        List<Message> messages = List.of(message1, message2);

        given(messageRepository.findByChatRoom_ChatroomIdOrderByCreatedAtAsc(chatRoomId))
                .willReturn(messages);

        given(chatRoomService.getHostId(chatRoomId))
                .willReturn(hostId);

        // when
        List<MessageResponse> result = chatMessageService.getMessages(chatRoomId);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getContent()).isEqualTo("안녕하세요");
        assertThat(result.get(1).getContent()).isEqualTo("반갑습니다");

        assertThat(result.get(0).isHost()).isEqualTo(false);
        assertThat(result.get(1).isHost()).isEqualTo(false);

        verify(messageRepository)
                .findByChatRoom_ChatroomIdOrderByCreatedAtAsc(chatRoomId);

        verify(chatRoomService).getHostId(chatRoomId);
    }

    @Test
    @DisplayName("메시지 조회 성공 - 호스트 포함")
    void getMessages_호스트포함_호스트플래그정상동작() {
        // given
        Long chatRoomId = 1L;
        Long hostId = 10L;

        ChatRoom chatRoom = mock(ChatRoom.class);
        Member hostMember = mock(Member.class);
        Member normalMember = mock(Member.class);

        // host message
        Message hostMessage = Message.builder()
                .messageId(1L)
                .chatRoom(chatRoom)
                .member(hostMember)
                .content("호스트 메시지")
                .createdAt(LocalDateTime.now())
                .build();

        // normal message
        Message normalMessage = Message.builder()
                .messageId(2L)
                .chatRoom(chatRoom)
                .member(normalMember)
                .content("일반 메시지")
                .createdAt(LocalDateTime.now())
                .build();

        given(messageRepository.findByChatRoom_ChatroomIdOrderByCreatedAtAsc(chatRoomId))
                .willReturn(List.of(hostMessage, normalMessage));

        given(chatRoomService.getHostId(chatRoomId))
                .willReturn(hostId);

        // hostMember id 설정
        given(hostMember.getMemberId()).willReturn(hostId);
        given(normalMember.getMemberId()).willReturn(99L);

        // when
        List<MessageResponse> result = chatMessageService.getMessages(chatRoomId);

        // then
        assertThat(result.get(0).isHost()).isTrue();
        assertThat(result.get(1).isHost()).isFalse();
    }

    @Test
    @DisplayName("메시지 조회 성공 - 메시지 없음")
    void getMessages_메시지없음_빈리스트반환() {
        // given
        Long chatRoomId = 1L;
        Long hostId = 10L;

        given(messageRepository.findByChatRoom_ChatroomIdOrderByCreatedAtAsc(chatRoomId))
                .willReturn(List.of());

        given(chatRoomService.getHostId(chatRoomId))
                .willReturn(hostId);

        // when
        List<MessageResponse> result = chatMessageService.getMessages(chatRoomId);

        // then
        assertThat(result).isEmpty();

        verify(chatRoomService).getHostId(chatRoomId);
    }
}
