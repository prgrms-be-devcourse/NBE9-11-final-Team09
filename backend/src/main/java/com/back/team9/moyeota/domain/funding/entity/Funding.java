package com.back.team9.moyeota.domain.funding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fundingId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime departureDate;

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
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
