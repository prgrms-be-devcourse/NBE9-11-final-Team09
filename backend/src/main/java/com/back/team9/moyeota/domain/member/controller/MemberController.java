package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.*;
import com.back.team9.moyeota.domain.member.service.*;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberLoginService memberLoginService;
    private final boolean cookieSecure;
    private final MemberLogoutService memberLogoutService;
    private final MemberProfileService memberProfileService;
    private final MemberHistoryService memberHistoryService;

    public MemberController(
            MemberService memberService,
            MemberLoginService memberLoginService,
            MemberLogoutService memberLogoutService,
            MemberProfileService memberProfileService,
            MemberHistoryService memberHistoryService,
            @Value("${jwt.cookie-secure}") boolean cookieSecure
    ) {
        this.memberService = memberService;
        this.memberLoginService = memberLoginService;
        this.memberLogoutService = memberLogoutService;
        this.memberProfileService = memberProfileService;
        this.memberHistoryService = memberHistoryService;
        this.cookieSecure = cookieSecure;
    }

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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "USR_GET_MY_INFO_SUCCESS",
                "내 정보 조회 성공",
                memberProfileService.getMyInfo(memberId)
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberUpdateResponse>> updateMyInfo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "USR_UPDATE_MY_INFO_SUCCESS",
                "내 정보 수정 성공",
                memberProfileService.updateMyInfo(memberId, request)
        ));
    }

    @GetMapping("/me/participations")
    public ResponseEntity<ApiResponse<PageResponse<MemberParticipationResponse>>> getMyParticipations(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "USR_GET_MY_PARTICIPATIONS_SUCCESS",
                "내 참여 내역 조회 성공",
                memberHistoryService.getMyParticipations(memberId, pageable)
        ));
    }

    @GetMapping("/me/fundings")
    public ResponseEntity<ApiResponse<PageResponse<MemberFundingResponse>>> getMyFundings(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "USR_GET_MY_FUNDINGS_SUCCESS",
                "내 모집 내역 조회 성공",
                memberHistoryService.getMyFundings(memberId, pageable)
        ));
    }

    @GetMapping("/me/payments")
    public ResponseEntity<ApiResponse<PageResponse<MemberPaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "USR_GET_MY_PAYMENTS_SUCCESS",
                "내 결제 내역 조회 성공",
                memberHistoryService.getMyPayments(memberId, pageable)
        ));
    }
}
