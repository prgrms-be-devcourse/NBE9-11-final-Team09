package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FundingUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        String title,
        String content,

        @NotNull(message = "버스 종류는 필수입니다.")
        BusType busType,

        @NotNull(message = "최소 인원은 필수입니다.")
        @Positive(message = "최소 인원은 1명 이상이어야 합니다.")
        Integer minParticipants,

        @NotNull(message = "왕복 여부는 필수입니다.")
        TripType tripType,

        @NotNull(message = "목표 금액은 필수입니다.")
        @Positive(message = "목표 금액은 0보다 커야 합니다.")
        Integer totalPrice,

        @NotEmpty(message = "최소 1개의 노선이 필요합니다.")
        List<PathInfoUpdateRequest> paths
) {
}
