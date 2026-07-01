package com.back.team9.moyeota.domain.admin.controller.member;

import com.back.team9.moyeota.domain.admin.dto.member.*;
import com.back.team9.moyeota.domain.admin.service.member.AdminMemberService;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController implements AdminMemberControllerDocs {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberListResponse>>> getMembers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_MEMBERS_SUCCESS",
                "회원 목록 조회에 성공했습니다.",
                adminMemberService.getMembers(pageable)
        ));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> getMember(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_MEMBER_SUCCESS",
                "회원 상세 조회에 성공했습니다.",
                adminMemberService.getMember(memberId)
        ));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberWithdrawResponse>> withdrawMember(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberWithdrawRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_WITHDRAW_MEMBER_SUCCESS",
                "회원이 강제 탈퇴 처리되었습니다.",
                adminMemberService.withdrawMember(memberId, request)
        ));
    }
}
