package com.back.team9.moyeota.domain.funding.entity;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(
                name = "idx_funding_status_departure_date",
                columnList = "status, departure_date"
        )
})
public class Funding extends BaseEntity {

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
    @Column(nullable = false)
    private TripType tripType;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    public static Funding create(
            Member member,
            String title,
            String content,
            LocalDate departureDate,
            BusType busType,
            Integer minParticipants,
            BigDecimal totalPrice,
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

    public void closeRecruitment() {
        this.status = FundingStatus.CLOSED;
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
            BigDecimal totalPrice,
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
