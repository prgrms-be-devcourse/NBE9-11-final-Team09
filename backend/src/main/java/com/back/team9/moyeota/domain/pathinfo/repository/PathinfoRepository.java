package com.back.team9.moyeota.domain.pathinfo.repository;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PathinfoRepository extends JpaRepository<Pathinfo, Long> {
    // 특정 펀딩의 모든 노선 조회
    List<Pathinfo> findByFunding_FundingId(Long fundingId);

    // 특정 펀딩의 특정 방향 노선 조회
    // 펀딩 수정 시 기존 가는/오는 노선 찾는데 사용
    Optional<Pathinfo> findByFunding_FundingIdAndDirection(
            Long fundingId,
            Direction direction
    );

    // 특정 펀딩의 특정 방향 노선 중 특정 상태 제외
    // 현재 미사용
    Optional<Pathinfo> findByFunding_FundingIdAndDirectionAndStatusNot(
            Long fundingId,
            Direction direction,
            PathinfoStatus status
    );

    // 여러 펀딩의 특정 방향 노선 일괄 조회
    // 펀딩 목록 조회에서 노선 정보 보여주기 위해 가는 노선만 가져오는데 사용
    // 취소/실패 펀딩 조회 고려해 모든 상태 조회
    List<Pathinfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    );

    // 특정 펀딩의 노선 목록 조회
    // 상세조회에서 유효한 노선만 보여주는데 사용
    List<Pathinfo> findByFunding_FundingIdAndStatusNot(
            Long fundingId,
            PathinfoStatus status
    );
}
