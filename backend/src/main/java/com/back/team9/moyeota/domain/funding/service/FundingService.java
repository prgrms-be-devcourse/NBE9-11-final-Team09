package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import com.back.team9.moyeota.domain.funding.event.FundingSeatsRecreateEvent;
import com.back.team9.moyeota.domain.funding.policy.FundingPricePolicy;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.funding.validator.FundingValidator;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.back.team9.moyeota.domain.participation.entity.ParticipationStatus.ACTIVE;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final PathinfoService pathinfoService;
    private final ApplicationEventPublisher eventPublisher;
    private final ParticipationRepository participationRepository;
    private final ChatRoomRepository chatRoomRepository;

    // 펀딩 생성
    @Transactional
    public FundingCreateResponse createFunding(Long memberId, FundingCreateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateHostSeatRequest(
                request.tripType(),
                request.hostOutboundSeatNumber(),
                request.hostReturnSeatNumber()
        );
        pathinfoService.validatePathinfoRequest(
                request.tripType(),
                request.route()
        );

        BigDecimal totalPrice = FundingPricePolicy.calculateTotalPrice(
                request.route(),
                request.busType(),
                request.tripType()
        );
        FundingValidator.validateFundingRequest(
                request.minParticipants(),
                request.busType()
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
                totalPrice,
                request.tripType()
        );

        Funding savedFunding = fundingRepository.save(funding);

        pathinfoService.createPathinfos(
                savedFunding,
                request.tripType(),
                request.route()
        );
        eventPublisher.publishEvent(new FundingCreatedEvent(
                savedFunding,
                request.hostOutboundSeatNumber(),
                request.hostReturnSeatNumber()
        ));

        return new FundingCreateResponse(
                savedFunding.getFundingId(),
                savedFunding.getStatus(),
                savedFunding.getCreatedAt()
        );

    }

    // 펀딩 상세 조회
    @Transactional(readOnly = true)
    public FundingDetailResponse getFunding(Long fundingId, Long memberId) {

        Funding funding = findFundingById(fundingId);
        int currentParticipants = countActiveParticipants(fundingId);
        List<PathinfoResponse> pathinfos = pathinfoService.getPathinfoResponsesForDetail(funding);
        Long chatRoomId = findChatRoomIdByFundingId(fundingId);
        boolean isHost = memberId != null
                && funding.getMember().getMemberId().equals(memberId);

        boolean isJoined = memberId != null
                && participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndStatus(
                fundingId,
                memberId,
                ACTIVE
        );
        return FundingDetailResponse.from(
                funding,
                pathinfos,
                currentParticipants,
                chatRoomId,
                isHost,
                isJoined
        );
    }

    // 펀딩 목록 조회(핕터링)
    @Transactional(readOnly = true)
    public PageResponse<FundingListResponse> getFundingList(
            FundingSearchCondition condition,
            Pageable pageable
    ) {

        Page<Funding> fundings =
                fundingRepository.findPageByCondition(condition, pageable);

        List<Long> fundingIds =
                fundings.getContent().stream()
                        .map(Funding::getFundingId)
                        .toList();

        Map<Long, Integer> participantCountMap =
                getActiveParticipantCountMap(fundingIds);

        List<Pathinfo> pathinfos =
                fundingIds.isEmpty()
                        ? Collections.emptyList()
                        : pathinfoService.findByFundingIdsAndDirection(
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

        Page<FundingListResponse> response =
                fundings.map(funding ->
                        FundingListResponse.from(
                                funding,
                                pathinfoMap.get(
                                        funding.getFundingId()
                                ),
                                participantCountMap.getOrDefault(
                                        funding.getFundingId(),
                                        0
                                )
                        )
                );

        return PageResponse.from(response);
    }

    // 펀딩 취소(방장검증, 연결된 노선 취소 처리)
    @Transactional
    public void cancelFunding(Long memberId, Long fundingId) {
        Funding funding = findFundingById(fundingId);
        FundingValidator.validateHost(funding, memberId);
        FundingValidator.validateModifiableStatus(funding);
        funding.cancel();
        pathinfoService.cancelPathinfos(fundingId);
    }

    // 펀딩 수정
    // 참가자 여부에 따라 수정 가능 범위 상이
    @Transactional
    public void updateFunding(Long memberId, Long fundingId, FundingUpdateRequest request) {

        Funding funding = findFundingById(fundingId);
        FundingValidator.validateHost(funding, memberId);
        FundingValidator.validateModifiableStatus(funding);

        int currentParticipants = countActiveParticipants(fundingId);

        if (currentParticipants > 0) {
            updateFundingWithParticipants(funding, request);
            return;
        }

        pathinfoService.validatePathinfoRequest(
                request.tripType(),
                request.route()
        );

        BigDecimal totalPrice = FundingPricePolicy.calculateTotalPrice(
                request.route(),
                request.busType(),
                request.tripType()
        );

        FundingValidator.validateFundingRequest(
                request.minParticipants(),
                request.busType()
        );

        updateFundingWithoutParticipants(funding, request, totalPrice);

    }

    // 참가자 있을경우 제목/내용만 수정 허용
    private void updateFundingWithParticipants(Funding funding, FundingUpdateRequest request) {

        boolean changed =
                !Objects.equals(funding.getBusType(), request.busType())
                        || !Objects.equals(funding.getMinParticipants(), request.minParticipants())
                        || !Objects.equals(funding.getTripType(), request.tripType())
                        || pathinfoService.isRouteChanged(
                        funding.getFundingId(),
                        funding.getTripType(),
                        request.route()
                );

        if (changed) {
            throw new BusinessException(
                    ErrorCode.FUNDING_UPDATE_RESTRICTED_BY_PARTICIPANTS
            );
        }

        funding.updateTitleAndContent(request.title(), request.content());
    }

    // 참가자 없을경우 전체 수정 허용
    private void updateFundingWithoutParticipants(
            Funding funding,
            FundingUpdateRequest request,
            BigDecimal totalPrice
    ) {

        LocalDate departureDate = request.route()
                .departureTime()
                .toLocalDate();
        boolean shouldRecreateSeats = isSeatStructureChanged(funding, request);
        if (shouldRecreateSeats) {
            validateHostSeatRequest(
                    request.tripType(),
                    request.hostOutboundSeatNumber(),
                    request.hostReturnSeatNumber()
            );
        }

        funding.update(
                request.title(),
                request.content(),
                request.busType(),
                request.minParticipants(),
                totalPrice,
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

        if (shouldRecreateSeats) {
            eventPublisher.publishEvent(
                    new FundingSeatsRecreateEvent(
                            funding,
                            request.hostOutboundSeatNumber(),
                            request.hostReturnSeatNumber()
                    )
            );
        }

    }

    // 펀딩 조회 공통 메서드
    private Funding findFundingById(Long fundingId) {
        return fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));
    }
    // 참가자 수 조회(int로 반환)
    private int countActiveParticipants(Long fundingId) {
        return (int) participationRepository.countByFunding_FundingIdAndStatus(
                fundingId,
                ACTIVE
        );
    }

    private Map<Long, Integer> getActiveParticipantCountMap(List<Long> fundingIds) {
        if (fundingIds.isEmpty()) {
            return Map.of();
        }

        return participationRepository.countByFundingIdsAndStatus(
                        fundingIds,
                        ACTIVE
                )
                .stream()
                .collect(Collectors.toMap(
                        ParticipationRepository.FundingParticipationCount::getFundingId,
                        count -> count.getCount().intValue()
                ));
    }

    private Long findChatRoomIdByFundingId(Long fundingId) {
        return chatRoomRepository.findByFundingFundingId(fundingId)
                .map(ChatRoom::getChatroomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private boolean isSeatStructureChanged(
            Funding funding,
            FundingUpdateRequest request
    ) {
        return !Objects.equals(funding.getBusType(), request.busType())
                || !Objects.equals(funding.getTripType(), request.tripType());
    }

    private void validateHostSeatRequest(
            TripType tripType,
            String hostOutboundSeatNumber,
            String hostReturnSeatNumber
    ) {
        if (isBlank(hostOutboundSeatNumber)) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }

        if (tripType == TripType.ROUND && isBlank(hostReturnSeatNumber)) {
            throw new BusinessException(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
