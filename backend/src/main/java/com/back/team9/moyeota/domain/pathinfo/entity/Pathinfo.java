package com.back.team9.moyeota.domain.pathinfo.entity;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pathinfo_funding_direction",
                        columnNames = {"funding_id", "direction"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_pathinfo_status_departure_time",
                        columnList = "status, departure_time"
                )
        }
)
public class Pathinfo extends BaseEntity {

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
    @Column(nullable = false)
    private Region arrivalRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PathinfoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusType busType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    public static Pathinfo create(
            Funding funding,
            LocalDateTime departureTime,
            String departureAddress,
            Region departureRegion,
            String arrivalAddress,
            Region arrivalRegion,
            Direction direction
    ) {
        Pathinfo pathinfo = new Pathinfo();
        pathinfo.funding = funding;
        pathinfo.departureTime = departureTime;
        pathinfo.departureAddress = departureAddress;
        pathinfo.departureRegion = departureRegion;
        pathinfo.arrivalAddress = arrivalAddress;
        pathinfo.arrivalRegion = arrivalRegion;
        pathinfo.busType = funding.getBusType();
        pathinfo.direction = direction;
        pathinfo.status = PathinfoStatus.PENDING;
        return pathinfo;
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
        this.busType = funding.getBusType();
        this.direction = direction;
        this.status = PathinfoStatus.PENDING;
    }

    public void cancel() {
        this.status = PathinfoStatus.CANCELLED;
    }

    public void changeBusType(BusType busType) {
        this.busType = busType;
    }

    public void complete() {
        this.status = PathinfoStatus.COMPLETED;
    }
}
