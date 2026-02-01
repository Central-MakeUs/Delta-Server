# Curriculum (단원/유형)

담당: Curriculum 파트

최종 수정일: 2026-01-25 (초안 갱신)

---

## 개요

- 단원(Unit)과 유형(ProblemType)을 관리합니다.
- 현재(0.0.1) 기준으로 공개 API는 일부 스텁이며 내부 참조용으로 사용됩니다.

## 관련 코드

- 모델: `src/main/java/cmc/delta/domain/curriculum/model/Unit.java`, `ProblemType.java`
- JPA: `.../adapter/out/persistence/jpa/UnitJpaRepository.java`
- 컨트롤러(스텁): `src/main/java/cmc/delta/domain/curriculum/adapter/in/web/CurriculumController.java`

---

## 모델 요약

- `Unit`
  - `id`: String (예: `S1/U1`)
  - `name`: String
  - `parent`: Unit | null
  - `sortOrder`: int
  - `active`: boolean
- `ProblemType`
  - `id`: String (예: `T1`)
  - `name`: String
  - `sortOrder`: int
  - `active`: boolean
  - `custom`: boolean
  - `createdByUser`: User (custom=true인 경우)

---

## 참고 엔드포인트

> 현재 구현은 내부 사용 중심이므로 외부에 노출할 API는 스펙 정리가 필요합니다. 예시(제안):

- `GET /api/v1/curriculum/units` — 전체 단원 트리 반환
- `GET /api/v1/curriculum/types` — 유형 목록(시스템+사용자 커스텀)

응답 예시(간략)

```json
{
  "status":200,
  "code":"SUC_...",
  "data":[ { "id":"S1", "name":"중학교 수학", "children":[...]} ]
}
```

---

## 사용 지점

- `UnitSubjectResolver` (scan에서 과목/단원 계산)
- `AiCurriculumPromptBuilder` (AI 프롬프트 생성 시 옵션 제공)

---

## 향후 권장사항

- 공개 API로 노출 시 캐싱 정책(단원 트리는 자주 바뀌지 않으므로 Cache-First), 권한(읽기는 public, 쓰기는 제한) 명시
