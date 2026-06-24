package com.back.team9.moyeota.domain.notification.repository;

import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByMember_MemberIdAndFunding_FundingIdAndNotificationType(
            Long memberId,
            Long fundingId,
            NotificationType notificationType
    );

    @Query("""
        select n.member.memberId
        from Notification n
        where n.funding.fundingId = :fundingId
          and n.notificationType = :notificationType
          and n.member.memberId in :memberIds
        """)
    List<Long> findSentMemberIds(
            @Param("fundingId") Long fundingId,
            @Param("notificationType") NotificationType notificationType,
            @Param("memberIds") List<Long> memberIds
    );
}
