package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.auth.*;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Member Auth", description = "회원 인증 관련 API")
public interface MemberAuthControllerDocs {

    @Operation(
            summary = "회원가입 요청",
            description = "이메일·비밀번호·닉네임으로 회원가입을 요청합니다. 요청 후 입력한 이메일로 인증 메일이 발송됩니다."
    )
    ResponseEntity<ApiResponse<Void>> requestSignup(
            @Valid @RequestBody MemberSignupRequest request
    );

    @Operation(
            summary = "이메일 인증 메일 재발송",
            description = "이메일 인증 메일을 재발송합니다. 인증 메일이 만료되었거나 받지 못한 경우 사용합니다."
    )
    ResponseEntity<ApiResponse<Void>> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    );

    @Operation(
            summary = "이메일 인증 확인 및 회원가입 완료",
            description = "이메일로 받은 인증 토큰을 확인하고 회원가입을 최종 완료합니다."
    )
    ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    );

    @Operation(
            summary = "로그인",
            description = "이메일·비밀번호로 로그인합니다. 응답 헤더의 Authorization에 Access Token, 쿠키에 Refresh Token이 설정됩니다."
    )
    ResponseEntity<ApiResponse<MemberLoginResponse>> login(
            @Valid @RequestBody MemberLoginRequest request
    );

    @Operation(
            summary = "카카오 소셜 로그인",
            description = "카카오 인가 코드로 소셜 로그인합니다. 신규 회원인 경우 자동으로 회원가입 처리됩니다."
    )
    ResponseEntity<ApiResponse<MemberLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoAuthorizationCodeLoginRequest request
    );

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 세션을 종료합니다. Access Token을 블랙리스트에 등록하고 Refresh Token 쿠키를 만료시킵니다."
    )
    ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Bearer {accessToken}", required = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}
