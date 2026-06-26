package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.history.MemberFundingResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberPaymentResponse;
import com.back.team9.moyeota.domain.member.service.history.MemberHistoryService;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberHistoryController implements MemberHistoryControllerDocs {

    public MemberHistoryController(
            MemberHistoryService memberHistoryService
    ) {
        this.memberHistoryService = memberHistoryService;
    }

    private final MemberHistoryService memberHistoryService;

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
