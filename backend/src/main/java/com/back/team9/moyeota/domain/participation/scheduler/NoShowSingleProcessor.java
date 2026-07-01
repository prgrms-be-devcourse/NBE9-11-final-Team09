package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NoShowSingleProcessor {

    private final ParticipationRepository participationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOneNoShow(Long participationId) {

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        //상태 재검증
        if (participation.getPaymentStatus() != ParticipationPaymentStatus.ACTIVE
                || participation.getStatus() != ParticipationStatus.ACTIVE) {
            return;
        }

        participation.markAsNoShow();
        participation.getOutboundSeat().release();

        if (participation.getReturnSeat() != null) {
            participation.getReturnSeat().release();
        }
    }
}
