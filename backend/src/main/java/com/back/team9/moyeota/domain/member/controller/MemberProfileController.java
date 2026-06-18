package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.*;
import com.back.team9.moyeota.domain.member.service.MemberLogoutService;
import com.back.team9.moyeota.domain.member.service.MemberProfileService;
import com.back.team9.moyeota.domain.member.service.MemberWithdrawService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/members")
public class MemberProfileController {

    public MemberProfileController(
            MemberProfileService memberProfileService,
            MemberWithdrawService memberWithdrawService,
            MemberLogoutService memberLogoutService,
            @Value("${jwt.cookie-secure}") boolean cookieSecure
    ) {
        this.memberProfileService = memberProfileService;
        this.memberWithdrawService = memberWithdrawService;
        this.memberLogoutService = memberLogoutService;
        this.cookieSecure = cookieSecure;
    }

    private final MemberProfileService memberProfileService;
    private final MemberWithdrawService memberWithdrawService;
    private final MemberLogoutService memberLogoutService;
    private final boolean cookieSecure;

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

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long memberId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody MemberWithdrawRequest request
    ) {
        memberWithdrawService.withdraw(memberId, request);
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
                        "USR_WITHDRAW_SUCCESS",
                        "회원탈퇴가 완료되었습니다."
                ));
    }
}
