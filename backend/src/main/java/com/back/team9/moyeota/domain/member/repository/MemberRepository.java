package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndMemberIdNot(
            String nickname,
            Long memberId
    );

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(
            Provider provider,
            String providerId
    );
}
