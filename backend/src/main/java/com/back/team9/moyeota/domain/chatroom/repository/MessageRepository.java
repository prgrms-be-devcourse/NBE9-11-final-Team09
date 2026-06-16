package com.back.team9.moyeota.domain.chatroom.repository;

import com.back.team9.moyeota.domain.chatroom.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
