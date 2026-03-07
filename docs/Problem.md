# Problem Domain API

관련 코드:
- `src/main/java/cmc/delta/domain/problem/adapter/in/web/scan/ProblemScanController.java`
- `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemController.java`
- `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemStatsController.java`

## Problem Scan

Base URL: `/api/v1/problem-scans`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| SCAN-01 | POST | `/api/v1/problem-scans` | Required | 문제 스캔 생성 | multipart `file` | `ApiResponse<ProblemScanCreateResponse>` |
| SCAN-02 | GET | `/api/v1/problem-scans/{scanId}` | Required | 문제 스캔 상세 조회 | Path `scanId` | `ApiResponse<ProblemScanDetailResponse>` |
| SCAN-03 | GET | `/api/v1/problem-scans/{scanId}/summary` | Required | 문제 스캔 요약 조회 | Path `scanId` | `ApiResponse<ProblemScanSummaryResponse>` |

## Problem Card

Base URL: `/api/v1/problems`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| PROB-01 | POST | `/api/v1/problems` | Required | 오답카드 생성 | `ProblemCreateRequest` (JSON) | `ApiResponse<ProblemCreateResponse>` |
| PROB-02 | GET | `/api/v1/problems` | Required | 오답카드 목록 조회(페이지) | `MyProblemListRequest` (query) | `ApiResponse<PagedResponse<ProblemListItemResponse>>` |
| PROB-03 | GET | `/api/v1/problems/scroll` | Required | 오답카드 목록 조회(커서) | `MyProblemScrollRequest` (query) | `ApiResponse<CursorPagedResponse<ProblemListItemResponse>>` |
| PROB-04 | POST | `/api/v1/problems/{problemId}/complete` | Required | 오답카드 완료 처리 | Path `problemId`, `ProblemCompleteRequest` | `ApiResponse<Void>` |
| PROB-05 | GET | `/api/v1/problems/{problemId}` | Required | 오답카드 상세 조회 | Path `problemId` | `ApiResponse<ProblemDetailResponse>` |
| PROB-06 | POST | `/api/v1/problems/{problemId}/ai-solution-requests` | Required | AI 풀이 생성 요청 | Path `problemId` | `ApiResponse<ProblemAiSolutionRequestResponse>` |
| PROB-07 | GET | `/api/v1/problems/{problemId}/ai-solution` | Required | AI 풀이 조회 | Path `problemId` | `ApiResponse<ProblemAiSolutionDetailResponse>` |
| PROB-08 | DELETE | `/api/v1/problems/{problemId}/ai-solution` | Required | AI 풀이 삭제 | Path `problemId` | `ApiResponse<Void>` |
| PROB-09 | PATCH | `/api/v1/problems/{problemId}` | Required | 오답카드 정답/풀이 수정 | Path `problemId`, `ProblemUpdateRequest` | `ApiResponse<Void>` |
| PROB-10 | DELETE | `/api/v1/problems/{problemId}` | Required | 오답카드 삭제 | Path `problemId` | `ApiResponse<Void>` |

## Problem Stats

Base URL: `/api/v1/problems/stats`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| PSTAT-01 | GET | `/api/v1/problems/stats/units` | Required | 단원별 오답 통계 | `ProblemStatsRequest` (query) | `ApiResponse<ProblemStatsResponse<ProblemUnitStatsItemResponse>>` |
| PSTAT-02 | GET | `/api/v1/problems/stats/types` | Required | 유형별 오답 통계 | `ProblemStatsRequest` (query) | `ApiResponse<ProblemStatsResponse<ProblemTypeStatsItemResponse>>` |
| PSTAT-03 | GET | `/api/v1/problems/stats/monthly` | Required | 월별 오답 현황 | `ProblemMonthlyProgressRequest` (query) | `ApiResponse<ProblemMonthlyProgressResponse>` |
