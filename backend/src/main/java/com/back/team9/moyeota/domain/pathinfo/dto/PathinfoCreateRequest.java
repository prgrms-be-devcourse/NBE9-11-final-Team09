package com.back.team9.moyeota.domain.pathinfo.dto;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PathinfoCreateRequest(
        @NotNull(message = "출발 시간은 필수입니다.")
        LocalDateTime departureTime,

        @NotBlank(message = "출발지는 필수입니다.")
        String departureAddress,

        @NotNull(message = "출발 지역은 필수입니다.")
        Region departureRegion,

        @NotBlank(message = "도착지는 필수입니다.")
        String arrivalAddress,

        @NotNull(message = "도착 주소는 필수입니다.")
        Region arrivalRegion,

        @NotNull(message = "방향은 필수입니다.")
        Direction direction
) {
}
