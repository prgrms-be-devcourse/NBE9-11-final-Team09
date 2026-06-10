package com.back.team9.moyeota.global.dto;

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
}
