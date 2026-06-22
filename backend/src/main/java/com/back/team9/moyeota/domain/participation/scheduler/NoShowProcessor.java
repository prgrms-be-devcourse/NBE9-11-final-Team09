package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowProcessor {

    private final ParticipationRepository participationRepository;
    private final NoShowSingleProcessor noShowSingleProcessor;

    public void processNoShow(LocalDateTime now) {

        LocalDateTime deadline = now.plusHours(24);

        List<Participation> targets = participationRepository.findNoShowTargets(
                now,
                deadline,
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        );

        if (targets.isEmpty()) {
            log.info("NO_SHOW 처리 대상 없음");
            return;
        }

        log.info("NO_SHOW 처리 시작 - 대상 인원: {}명", targets.size());

        for (Participation participation : targets) {
            try {
                noShowSingleProcessor.processOneNoShow(participation.getParticipationId());
                log.info("NO_SHOW 처리 완료 - participationId: {}",
                        participation.getParticipationId());
            } catch (Exception e) {
                log.error("NO_SHOW 처리 실패 - participationId: {}",
                        participation.getParticipationId(), e);
            }
        }

        log.info("NO_SHOW 처리 완료 - 총 {}명 처리", targets.size());
    }
}