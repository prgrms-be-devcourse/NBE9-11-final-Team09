package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.FundingCreateRequest;
import com.back.team9.moyeota.domain.funding.dto.FundingCreateResponse;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// TODO: 현재 타 도메인의 Repository 메서드를 그대로 사용중. 이후 서비스로 전환 필요
@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FundingCreateResponse createFunding(Long memberId, FundingCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // TODO: 출발일 path 에서 추출
        LocalDate departureDate = LocalDate.parse("2026-08-15"); //임시

        Funding funding = Funding.create(
                member,
                request.title(),
                request.content(),
                departureDate,
                request.busType(),
                request.minParticipants(),
                request.maxParticipants(),
                request.tripType()
        );

        Funding savedFunding = fundingRepository.save(funding);

        // TODO: 채팅방 생성
        //ChatRoom chatRoom = ChatRoom.create(saveFunding);
        return new FundingCreateResponse(
                savedFunding.getFundingId(),
                savedFunding.getStatus(),
                savedFunding.getCreatedAt()
                );

    }
}
