package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowProcessor {

    private final ParticipationRepository participationRepository;

    @Transactional
    public void processNoShow(LocalDateTime now) {

        // 출발 24시간 이내인 잔액 미납 참여자 조회
        LocalDateTime deadline = now.plusHours(24);

        List<Participation> targets = participationRepository.findNoShowTargets(
                now,
                deadline,
                ParticipationPaymentStatus.ACTIVE,  // 보증금O, 잔액X
                ParticipationStatus.ACTIVE  // 취소되지 않은 참여자
        );

        if (targets.isEmpty()) {
            log.info("NO_SHOW 처리 대상 없음");
            return;
        }

        log.info("NO_SHOW 처리 시작 - 대상 인원: {}명", targets.size());

        for (Participation participation : targets) {
            try {
                // 상태 변경 payment_status → NO_SHOW, status → CANCELED
                participation.markAsNoShow();

                // 가는편 좌석 복구
                participation.getOutboundSeat().release();

                // 오는편 좌석 복구 (왕복인 경우만)
                if (participation.getReturnSeat() != null) {
                    participation.getReturnSeat().release();
                }

                log.info("NO_SHOW 처리 완료 - participationId: {}",
                        participation.getParticipationId());

            } catch (Exception e) {
                // 한 명 실패해도 다음 사람 계속 처리
                log.error("NO_SHOW 처리 실패 - participationId: {}",
                        participation.getParticipationId(), e);
            }
        }
        log.info("NO_SHOW 처리 완료 - 총 {}명 처리", targets.size());
    }
}