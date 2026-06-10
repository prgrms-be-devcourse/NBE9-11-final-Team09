package com.back.team9.moyeota.domain.notification.repository;

import com.back.team9.moyeota.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
