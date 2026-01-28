![img.png](img.png)

# 세모(SEMO) Backend

세모는 “틀린 문제를 다시 맞히는 경험”에서 출발한 이름입니다.
문제를 틀리면 보통 ‘오답’으로 남겨두지만, 세모는 그 문제를 다시 꺼내 정답으로 바꾸는 과정에 집중합니다.
틀린 문제를 다시 맞히는 순간, 비어 있던 세모가 채워지듯 학습도 완성됩니다.

이 문서는 프로젝트의 목적/구성/설계 의도를 요약하고,
로컬 실행 및 운영 관점(runbook, troubleshooting)을 함께 제공합니다.

## Overview

세모는 수학 문제 사진을 업로드하면 OCR/AI 분석을 통해 단원·유형을 추천하고,
사용자는 최소 수정만으로 오답카드를 생성/관리할 수 있는 서비스입니다.

### 개선 과정

- [페이지 최적화](https://recondite-bear-a3b.notion.site/2f65b82d76f480b9afa5f2b5f89a642f?source=copy_link)
- [비동기 파이프라인 개선](https://recondite-bear-a3b.notion.site/2f65b82d76f4803db2b5e124a14e1bfe?source=copy_link)
- [OCR 선택 과정](https://recondite-bear-a3b.notion.site/OCR-2e15b82d76f4805e84a5c9764f14fd3f?source=copy_link)

### 테스트
- 단위 테스트: JUnit5, Mockito

**커버리지는 60% 이상을 목표로 하며, 주요 도메인/서비스에 집중합니다.**
현재 커버리지 65% 이상을 유지하고 있습니다.

## 설계

### 1) 외부 API 기반 파이프라인의 운영 안정성

OCR/AI는 외부 호출 비용과 실패 확률이 높아서, 단순 비동기 처리만으로는 운영이 어렵습니다.
그래서 아래와 같은 안정성 장치를 포함합니다.

- 중복 처리 방지: DB 락(`locked_at / lock_owner / lock_token`)
- 재시도/백오프: `*_attempt_count`, `next_retry_at`
- 429(레이트리밋)은 실패가 아니라 대기 성격이라 별도 상한/딜레이 정책

### 2) 에러/로그 표준화

- 모든 응답은 `ApiResponse`로 래핑하고, 비즈니스 에러는 `ErrorCode`로 표준화합니다.
- 외부 OAuth 장애는 “토큰 교환/프로필 조회/JWK 로딩” 단위로 ErrorCode를 분리해 로그에서 원인 구분이 가능하도록 했습니다.
- 모든 요청/워커 배치에는 `X-Trace-Id`를 부여해 로그 상관관계를 추적할 수 있습니다.

### 3) 인증/토큰 처리

- JWT access/refresh 분리
- refresh 토큰은 해시로 저장하고, 재발급(rotate) 시 검증/교체 흐름을 포함합니다.
- access 토큰은 JTI 기반 블랙리스트(옵션) 흐름을 포함합니다.

## 다음 개선 아이디어

- Spotless formatter 설정 파일(`tools/naver-eclipse-formatter.xml`)을 레포에 포함하거나, 대체 포맷터로 전환해 온보딩 비용을 줄이는 방향
- DB 스키마 마이그레이션을 `ddl-auto: update`에서 Flyway/Liquibase로 전환

## 파이프라인

`사진 업로드 → OCR → AI 분류 → 단원/유형 자동 추천 → 최소 수정 → 최종 등록`

수학 문제 사진 업로드를 시작으로 OCR/AI 분석을 거쳐 단원·유형을 자동 추천(preselect)하고,
사용자가 최소 수정만으로 문제 등록을 완료하는 파이프라인을 구성했습니다.
외부 API 호출이 포함되어 중복 처리, 무한 재시도, 비용 증가를 제어할 수 있도록 운영 안정성 로직을 포함했습니다.

## 주요 기능

### 1) ProblemScan 파이프라인

- 사진 업로드 시 `problem_scan` 작업카드 생성, 원본 이미지를 `Asset(ORIGINAL)`로 S3 저장
- 비동기 OCR 수행 후 결과 저장 및 상태 `OCR_DONE` 전환
- 비동기 AI 분류(단원/유형) 수행 후 결과 저장 및 상태 `AI_DONE` 전환
- 추천 결과 기반으로 사용자가 최소 수정 후 최종 등록

### 2) 운영 안정성

- 중복 처리 방지(락): `locked_at / lock_owner / lock_token`
- 재시도/백오프: `ocr_attempt_count / ai_attempt_count / next_retry_at`
- 상태 전이 제한: `UPLOADED → OCR_DONE → AI_DONE` 또는 `FAILED`
- 원문 저장: `ocr_raw_json`(OCR), `ai_draft_json`(AI)

### 3) 이미지/자산 관리

- Asset 테이블 SSOT: 원본/보정/크롭 자산 일원화
- `storage_key` 규칙화 및 Presigned URL 기반 조회

## Tech Stack

- Java 17
- Spring Boot 3.5.x
- Gradle (Wrapper)
- MySQL, Redis
- JPA + QueryDSL
- Spring Security + JWT
- Swagger (springdoc-openapi)
- AWS S3 (Asset + Presigned URL)
- 외부 연동: Mathpix(OCR), Gemini(AI), Kakao/Apple OAuth

## Architecture

- Hexagonal Architecture (Ports & Adapters)
  - 외부 시스템(S3/OCR/AI/OAuth)은 `port/out` + `adapter/out`로 분리
  - Controller는 요청 바인딩/검증/유스케이스 호출 중심으로 유지
  - 검증/가공/외부 호출/후처리는 Validator/Mapper/Client/PostProcessor로 분리

패키지 레이아웃(예시):

```
src/main/java/cmc/delta
  domain/<feature>
    adapter/in
    adapter/out
    application/port/in
    application/port/out
    application/service
    application/validation
    model
  global
    config
    error
    logging
    storage
```

## API 문서 (Swagger)

로컬 실행 후:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Quick Start (Local)

### 준비물

- JDK 17
- MySQL
- Redis

### 환경변수

이 프로젝트는 `spring-dotenv`를 사용하며, 루트의 `.env`를 읽어 Spring 설정에 주입합니다.

자주 헷갈리는 포인트:

- `application-local.yml`은 DB를 `LOCAL_DB_*`로 읽습니다. `.env.example`의 `DB_URL/DB_USERNAME/DB_PASSWORD`만 채우면 로컬이 안 뜰 수 있습니다.
- Apple private key는 env에서 `\n` 형태로 들어올 수 있고, 코드에서 개행 복구 로직이 있습니다.

1) `.env.example`를 복사해서 `.env` 생성

2) 로컬 프로필은 `application-local.yml` 기준으로 아래 값을 사용합니다.

- DB (local): `LOCAL_DB_URL`, `LOCAL_DB_USERNAME`, `LOCAL_DB_PASSWORD`
- DB (prod): `PROD_DB_URL`, `PROD_DB_USERNAME`, `PROD_DB_PASSWORD`
- 공통: `JWT_SECRET_BASE64`, `REDIS_PASSWORD`, `S3_*`, `AWS_*`, `MATHPIX_*`, `GEMINI_API_KEY`
- OAuth: `KAKAO_*`, `APPLE_*`

### 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

엔트리 포인트: `src/main/java/cmc/delta/DeltaApplication.java`

헬스 체크:

- `GET /health` (커스텀, 인증 없이 접근 가능)
- `GET /actuator/health` (인증 없이 접근 가능)

## Test / Build

```bash
./gradlew test
```

```bash
./gradlew clean build
```

단일 테스트 실행:

```bash
./gradlew test --tests "cmc.delta.domain.user.application.service.UserServiceImplTest"
```

```bash
./gradlew clean bootJar
```

## Docker

```bash
docker build -t delta .
docker run --rm -p 8080:8080 --env-file .env -e SPRING_PROFILES_ACTIVE=prod delta
```

## 운영 관점 (Runbook)

### 1) TraceId / 로그

- 모든 요청에 `X-Trace-Id`를 부여하고, 응답 헤더로도 내려줍니다.
  - 클라이언트가 `X-Trace-Id`를 보내면 그대로 사용하고, 없으면 서버에서 생성합니다.
- 에러 로그는 `traceId`, `errorCode`, `exType`, `exMsg`를 함께 남깁니다.

### 2) 에러 처리 정책

- 비즈니스/도메인 에러는 `BusinessException` + `ErrorCode`로 통일합니다.
- 5xx(ErrorCode.status가 5xx)인 경우, 클라이언트에는 상세 메시지/데이터를 숨기고 기본 메시지를 내려줍니다.

### 3) ProblemScan 워커 안정성(락/재시도)

- 중복 처리 방지: DB 기반 락(`locked_at / lock_owner / lock_token`)
- 재시도/백오프: `ocr_attempt_count / ai_attempt_count / next_retry_at`
- AI는 429(레이트리밋) 케이스를 별도 카운트/상한으로 더 길게 허용합니다.
- 튜닝 포인트는 `application-*.yml`의 `worker.ocr.*`, `worker.ai.*` 값을 사용합니다.

### 4) 외부 연동(OAuth/OCR/AI) 실패 시 관점

- provider 4xx(잘못된 code/토큰 등): 인증 실패로 간주
- provider 5xx/timeout: 외부 시스템 장애로 간주(운영 알림/재시도 대상)

## Lint / Format (Spotless)

Spotless가 설정되어 있고, `check`가 `spotlessCheck`를 포함합니다.
다만 현재 레포는 포맷 위반이 누적되어 있을 수 있고, formatter 설정 파일(`tools/naver-eclipse-formatter.xml`)이
개인 로컬에 없으면 실패할 수 있습니다.

- 포맷 적용: `./gradlew spotlessApply`
- 포맷 검사: `./gradlew spotlessCheck`
