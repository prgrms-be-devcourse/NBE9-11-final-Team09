package com.back.team9.moyeota.domain.member.service.history;

import com.back.team9.moyeota.domain.member.dto.history.MemberFundingResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberPaymentResponse;
import com.back.team9.moyeota.domain.member.repository.MemberFundingQueryRepository;
import com.back.team9.moyeota.domain.member.repository.MemberHostSeatQueryRepository;
import com.back.team9.moyeota.domain.member.repository.MemberParticipationQueryRepository;
import com.back.team9.moyeota.domain.member.repository.MemberPaymentQueryRepository;
import com.back.team9.moyeota.domain.member.repository.projection.MemberFundingSummary;
import com.back.team9.moyeota.domain.member.repository.projection.MemberHostSeatSummary;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberHistoryService {

    private final MemberParticipationQueryRepository participationQueryRepository;
    private final MemberFundingQueryRepository fundingQueryRepository;
    private final MemberPaymentQueryRepository paymentQueryRepository;
    private final MemberHostSeatQueryRepository hostSeatQueryRepository;

    @Transactional(readOnly = true)
    public PageResponse<MemberParticipationResponse> getMyParticipations(
            Long memberId,
            Pageable pageable
    ) {
        Page<MemberParticipationResponse> participations =
                participationQueryRepository.findByMember_MemberId(
                        memberId,
                        pageable
                ).map(MemberParticipationResponse::from);

        return PageResponse.from(participations);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberFundingResponse> getMyFundings(
            Long memberId,
            Pageable pageable
    ) {
        Page<MemberFundingSummary> fundingSummaries =
                fundingQueryRepository.findMyFundings(
                        memberId,
                        ParticipationStatus.CANCELED,
                        pageable
                );

        Map<Long, List<String>> hostSeatNumbersByFundingId =
                getHostSeatNumbersByFundingId(
                        memberId,
                        fundingSummaries
                );

        Page<MemberFundingResponse> fundings =
                fundingSummaries.map(summary -> toMemberFundingResponse(
                        summary,
                        hostSeatNumbersByFundingId
                ));

        return PageResponse.from(fundings);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberPaymentResponse> getMyPayments(
            Long memberId,
            Pageable pageable
    ) {
        Page<MemberPaymentResponse> payments =
                paymentQueryRepository.findByParticipation_Member_MemberId(
                        memberId,
                        pageable
                ).map(MemberPaymentResponse::from);

        return PageResponse.from(payments);
    }

    private Map<Long, List<String>> getHostSeatNumbersByFundingId(
            Long memberId,
            Page<MemberFundingSummary> fundingSummaries
    ) {
        List<Long> fundingIds = fundingSummaries.getContent()
                .stream()
                .map(MemberFundingSummary::getFundingId)
                .toList();

        if (fundingIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return hostSeatQueryRepository.findHostSeatsByFundingIds(
                        memberId,
                        fundingIds
                )
                .stream()
                .collect(Collectors.groupingBy(
                        MemberHostSeatSummary::getFundingId,
                        Collectors.mapping(
                                MemberHostSeatSummary::getSeatNumber,
                                Collectors.toList()
                        )
                ));
    }

    private MemberFundingResponse toMemberFundingResponse(
            MemberFundingSummary summary,
            Map<Long, List<String>> hostSeatNumbersByFundingId
    ) {
        return new MemberFundingResponse(
                summary.getFundingId(),
                summary.getFundingTitle(),
                summary.getDepartureDate(),
                summary.getCurrentParticipants(),
                summary.getMaxParticipants(),
                summary.getStatus(),
                summary.getCreatedAt(),
                hostSeatNumbersByFundingId.getOrDefault(
                        summary.getFundingId(),
                        Collections.emptyList()
                )
        );
    }
}