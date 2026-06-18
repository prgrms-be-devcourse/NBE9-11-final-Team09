package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.auth.*;
import com.back.team9.moyeota.domain.member.service.MemberLoginService;
import com.back.team9.moyeota.domain.member.service.MemberLogoutService;
import com.back.team9.moyeota.domain.member.service.MemberService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/members")
public class MemberAuthController {

    public MemberAuthController(
            MemberService memberService,
            MemberLoginService memberLoginService,
            MemberLogoutService memberLogoutService,
            @Value("${jwt.cookie-secure}") boolean cookieSecure
    ) {
        this.memberService = memberService;
        this.memberLoginService = memberLoginService;
        this.memberLogoutService = memberLogoutService;
        this.cookieSecure = cookieSecure;
    }

    private final MemberService memberService;
    private final MemberLoginService memberLoginService;
    private final MemberLogoutService memberLogoutService;
    private final boolean cookieSecure;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> requestSignup(
            @Valid @RequestBody MemberSignupRequest request
    ) {
        memberService.requestSignup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "USR_SIGNUP_REQUEST_SUCCESS",
                        "인증 메일이 발송되었습니다."
                ));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        memberService.confirmEmailVerification(request);

        return ResponseEntity.ok(new ApiResponse<>(
                "USR_EMAIL_VERIFICATION_SUCCESS",
                "이메일 인증 및 회원가입이 완료되었습니다."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MemberLoginResponse>> login(
            @Valid @RequestBody MemberLoginRequest request
    ) {
        MemberLoginResult result = memberLoginService.login(request);

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(
                        result.refreshTokenExpiresIn()
                ))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ApiResponse<>(
                        "USR_LOGIN_SUCCESS",
                        "로그인 성공",
                        result.response()
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        memberLogoutService.logout(authorization);

        ResponseCookie expiredRefreshTokenCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshTokenCookie.toString()
                )
                .body(new ApiResponse<>(
                        "USR_LOGOUT_SUCCESS",
                        "로그아웃 되었습니다."
                ));
    }
}
