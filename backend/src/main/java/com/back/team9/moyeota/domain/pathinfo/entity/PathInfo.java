package com.back.team9.moyeota.domain.pathinfo.entity;

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

    @Column(nullable = false)
    private Long fundingId;

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
