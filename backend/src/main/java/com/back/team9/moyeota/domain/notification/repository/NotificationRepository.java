package com.back.team9.moyeota.domain.notification.repository;

import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    Page<Notification> findByMember_MemberIdOrderByCreatedAtDesc(
            Long memberId,
            Pageable pageable
    );
    @Query(
            value = """
        select n
        from Notification n
        join fetch n.funding
        where n.member.memberId = :memberId
        order by n.createdAt desc
    """,
            countQuery = """
        select count(n)
        from Notification n
        where n.member.memberId = :memberId
    """
    )
    Page<Notification> findAllWithFunding(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
        select n
        from Notification n
        join fetch n.member
        where n.notificationId = :notificationId
    """)
    Optional<Notification> findByIdWithMember(Long notificationId);
}
