package com.back.team9.moyeota.domain.pathinfo.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PathInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pathinfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private String departureAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region departureRegion;

    @Column(nullable = false)
    private String arrivalAddress;

    @Enumerated(EnumType.STRING)
    private Region arrivalRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PathInfoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
