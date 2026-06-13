package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.funding.validator.FundingValidator;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final PathinfoService pathinfoService;
    private final FundingValidator fundingValidator;

    // 펀딩 생성
    @Transactional
    public FundingCreateResponse createFunding(Long memberId, FundingCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        fundingValidator.validateFundingRequest(
                request.minParticipants(),
                request.busType(),
                request.totalPrice()
        );

        LocalDate departureDate = request.route()
                .departureTime()
                .toLocalDate();

        Funding funding = Funding.create(
                member,
                request.title(),
                request.content(),
                departureDate,
                request.busType(),
                request.minParticipants(),
                request.totalPrice(),
                request.tripType()
        );

        Funding savedFunding = fundingRepository.save(funding);

        pathinfoService.createPathinfos(
                savedFunding,
                request.tripType(),
                request.route()
        );

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

        List<PathinfoResponse> pathinfos = pathinfoService.getPathinfoResponses(fundingId);

        return FundingDetailResponse.from(
                funding,
                pathinfos,
                0,      // TODO 현재 참가자 수
                null,   // TODO 채팅방 ID
                false,  // TODO 방장 여부
                false   // TODO 참여 여부
        );
    }

    // 펀딩 목록 조회
    @Transactional(readOnly = true)
    public List<FundingListResponse> getFundingList() {

        List<Funding> fundings = fundingRepository.findAllWithMember();

        List<Long> fundingIds =
                fundings.stream()
                        .map(Funding::getFundingId)
                        .toList();

        List<Pathinfo> pathinfos =
                pathinfoService
                        .findByFunding_FundingIdInAndDirection(
                                fundingIds,
                                Direction.OUTBOUND
                        );

        Map<Long, Pathinfo> pathinfoMap =
                pathinfos.stream()
                        .collect(
                                Collectors.toMap(
                                        path ->
                                                path.getFunding()
                                                        .getFundingId(),
                                        path -> path
                                )
                        );

        return fundings.stream()
                .map(funding ->
                        FundingListResponse.from(
                                funding,
                                pathinfoMap.get(
                                        funding.getFundingId()
                                ),
                                0
                        )
                )
                .toList();
    }

    // 펀딩 취소
    // TODO: 방장일 경우
    @Transactional
    public void cancelFunding(Long memberId, Long fundingId) {
        Funding funding = findFundingById(fundingId);
        fundingValidator.validateHost(funding, memberId);
        if (funding.getStatus() == FundingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.FUNDING_ALREADY_CANCELLED);
        }
        funding.cancel();
        pathinfoService.cancelPathinfos(fundingId);
    }

    @Transactional
    public void updateFunding(Long memberId, Long fundingId, FundingUpdateRequest request) {

        Funding funding = findFundingById(fundingId);
        fundingValidator.validateHost(funding, memberId);
        int currentParticipants = 0; // TODO

        if (currentParticipants > 0) {
            updateFundingWithParticipants(funding, request);
            return;
        }

        fundingValidator.validateFundingRequest(
                request.minParticipants(),
                request.busType(),
                request.totalPrice()
        );

        updateFundingWithoutParticipants(funding, request);

    }

    private void updateFundingWithParticipants(Funding funding, FundingUpdateRequest request) {

        boolean changed =
                !Objects.equals(funding.getBusType(), request.busType())
                        || !Objects.equals(funding.getMinParticipants(), request.minParticipants())
                        || !Objects.equals(funding.getTotalPrice(), request.totalPrice())
                        || !Objects.equals(funding.getTripType(), request.tripType())
                        || pathinfoService.isRouteChanged(
                        funding.getFundingId(),
                        funding.getTripType(),
                        request.route()
                );

        if (changed) {
            throw new BusinessException(
                    ErrorCode.FUNDING_RESTRICTED_UPDATE
            );
        }

        funding.updateTitleAndContent(request.title(), request.content());
    }

    private void updateFundingWithoutParticipants(
            Funding funding,
            FundingUpdateRequest request
    ) {

        LocalDate departureDate = request.route()
                .departureTime()
                .toLocalDate();

        funding.update(
                request.title(),
                request.content(),
                request.busType(),
                request.minParticipants(),
                request.totalPrice(),
                request.tripType(),
                departureDate
        );

        pathinfoService.updatePathinfos(
                funding,
                request.tripType(),
                request.route()
        );
        pathinfoService.syncBusType(
                funding.getFundingId(),
                funding.getBusType()
        );

    }


    private Funding findFundingById(Long fundingId) {
        return fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));
    }



}
