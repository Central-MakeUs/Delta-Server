# Curriculum Domain API

관련 코드:
- `src/main/java/cmc/delta/domain/curriculum/adapter/in/web/type/ProblemTypeController.java`
- `src/main/java/cmc/delta/domain/curriculum/adapter/in/web/CurriculumController.java`

## Problem Type

Base URL: `/api/v1/problem-types`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| PTYPE-01 | GET | `/api/v1/problem-types` | Required | 내 유형 목록 조회(기본+커스텀) | query `includeInactive` | `ApiResponse<ProblemTypeListResponse>` |
| PTYPE-02 | POST | `/api/v1/problem-types` | Required | 커스텀 유형 추가 | `ProblemTypeCreateRequest` (JSON) | `ApiResponse<ProblemTypeItemResponse>` |
| PTYPE-03 | PATCH | `/api/v1/problem-types/{typeId}` | Required | 커스텀 유형 수정 | Path `typeId`, `ProblemTypeUpdateRequest` | `ApiResponse<ProblemTypeItemResponse>` |
| PTYPE-04 | PATCH | `/api/v1/problem-types/{typeId}/activation` | Required | 커스텀 유형 활성/비활성 | Path `typeId`, `ProblemTypeActivationRequest` | `ApiResponse<Void>` |

## Curriculum Controller 상태

- `CurriculumController`는 현재 외부 노출 엔드포인트가 없습니다.
