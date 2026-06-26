package com.back.team9.moyeota.domain.chatroom.service;

import com.back.team9.moyeota.domain.chatroom.dto.ChatMessageRequest;
import com.back.team9.moyeota.domain.chatroom.dto.MessageResponse;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.Message;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.chatroom.repository.MessageRepository;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomService chatRoomService;


    public Message sendMessage(ChatMessageRequest request, Long memberId) {

        ChatRoom room = chatRoomService.getRoomById(request.getChatRoomId());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Message message = Message.builder()
                .chatRoom(room)
                .member(member)
                .content(request.getMessage())
                .build();

        return messageRepository.save(message);
    }

    public List<MessageResponse> getMessages(Long chatRoomId){
        List<Message> messages = messageRepository.findByChatRoom_ChatroomIdOrderByCreatedAtAsc(chatRoomId);
        Long hostId = chatRoomService.getHostId(chatRoomId);

        return messages.stream()
                .map(m -> MessageResponse.from(m, hostId))
                .toList();
    }
}
