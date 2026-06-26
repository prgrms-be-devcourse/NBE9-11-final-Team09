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
                @Tag(name = "Member Auth"),
                @Tag(name = "Member Profile"),
                @Tag(name = "Member History"),
                @Tag(name = "Funding"),
                @Tag(name = "Participation"),
                @Tag(name = "Seat"),
                @Tag(name = "Payment"),
                @Tag(name = "Settlement"),
                @Tag(name = "ChatRoom"),
                @Tag(name = "Notification"),
                @Tag(name = "Admin Auth"),
                @Tag(name = "Admin Member"),
                @Tag(name = "Admin Funding"),
                @Tag(name = "Admin Payment"),
                @Tag(name = "Admin Settlement"),
                @Tag(name = "Admin Settlement Query"),
                @Tag(name = "Admin Statistics")
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
