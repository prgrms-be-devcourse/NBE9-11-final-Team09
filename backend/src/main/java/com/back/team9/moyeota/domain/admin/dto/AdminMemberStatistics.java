package com.back.team9.moyeota.domain.admin.dto;

public record AdminMemberStatistics(
        Long totalUsers,
        Long activeUsers,
        Long withdrawnUsers
) {
}