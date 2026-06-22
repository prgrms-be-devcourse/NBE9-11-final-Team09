package com.back.team9.moyeota.domain.member.service.profile;

import com.back.team9.moyeota.domain.member.dto.profile.MemberInfoResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateRequest;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateResponse;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

// 회원 프로필 조회 및 수정 서비스
@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(Long memberId) {
        return MemberInfoResponse.from(getMember(memberId));
    }

    @Transactional
    public MemberUpdateResponse updateMyInfo(
            Long memberId,
            MemberUpdateRequest request
    ) {
        validateUpdateRequest(request);

        Member member = getMember(memberId);

        if (request.nickname() != null
                && !request.nickname().equals(member.getNickname())
                && memberRepository.existsByNicknameAndMemberIdNot(
                request.nickname(),
                memberId
        )) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateProfile(
                request.nickname(),
                request.phoneNumber(),
                LocalDateTime.now(clock)
        );

        return MemberUpdateResponse.from(member);
    }

    private Member getMember(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND
                ));
    }

    private void validateUpdateRequest(MemberUpdateRequest request) {
        if (request.nickname() == null && request.phoneNumber() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.nickname() != null && request.nickname().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
