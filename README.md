![img.png](img.png)

## 세모 (SEMO)

### 소개
세모는 “틀린 문제를 다시 맞히는 경험”에서 출발한 이름입니다.  
문제를 틀리면 보통 ‘오답’으로 남겨두지만, 세모는 그 문제를 다시 꺼내 정답으로 바꾸는 과정에 집중합니다.  
틀린 문제를 다시 맞히는 순간, 비어 있던 세모가 채워지듯 학습도 완성됩니다.  
세모는 오답을 기록하는 곳이 아니라, 오답을 정리하고 끝내는 공간입니다.

### 파이프라인
`사진 업로드 → OCR → AI 분류 → 단원/유형 자동 추천 → 최소 수정 → 최종 등록`

수학 문제 사진 1장 업로드를 시작으로 OCR/AI 분석을 거쳐 단원·유형을 자동 추천(preselect)하고,  
사용자가 최소 수정만으로 문제 등록을 완료하는 파이프라인을 구성했습니다.  
외부 API 호출이 포함되어 중복 처리, 무한 재시도, 비용 증가를 제어할 수 있도록 운영 안정성 로직을 포함했습니다.

### 주요 기능

#### 1) ProblemScan 파이프라인
- 사진 업로드 시 `problem_scan` 작업카드 생성, 원본 이미지를 `Asset(ORIGINAL)`로 S3 저장
- 비동기 OCR 수행 후 결과 저장 및 상태 `OCR_DONE` 전환
- 비동기 AI 분류(단원/유형) 수행 후 결과 저장 및 상태 `AI_DONE` 전환
- 추천 결과 기반으로 사용자가 최소 수정 후 최종 등록

#### 2) 운영 안정성
- 중복 처리 방지(락): `locked_at / lock_owner / lock_token`
- 재시도/백오프: `ocr_attempt_count / ai_attempt_count / next_retry_at`
- 상태 전이 제한: `UPLOADED → OCR_DONE → AI_DONE` 또는 `FAILED`
- 원문 저장: `ocr_raw_json`(OCR), `ai_draft_json`(AI)

#### 3) 이미지/자산 관리
- Asset 테이블 SSOT: 원본/보정/크롭 자산 일원화
- `storage_key` 규칙화 및 Presigned URL 기반 조회

### OCR / AI
- OCR: Mathpix (수식/LaTeX 변환 중심)
- AI: Gemini (단원/유형 분류 및 구조화된 결과(JSON) 생성)

### Architecture
- Hexagonal Architecture (Ports & Adapters)
    - S3/OCR/AI 등 외부 시스템은 Port/Adapter로 분리
    - Controller는 요청 처리/유스케이스 호출/DTO 반환 중심으로 유지
    - 검증/가공/외부 호출/후처리는 Validator/Mapper/Client/PostProcessor로 분리

### Tech Stack
- Java 17 (Toolchain)
- Spring Boot 3.5.7
- Gradle Plugins
    - `org.springframework.boot` 3.5.7
    - `io.spring.dependency-management` 1.1.7
    - `com.diffplug.spotless` 6.22.0
- AWS S3 (Asset + Presigned URL)
- JPA (Persistence)
- Scheduler 기반 워커 (락/재시도/백오프 포함)
