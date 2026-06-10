package com.back.team9.moyeota.domain.chatroom.repository;

import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
