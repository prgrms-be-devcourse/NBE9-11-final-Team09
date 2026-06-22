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

    // 각 참여자마다 독립 트랜잭션 → 실패 시 해당 건만 롤백
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOneNoShow(Long participationId) {

        // 새 트랜잭션 안에서 다시 조회
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        // 조회 시점과 처리 시점 사이에 잔액 결제가 완료됐을 수 있으므로 상태 재검증
        if (participation.getPaymentStatus() != ParticipationPaymentStatus.ACTIVE
                || participation.getStatus() != ParticipationStatus.ACTIVE) {
            return;
        }

        // 상태 변경 payment_status → NO_SHOW, status → CANCELED
        participation.markAsNoShow();

        // 가는편 좌석 복구
        participation.getOutboundSeat().release();

        // 오는편 좌석 복구 (왕복인 경우만)
        if (participation.getReturnSeat() != null) {
            participation.getReturnSeat().release();
        }
    }
}
