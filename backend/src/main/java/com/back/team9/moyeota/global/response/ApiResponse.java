package com.back.team9.moyeota.global.response;

public record ApiResponse<T>(
        String resultCode,
        String msg,
        T data
) {
    public ApiResponse(String resultCode, String msg){
        this(resultCode, msg,null);
    }
}
