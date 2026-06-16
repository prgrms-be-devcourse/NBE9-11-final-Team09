package com.back.team9.moyeota.global.init;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class Baseinit {

    private final MemberRepository memberRepository;
    private final FundingRepository fundingRepository;

    @PostConstruct
    public void init() {

        Member member1 = memberRepository.save(
                Member.builder()
                        .email("user1@test.com")
                        .name("사용자1")
                        .nickname("사용자1")
                        .phoneNumber("01011111111")
                        .status(MemberStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Member member2 = memberRepository.save(
                Member.builder()
                        .email("user2@test.com")
                        .name("사용자2")
                        .nickname("사용자2")
                        .phoneNumber("01011111112")
                        .status(MemberStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Member member3 = memberRepository.save(
                Member.builder()
                        .email("user3@test.com")
                        .name("사용자3")
                        .nickname("사용자3")
                        .phoneNumber("01011111113")
                        .status(MemberStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Funding funding = Funding.create(
                member1,
                "채팅 테스트용 펀딩",
                "테스트",
                LocalDate.now().plusDays(7),
                BusType.BUS_25,
                3,
                300000,
                TripType.ONE_WAY
        );

        fundingRepository.save(funding);
    }
}