package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PathinfoTimeValidator {

    private final Clock clock;

    public void validateDepartureDate(LocalDateTime departureTime) {
        if (departureTime == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        if (departureTime.isBefore(LocalDateTime.now(clock).plusDays(14))) {
            throw new BusinessException(
                    ErrorCode.DEPARTURE_DATE_TOO_SOON
            );
        }
    }
}
