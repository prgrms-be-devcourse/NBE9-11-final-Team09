# NBE9-11-final-Team09
데브코스 백엔드 9기 11회차 최종 프로젝트 9팀 레포지토리 입니다.

# 🚌 모여타 (Moyeota)

같은 출발지와 목적지를 가진 사람들이 함께 버스를 대절하고 비용을 나누는 **펀딩 기반 이동 매칭 플랫폼**

---

## 📌 프로젝트 소개

### 문제 인식
- 대형 콘서트·스포츠 경기·축제 등 특정 행사가 종료된 후, 많은 인원이 동시에 이동하며 교통 혼잡 및 이동 비용 증가 문제가 발생합니다.
- 행사와 무관하게 같은 출발지·목적지를 가진 사람들이 이동 수단을 함께 이용하고 싶어도 서로를 연결할 방법이 부족합니다.
  - 단체 버스 대절 정보가 분산되어 있어 개인이 알아보기 번거로움
  - 같은 목적지로 이동하는 사람들을 찾기 어려움
  - 행사 종료 후 교통 혼잡으로 이동 비용과 대기 시간 증가
  - 같은 경로를 이동하는 사람들 간의 연결·모집 플랫폼 부재

### 해결 방향
같은 출발지와 목적지를 가진 사람들을 모집하여 **펀딩 방식으로 전세버스 비용을 분담**하는 플랫폼을 구축합니다.
방장이 이동 그룹(펀딩)을 개설하면 참여자가 보증금을 결제해 좌석을 예약하고, 최소 인원이 달성되면 전세버스 운행이 확정됩니다.

### 개발 방향
- **MVP 기반 개발**: 핵심 기능을 우선 완성하여 일정 내 서비스 제공
- **사용자 중심 기획**: 실제 불편함에서 출발한 기능 설계
- **표준화된 구조**: REST API · ERD · GitHub Branch 전략 등 협업 가능한 문서화
- **지속 가능한 품질 관리**: 테스트 코드 · CI/CD · 코드 리뷰 · 배포 로그 기반

---

## 💡 서비스 이용 흐름

```
펀딩 생성 → 좌석 선택 → 보증금 결제 → 펀딩 확정 → 잔액 결제 → 버스 탑승
```

| 시점 | 이용자 | 방장 |
|---|---|---|
| 펀딩 모집 시작 | 펀딩 참여, 보증금 결제, 좌석 선택 (자유롭게 환불 가능) | 펀딩 오픈, 좌석 선택 |
| 출발 -10일 자정 | 펀딩 여부 확정 연락받음 (미달 시 보증금 전액 환불) | 버스대절업체 컨택 |
| 출발 -7일 자정 | 추가 인원 받은 후 잔액 결제금액 확정 | - |
| 출발 -24시간 | 잔액 결제 (미결제 시 참여 취소, 환불 없음) | 참여확정 인원 정보 전달받음 |
| 출발 D-day | 버스 탑승 | - |
| 출발 +1일 | - | 페이백 |

---

## 👥 팀 구성 및 역할

| 이름 | 담당 도메인 |
|---|---|
| **안수빈** (팀장) | Seat, Participation (참여·좌석 / 동시성 처리) |
| 김지영 | Payment, Settlement (결제·정산 / Toss Payments) |
| 이하민 | Funding, PathInfo, Scheduler / Global 공통 |
| 유승희 | AWS 인프라, Chat, Notification |
| 최민호 | Member, Admin, JWT 인증/인가 |

---

## 🛠 기술 스택

**Backend**
Java 25 · Spring Boot 4.0.6 · Spring Security · Spring Data JPA · MySQL (RDS) · Redis · JWT

**Frontend**
Next.js (App Router) · React · TypeScript · Tailwind CSS

**Infra / DevOps**
AWS EC2 · Docker / Docker Compose · GitHub Actions (CI/CD) · Vercel (Frontend Hosting)

**Monitoring**
Prometheus · Grafana

**External API / Infra**
Kakao Login (OAuth) · Toss Payments · Gmail SMTP (알림 메일 발송)

**Testing**
k6 

---

## 🏗 시스템 아키텍처

```
[User Browser]
      │ HTTPS
      ▼
[Frontend Hosting: Vercel (Next.js / React / TS)]
      │ REST API
      ▼
┌───────────────── AWS Cloud (VPC) ─────────────────┐
│  EC2 / Docker Compose                              │
│  ┌─── Application Runtime ───┐  ┌─ Observability ─┐│
│  │  Spring Boot               │  │  Prometheus     ││
│  │   ├─ Redis                 │──▶  (actuator      ││
│  │   └─ MySQL / RDS           │  │   scrape)       ││
│  └─────────────────────────────┘  └──▶ Grafana ────┘│
└──────────────────────────────────────────────────┘
      │
      ├─ OAuth ────────▶ Kakao Login
      ├─ Payment API ──▶ Toss Payments
      └─ SMTP ─────────▶ Gmail SMTP

[GitHub] ──push/PR──▶ [GitHub Actions] ──build/test/deploy──▶ EC2
```

- Spring Boot는 내부 포트(8081)로 Actuator Metrics를 노출하고, Prometheus가 이를 스크래핑합니다.
- Grafana는 Prometheus를 PromQL로 조회하여 대시보드를 구성합니다.
- 프론트엔드는 Vercel에 별도 배포되며, 백엔드와는 REST API로 통신합니다.

---

## 🔄 CI/CD 파이프라인

```
브랜치 push
   │
   ▼
[Test]
 코드 가져오기 → JDK 25 세팅 → 스프링부트 테스트
   │
   ▼
Test 통과 && dev 브랜치 push
   │
   ▼
[Deploy]
 빌드 → Docker build & push → EC2 SSH 배포 → deploy.sh 실행
```

GitHub Actions를 통해 브랜치 push 시 테스트가 실행되고, `dev` 브랜치에 테스트를 통과한 코드가 반영되면 자동으로 Docker 이미지를 빌드하여 EC2에 배포합니다.

---

## 📁 프로젝트 구조

### 백엔드

```
backend/
├── Dockerfile
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── gradlew / gradlew.bat
├── logs/
└── src/
    ├── main/
    │   ├── java/com/back/team9/moyeota/
    │   │   ├── MoyeotaApplication.java
    │   │   ├── domain/
    │   │   │   ├── admin/            # 관리자 (auth/funding/member/settlement/statistics)
    │   │   │   ├── chatroom/         # 채팅 (ChatRoom, Message)
    │   │   │   ├── funding/          # 펀딩 (생성/타임라인/스케줄러/가격정책)
    │   │   │   ├── member/           # 회원 (인증/이력/프로필, 소셜 로그인)
    │   │   │   ├── notification/     # 알림 (메일 발송 포함)
    │   │   │   ├── participation/    # 참여 (동시성/최종금액/노쇼 스케줄러)
    │   │   │   ├── pathinfo/         # 운행 노선
    │   │   │   ├── payment/          # 결제 (Toss 연동)
    │   │   │   ├── seat/             # 좌석 (Redis 기반 HOLD)
    │   │   │   └── settlement/       # 정산
    │   │   ├── global/
    │   │   │   ├── config/           # Async, Jpa, Security, Scheduler, WebSocket 등
    │   │   │   ├── error / exception # 공통 예외 처리
    │   │   │   ├── filter/           # MDC 로깅 필터
    │   │   │   ├── jwt/              # JWT 발급/검증/블랙리스트
    │   │   │   ├── response/         # 공통 응답 래퍼 (ApiResponse)
    │   │   │   ├── springDoc/        # Swagger 설정
    │   │   │   └── webMvc/
    │   │   └── health/
    │   └── resources/
    │       ├── application.yaml
    │       ├── application-dev.yaml
    │       ├── application-local.yml
    │       ├── application-prod.yml
    │       ├── application-secret.yml
    │       ├── application-test.yaml
    │       ├── logback-spring.xml
    │       └── templates/mail/
    └── test/
```

각 도메인은 `controller / dto / entity / repository / service` (필요 시 `event`, `scheduler`, `validator`, `client` 등) 구조를 따릅니다.

### 프론트엔드

```
frontend/
├── package.json
├── next.config.ts
├── tsconfig.json
└── src/
    ├── app/
    │   ├── (auth)/login, signup
    │   ├── admin/                 # 관리자 대시보드 (funding/member/settlement/overview 패널)
    │   ├── chat/[chatRoomId]/
    │   ├── funding/[id]/seats/
    │   ├── fundings/              # 목록/생성/수정
    │   ├── login/kakao/callback/
    │   ├── mypage/
    │   ├── notification/
    │   ├── payment/               # 보증금/잔액 결제, 성공/실패 페이지
    │   └── settlements/[fundingId]/
    ├── components/
    │   ├── chat/                  # ChatRoom, MessageBubble, MessageInput
    │   ├── seat/                  # SeatMap, SeatButton, SeatInfoPanel
    │   ├── common/, layout/, ui/
    ├── hooks/                     # useChat 등
    ├── lib/                       # 도메인별 API 클라이언트
    └── types/                     # 도메인별 타입 정의
```

---

## 🗄 ERD

핵심 엔터티: `Member`, `Admin`, `Funding`, `Pathinfo`, `Seat`, `Participation`, `Payment`, `Settlement`, `ChatRoom`, `Message`, `Notification`

- 좌석 상태(`Seat.status`)는 DB에는 `AVAILABLE / BOOKED`만 존재하며, `HOLD` 상태는 동시성 제어를 위해 Redis에서만 관리됩니다.
- `Funding.totalPrice`는 버스 대절비에 플랫폼 수수료(10%)가 포함된 금액이며, 1인당 금액은 `totalPrice ÷ 실제 참여 인원` (100원 단위 올림)으로 서버에서 계산됩니다.

*(ERD 상세 다이어그램은 Notion 문서 참고)*
