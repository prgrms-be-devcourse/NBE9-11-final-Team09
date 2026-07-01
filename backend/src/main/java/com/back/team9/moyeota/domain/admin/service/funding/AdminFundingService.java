package com.back.team9.moyeota.domain.admin.service.funding;

import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelRequest;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelResponse;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.repository.funding.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFundingService {

    private static final Set<FundingStatus> CANCELLABLE_STATUSES = Set.of(
            FundingStatus.RECRUITING,
            FundingStatus.CONFIRMED
    );

    private final AdminFundingQueryRepository fundingRepository;
    private final PathinfoService pathinfoService;
    private final ParticipationRepository participationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<AdminFundingListResponse> getFundings(Pageable pageable) {
        return PageResponse.from(
                fundingRepository.findAdminFundings(
                        ParticipationStatus.CANCELED,
                        pageable
                )
        );
    }

    @Transactional
    public AdminFundingCancelResponse cancelFunding(
            Long fundingId,
            AdminFundingCancelRequest request
    ) {
        validateCancelReason(request.reason());

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FUNDING_NOT_FOUND
                ));

        validateCancellableStatus(funding);

        funding.cancel();
        pathinfoService.cancelPathinfos(fundingId);

        List<Participation> activeParticipations =
                participationRepository.findByFunding_FundingIdAndStatus(fundingId, ParticipationStatus.ACTIVE);
        for (Participation participation : activeParticipations) {
            eventPublisher.publishEvent(new ParticipationCancelledEvent(participation.getParticipationId()));
        }

        if (!activeParticipations.isEmpty()) {
            try {
                notificationService.sendToFundingParticipants(fundingId, NotificationType.FUNDING_CANCELLED);
            } catch (Exception e) {
                log.warn("FUNDING_CANCELLED 알림 발송 실패 (fundingId={}): {}", fundingId, e.getMessage(), e);
            }
        }

        return AdminFundingCancelResponse.from(funding);
    }

    private void validateCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateCancellableStatus(Funding funding) {
        if (funding.getStatus() == FundingStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ADMIN_FUNDING_ALREADY_CANCELLED
            );
        }

        if (!CANCELLABLE_STATUSES.contains(funding.getStatus())) {
            throw new BusinessException(
                    ErrorCode.ADMIN_FUNDING_CANCEL_NOT_ALLOWED
            );
        }
    }
}
