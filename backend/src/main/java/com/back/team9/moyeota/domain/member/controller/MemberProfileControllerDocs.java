package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.profile.MemberInfoResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateRequest;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberWithdrawRequest;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Member Profile", description = "회원 프로필 관련 API")
public interface MemberProfileControllerDocs {

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 회원 본인의 프로필 정보(닉네임, 이메일, 계좌 등)를 조회합니다."
    )
    ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );

    @Operation(
            summary = "내 정보 수정",
            description = "닉네임, 환불 계좌 등 회원 정보를 수정합니다."
    )
    ResponseEntity<ApiResponse<MemberUpdateResponse>> updateMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberUpdateRequest request
    );

    @Operation(
            summary = "회원 탈퇴",
            description = "비밀번호 확인 후 회원 탈퇴를 진행합니다. 탈퇴 처리 후 세션이 즉시 종료됩니다."
    )
    ResponseEntity<ApiResponse<Void>> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "Bearer {accessToken}", required = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody MemberWithdrawRequest request
    );
}
