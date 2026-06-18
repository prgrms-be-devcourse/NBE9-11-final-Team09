package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.dto.history.MemberFundingResponse;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// 회원이 생성한 펀딩과 참여자 수를 조회하는 커스텀 리포지토리
public interface MemberFundingQueryRepository extends JpaRepository<Funding, Long> {

    @Query(
            value = """
                    select new com.back.team9.moyeota.domain.member.dto.MemberFundingResponse(
                        f.fundingId,
                        f.title,
                        f.departureDate,
                        count(p),
                        f.maxParticipants,
                        f.status,
                        f.createdAt
                    )
                    from Funding f
                    left join Participation p
                        on p.funding = f
                        and p.status <> :canceledStatus
                    where f.member.memberId = :memberId
                    group by
                        f.fundingId,
                        f.title,
                        f.departureDate,
                        f.maxParticipants,
                        f.status,
                        f.createdAt
                    """,
            countQuery = """
                    select count(f)
                    from Funding f
                    where f.member.memberId = :memberId
                    """
    )
    Page<MemberFundingResponse> findMyFundings(
            Long memberId,
            ParticipationStatus canceledStatus,
            Pageable pageable
    );
}