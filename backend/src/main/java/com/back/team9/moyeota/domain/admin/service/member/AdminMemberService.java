package com.back.team9.moyeota.domain.admin.service.member;

import com.back.team9.moyeota.domain.admin.dto.member.*;
import com.back.team9.moyeota.domain.admin.repository.member.AdminMemberQueryRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final AdminMemberQueryRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminMemberListResponse> getMembers(Pageable pageable) {
        return PageResponse.from(
                memberRepository.findAll(pageable)
                        .map(AdminMemberListResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse getMember(Long memberId) {
        Member member = getMemberById(memberId);

        return AdminMemberDetailResponse.of(
                member,
                memberRepository.countParticipationsByMemberId(memberId),
                memberRepository.countFundingsByMemberId(memberId),
                memberRepository.countPaymentsByMemberId(memberId)
        );
    }

    @Transactional
    public AdminMemberWithdrawResponse withdrawMember(
            Long memberId,
            AdminMemberWithdrawRequest request
    ) {
        Member member = getMemberById(memberId);

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.ADMIN_TARGET_ALREADY_WITHDRAWN);
        }

        member.withdraw();

        return AdminMemberWithdrawResponse.from(member);
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
