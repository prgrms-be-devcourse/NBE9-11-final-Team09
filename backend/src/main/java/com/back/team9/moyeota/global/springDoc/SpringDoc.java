package com.back.team9.moyeota.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "모여타(Moyeota) REST API",
                version = "1.0.0",
                description = """
                        **모여타** — 버스 대절 인원 모집 및 결제 플랫폼 REST API 문서입니다.

                        ### 주요 기능
                        - **펀딩**: 버스 대절 모집 생성·조회·수정·취소
                        - **참여 & 좌석**: Redis 기반 5분 선점 후 보증금·잔액 2단계 결제
                        - **결제**: Toss Payments 연동 (보증금 DEPOSIT / 잔액 BALANCE / 환불)
                        - **정산**: 방장 페이백 — 수수료 차감 후 관리자 승인·지급
                        - **실시간**: WebSocket 채팅 + SSE 알림

                        ### 인증
                        우측 상단 **Authorize** 버튼에서 `Bearer {accessToken}` 형식으로 입력 후 사용하세요.
                        """
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "로컬 개발 서버"),
                @Server(url = "https://api.moyeota.com", description = "운영 서버")
        },
        tags = {
                @Tag(name = "Member Auth",            description = "회원 이메일 인증·로그인·소셜 로그인 API"),
                @Tag(name = "Member Profile",         description = "회원 프로필 조회·수정·탈퇴 API"),
                @Tag(name = "Member History",         description = "회원 참여·모집·결제 내역 페이징 조회 API"),
                @Tag(name = "Funding",                description = "버스 대절 펀딩 모집 생성·조회·수정·취소 API"),
                @Tag(name = "Participation",          description = "펀딩 참여 신청·취소 및 참여자 목록 조회 API"),
                @Tag(name = "Seat",                   description = "버스 좌석 배치도 조회 및 Redis 기반 5분 선점 API"),
                @Tag(name = "Payment",                description = "Toss Payments 연동 결제(보증금·잔액·환불) API"),
                @Tag(name = "Settlement",             description = "방장 페이백 정산 요청·조회 API"),
                @Tag(name = "ChatRoom",               description = "WebSocket 기반 실시간 채팅방 REST API"),
                @Tag(name = "Notification",           description = "실시간 이벤트 알림 조회 API"),
                @Tag(name = "Admin Auth",             description = "관리자 로그인·로그아웃 API"),
                @Tag(name = "Admin Member",           description = "관리자 회원 조회·강제 탈퇴 API"),
                @Tag(name = "Admin Funding",          description = "관리자 펀딩 조회·강제 취소 API"),
                @Tag(name = "Admin Payment",          description = "관리자 환불 재처리 API"),
                @Tag(name = "Admin Settlement",       description = "관리자 페이백 승인·거절 API"),
                @Tag(name = "Admin Settlement Query", description = "관리자 정산 내역 조회 API"),
                @Tag(name = "Admin Statistics",       description = "관리자 서비스 현황 통계 API")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SpringDoc {

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("Moyeota API v1")
                .pathsToMatch("/api/**")
                .build();
    }
}
