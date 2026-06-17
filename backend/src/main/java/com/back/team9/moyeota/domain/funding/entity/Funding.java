package com.back.team9.moyeota.domain.funding.entity;

import com.back.team9.moyeota.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Funding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fundingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusType busType;

    @Column(nullable = false)
    private Integer minParticipants;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Boolean paybackHold;

    @Enumerated(EnumType.STRING)
    private TripType tripType;

    @Column(nullable = false)
    private Integer totalPrice;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Funding create(
            Member member,
            String title,
            String content,
            LocalDate departureDate,
            BusType busType,
            Integer minParticipants,
            Integer totalPrice,
            TripType tripType
    ) {
        Funding funding = new Funding();
        funding.member = member;
        funding.title = title;
        funding.content = content;
        funding.departureDate = departureDate;
        funding.status = FundingStatus.RECRUITING;
        funding.busType = busType;
        funding.totalPrice = totalPrice;
        funding.minParticipants = minParticipants;
        funding.maxParticipants = busType.getCapacity();
        funding.paybackHold = false;
        funding.tripType = tripType;
        return funding;
    }

    public void cancel() {
        this.status = FundingStatus.CANCELLED;
    }

    public void confirm() {
        this.status = FundingStatus.CONFIRMED;
    }

    public void fail() {
        this.status = FundingStatus.FAILED;
    }

    public void complete() {
        this.status = FundingStatus.COMPLETED;
    }

    public void update(
            String title,
            String content,
            BusType busType,
            Integer minParticipants,
            Integer totalPrice,
            TripType tripType,
            LocalDate departureDate
    ) {
        this.title = title;
        this.content = content;
        this.busType = busType;
        this.minParticipants = minParticipants;
        this.maxParticipants = busType.getCapacity();
        this.totalPrice = totalPrice;
        this.tripType = tripType;
        this.departureDate = departureDate;
    }

    public void updateTitleAndContent(
            String title,
            String content
    ) {
        this.title = title;
        this.content = content;
    }
}
