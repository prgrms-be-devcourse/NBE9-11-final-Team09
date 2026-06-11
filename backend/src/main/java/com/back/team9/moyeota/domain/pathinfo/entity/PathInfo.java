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
    private Long pathInfoId;

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
    @Column(nullable = false)
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

    public static PathInfo create(
            Funding funding,
            LocalDateTime departureTime,
            String departureAddress,
            Region departureRegion,
            String arrivalAddress,
            Region arrivalRegion,
            Direction direction
    ) {
        PathInfo pathInfo = new PathInfo();
        pathInfo.funding = funding;
        pathInfo.departureTime = departureTime;
        pathInfo.departureAddress = departureAddress;
        pathInfo.departureRegion = departureRegion;
        pathInfo.arrivalAddress = arrivalAddress;
        pathInfo.arrivalRegion = arrivalRegion;
        pathInfo.direction = direction;
        pathInfo.status = PathInfoStatus.PENDING;
        return pathInfo;
    }

    public void update(
            LocalDateTime departureTime,
            String departureAddress,
            Region departureRegion,
            String arrivalAddress,
            Region arrivalRegion,
            Direction direction
    ) {
        this.departureTime = departureTime;
        this.departureAddress = departureAddress;
        this.departureRegion = departureRegion;
        this.arrivalAddress = arrivalAddress;
        this.arrivalRegion = arrivalRegion;
        this.direction = direction;
    }
    public void cancel() {
        this.status = PathInfoStatus.CANCELLED;
    }
}
