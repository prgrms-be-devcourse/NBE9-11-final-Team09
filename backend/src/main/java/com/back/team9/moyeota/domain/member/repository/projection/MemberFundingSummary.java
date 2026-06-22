package com.back.team9.moyeota.domain.member.repository.projection;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MemberFundingSummary {

    Long getFundingId();

    String getFundingTitle();

    LocalDate getDepartureDate();

    Long getCurrentParticipants();

    Integer getMaxParticipants();

    FundingStatus getStatus();

    LocalDateTime getCreatedAt();

}