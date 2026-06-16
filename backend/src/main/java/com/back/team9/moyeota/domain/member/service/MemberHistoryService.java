package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.MemberFundingResponse;
import com.back.team9.moyeota.domain.member.dto.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.dto.MemberPaymentResponse;
import com.back.team9.moyeota.domain.member.repository.MemberFundingQueryRepository;
import com.back.team9.moyeota.domain.member.repository.MemberParticipationQueryRepository;
import com.back.team9.moyeota.domain.member.repository.MemberPaymentQueryRepository;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberHistoryService {

    private final MemberParticipationQueryRepository participationQueryRepository;
    private final MemberFundingQueryRepository fundingQueryRepository;
    private final MemberPaymentQueryRepository paymentQueryRepository;

    @Transactional(readOnly = true)
    public PageResponse<MemberParticipationResponse> getMyParticipations(
            Long memberId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<MemberParticipationResponse> participations =
                participationQueryRepository.findByMember_MemberId(
                        memberId,
                        pageRequest
                ).map(MemberParticipationResponse::from);

        return PageResponse.from(participations);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberFundingResponse> getMyFundings(
            Long memberId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return PageResponse.from(
                fundingQueryRepository.findMyFundings(
                        memberId,
                        ParticipationStatus.CANCELED,
                        pageRequest
                )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberPaymentResponse> getMyPayments(
            Long memberId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<MemberPaymentResponse> payments =
                paymentQueryRepository.findByParticipation_Member_MemberId(
                        memberId,
                        pageRequest
                ).map(MemberPaymentResponse::from);

        return PageResponse.from(payments);
    }
}