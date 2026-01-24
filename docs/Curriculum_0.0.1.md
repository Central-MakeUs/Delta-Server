# Curriculum (Curriculum/Units/Types)

> 최종 수정일: 2026.01.25

---

## 기본 설명

- Curriculum 도메인은 단원(Unit)과 유형(ProblemType)을 관리한다.
- 현재(0.0.1) 기준으로 **별도 public API 컨트롤러는 준비중**이며, Problem/ProblemScan 내부에서 참조된다.

관련 코드

- 도메인 모델
  - `src/main/java/cmc/delta/domain/curriculum/model/Unit.java`
  - `src/main/java/cmc/delta/domain/curriculum/model/ProblemType.java`
  - `src/main/java/cmc/delta/domain/curriculum/model/UnitTypeMap.java`

- JPA Repository
  - `src/main/java/cmc/delta/domain/curriculum/adapter/out/persistence/jpa/UnitJpaRepository.java`
  - `src/main/java/cmc/delta/domain/curriculum/adapter/out/persistence/jpa/ProblemTypeJpaRepository.java`

- (현재 스텁) 컨트롤러/유스케이스
  - `src/main/java/cmc/delta/domain/curriculum/adapter/in/web/CurriculumController.java`
  - `src/main/java/cmc/delta/domain/curriculum/application/service/CurriculumQueryService.java`

---

## 데이터 모델 요약

## Unit

- `id`: String (예: S1/U1)
- `name`: String
- `parent`: Unit (null이면 과목/대단원 루트)
- `sortOrder`: int
- `active`: boolean

Unit 트리는 `parent is null`이 루트(과목/대단원)이며, `parent != null`이 하위 단원이다.

## ProblemType

- `id`: String (예: T1)
- `name`: String
- `sortOrder`: int
- `active`: boolean
- `custom`: boolean
- `createdByUser`: User (custom=true인 경우)

`ProblemTypeJpaRepository.findAllActiveForUser(userId)`는

- 시스템 유형(custom=false) + 사용자 커스텀 유형(custom=true, createdByUser=userId)

을 합쳐서 정렬된 목록을 반환한다.

---

## Problem 도메인에서의 사용 지점

- 스캔 요약/상세에서 과목(subject)을 계산할 때 Unit 트리를 참조한다.
  - `src/main/java/cmc/delta/domain/problem/application/support/query/UnitSubjectResolver.java`
- AI 분류 프롬프트 생성 시 Unit/ProblemType 옵션을 내려준다.
  - `src/main/java/cmc/delta/domain/problem/adapter/in/worker/support/prompt/AiCurriculumPromptBuilder.java`
