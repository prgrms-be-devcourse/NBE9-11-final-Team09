package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.FundingCreateRequest;
import com.back.team9.moyeota.domain.funding.dto.FundingCreateResponse;
import com.back.team9.moyeota.domain.funding.dto.FundingDetailResponse;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.repository.PathInfoRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// TODO: 현재 타 도메인의 Repository 메서드를 그대로 사용중. 이후 서비스로 전환 필요
@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final PathInfoRepository pathInfoRepository;

    // 펀딩 생성
    @Transactional
    public FundingCreateResponse createFunding(Long memberId, FundingCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.paths() == null || request.paths().isEmpty()) {
            throw new BusinessException(ErrorCode.PATHINFO_REQUIRED);
        }

        LocalDate departureDate = request.paths()
                .getFirst()
                .departureTime()
                .toLocalDate();

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

        List<PathInfo> pathInfos = request.paths()
                .stream()
                .map(pathRequest -> PathInfo.create(
                        savedFunding,
                        pathRequest.departureTime(),
                        pathRequest.departureAddress(),
                        pathRequest.departureRegion(),
                        pathRequest.arrivalAddress(),
                        pathRequest.arrivalRegion(),
                        pathRequest.direction()
                ))
                .toList();

        pathInfoRepository.saveAll(pathInfos);

        // TODO: 채팅방 생성
        //ChatRoom chatRoom = ChatRoom.create(savedFunding);
        return new FundingCreateResponse(
                savedFunding.getFundingId(),
                savedFunding.getStatus(),
                savedFunding.getCreatedAt()
        );

    }

    // 펀딩 상세 조회
    @Transactional(readOnly = true)
    public FundingDetailResponse getFunding(Long fundingId) {
        Funding funding = findFundingById(fundingId);
        List<PathInfoResponse> pathInfos = pathInfoRepository.findByFunding_FundingId(fundingId)
                .stream()
                .map(PathInfoResponse::from)
                .toList();
        return new FundingDetailResponse(
                funding.getFundingId(),
                funding.getTitle(),
                funding.getContent(),
                funding.getDepartureDate(),
                funding.getStatus(),
                funding.getBusType(),
                funding.getMinParticipants(),
                funding.getMaxParticipants(),
                funding.getTripType(),
                pathInfos,
                0L, //임시 채팅Id
                0, //임시 현재 참가자
                false, //임시 방장 여부
                false, //임시 참여 여부
                funding.getCreatedAt()
        );
    }



    private Funding findFundingById(Long fundingId) {
        return fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));
    }
}
