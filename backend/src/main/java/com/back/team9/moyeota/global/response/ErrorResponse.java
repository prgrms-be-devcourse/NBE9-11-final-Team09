package com.back.team9.moyeota.global.response;

import com.back.team9.moyeota.global.error.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
){
    public static ErrorResponse of(ErrorCode errorCode){
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now()
        );
    }
    public static ErrorResponse of(
            ErrorCode errorCode,
            String message
    ) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message,
                LocalDateTime.now()
        );
    }
}
