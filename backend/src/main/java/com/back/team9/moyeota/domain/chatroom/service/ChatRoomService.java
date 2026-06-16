package com.back.team9.moyeota.domain.chatroom.service;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.ChatRoomResponse;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoomStatus;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;

    public ChatRoomResponse createRoom(Long fundingId){
        if(chatRoomRepository.existsByFundingFundingId(fundingId)){
            throw new BusinessException(ErrorCode.CHAY_ROOM_ALREADY_EXISTS);
        }

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(()->new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        ChatRoom room = ChatRoom.builder()
                .funding(funding)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        return ChatRoomResponse.from(saved);
    }

    public ChatRoom getRoom(Long fundingId){
        return chatRoomRepository.findByFundingFundingId(fundingId)
                .orElseThrow(()-> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    public Long getHostId(Long chatRoomId) {

        Long fundingId = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(()->new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .getFunding().getFundingId();

        return fundingRepository.findHostIdByFundingId(fundingId);
    }
}
