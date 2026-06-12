package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.MemberLoginRequest;
import com.back.team9.moyeota.domain.member.dto.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.service.MemberLoginService;
import com.back.team9.moyeota.domain.member.service.MemberService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    private final MemberLoginService memberLoginService;

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
        MemberLoginResponse response = memberLoginService.login(request);

        return ResponseEntity.ok(new ApiResponse<>(
                "USR_LOGIN_SUCCESS",
                "로그인 성공",
                response
        ));
    }
}