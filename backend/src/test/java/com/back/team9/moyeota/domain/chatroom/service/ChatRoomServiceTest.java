package com.back.team9.moyeota.domain.chatroom.service;

import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoomStatus;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.given;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import com.back.team9.moyeota.domain.funding.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ChatRoomServiceTest {
    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Test
    @DisplayName("채팅방 생성 성공")
    void createRoom_채팅방이존재하지않음_채팅방생성() {
        // given
        Long fundingId = 1L;

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);

        ChatRoom chatRoom = ChatRoom.builder()
                .chatroomId(1L)
                .funding(funding)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        given(chatRoomRepository.existsByFundingFundingId(fundingId))
                .willReturn(false);

        given(fundingRepository.findById(fundingId))
                .willReturn(Optional.of(funding));

        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willReturn(chatRoom);

        // when
        ChatRoomResponse result = chatRoomService.createRoom(fundingId);

        // then
        assertThat(result.getChatRoomId()).isEqualTo(1L);
        assertThat(result.getFundingId()).isEqualTo(fundingId);
        assertThat(result.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);

        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 중복 생성 실패")
    void createRoom_이미채팅방존재_예외발생() {
        // given
        Long fundingId = 1L;

        given(chatRoomRepository.existsByFundingFundingId(fundingId))
                .willReturn(true);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatRoomService.createRoom(fundingId)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.CHAY_ROOM_ALREADY_EXISTS);

        verify(chatRoomRepository, never()).save(any());
        verify(fundingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 펀딩으로 생성 실패")
    void createRoom_펀딩이존재하지않음_예외발생() {
        // given
        Long fundingId = 1L;

        given(chatRoomRepository.existsByFundingFundingId(fundingId))
                .willReturn(false);

        given(fundingRepository.findById(fundingId))
                .willReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatRoomService.createRoom(fundingId)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);

        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 조회 성공")
    void getRoom_채팅방존재_채팅방조회() {
        // given
        Long fundingId = 1L;

        ChatRoom chatRoom = mock(ChatRoom.class);

        given(chatRoomRepository.findByFundingFundingId(fundingId))
                .willReturn(Optional.of(chatRoom));

        // when
        ChatRoom result = chatRoomService.getRoom(fundingId);

        // then
        assertThat(result).isEqualTo(chatRoom);
    }

    @Test
    @DisplayName("채팅방 조회 실패")
    void getRoom_채팅방미존재_예외발생() {
        // given
        Long fundingId = 1L;

        given(chatRoomRepository.findByFundingFundingId(fundingId))
                .willReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatRoomService.getRoom(fundingId)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }
}
