package com.back.team9.moyeota.domain.admin.controller.member;

import com.back.team9.moyeota.domain.admin.dto.member.*;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Member", description = "관리자 회원 관리 API")
public interface AdminMemberControllerDocs {

    @Operation(
            summary = "회원 목록 조회",
            description = "전체 회원 목록을 페이지 단위로 조회합니다. 기본 정렬: 가입일 내림차순, 페이지당 20건."
    )
    ResponseEntity<ApiResponse<PageResponse<AdminMemberListResponse>>> getMembers(
            Pageable pageable
    );

    @Operation(
            summary = "회원 상세 조회",
            description = "회원 ID로 특정 회원의 상세 정보를 조회합니다."
    )
    ResponseEntity<ApiResponse<AdminMemberDetailResponse>> getMember(
            @Parameter(description = "조회할 회원 ID", required = true) @PathVariable Long memberId
    );

    @Operation(
            summary = "회원 강제 탈퇴",
            description = "관리자가 특정 회원을 강제 탈퇴 처리합니다. 탈퇴 사유를 입력해야 합니다."
    )
    ResponseEntity<ApiResponse<AdminMemberWithdrawResponse>> withdrawMember(
            @Parameter(description = "탈퇴 처리할 회원 ID", required = true) @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberWithdrawRequest request
    );
}
