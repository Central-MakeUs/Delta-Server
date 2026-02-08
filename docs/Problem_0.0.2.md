# Problem API

`Problem Base URL`: `/api/v1/problems`
`ProblemScan Base URL`: `/api/v1/problem-scans`
`ProblemStats Base URL`: `/api/v1/problems/stats`

인증: `Authorization: Bearer {accessToken}`

담당: Problem 파트

최종 수정일: 2026-01-25 (초안 갱신)

---

## 개요

1. 사진 업로드 → `POST /api/v1/problem-scans` (scan 생성 + Asset 저장)
2. 서버 비동기 워커: OCR → AI 분류
3. 프론트는 `GET /api/v1/problem-scans/{scanId}` 폴링
4. `AI_DONE`이면 `POST /api/v1/problems`로 최종 오답카드 생성

---

## 관련 코드

- Scan 컨트롤러: `src/main/java/cmc/delta/domain/problem/adapter/in/web/scan/ProblemScanController.java`
- Problem 컨트롤러: `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemController.java`
- Stats 컨트롤러: `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemStatsController.java`

---

## 공통 Response 형식

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {},
  "message": "..."
}
```

---

## ProblemScan

### `POST /api/v1/problem-scans`

- 설명: 문제 스캔 생성(업로드 + scan/asset 생성)
- 인증: Required
- Content-Type: `multipart/form-data`
- Multipart

```text
Key: file (file) - required
```

- Response (200)

```json
{
  "status":200,
  "code":"SUC_...",
  "data":{
    "scanId":123,
    "assetId":10,
    "status":"UPLOADED"
  },
  "message":"..."
}
```

- 오류
  - `400 BAD_REQUEST` — 파일 누락/형식 오류
  - `413 PAYLOAD_TOO_LARGE` — 파일 크기 초과
  - `500 INTERNAL_ERROR` — 저장/처리 에러

#### curl 예시

```bash
curl -X POST "https://api.example.com/api/v1/problem-scans" \
  -H "Authorization: Bearer {accessToken}" \
  -F "file=@/path/to/image.jpg" -i
```

---

### `GET /api/v1/problem-scans/{scanId}`

- 설명: 스캔 상세 조회(원본 이미지 URL + OCR/AI 상태)
- 인증: Required
- Path params: `scanId`: Long
- Response (200) — 예시

```json
{
  "status":200,
  "code":"SUC_...",
  "data":{
    "scanId":123,
    "status":"AI_DONE",
    "originalImage":{
      "assetId":10,
      "viewUrl":"https://...presigned...",
      "width":1170,
      "height":2532
    },
    "ocrPlainText":"...",
    "aiProblemLatex":"...",
    "aiSolutionLatex":"...",
    "ai":{
      "subjectId":"S1",
      "subjectName":"...",
      "unitId":"U1",
      "unitName":"...",
      "typeId":"T1",
      "typeName":"...",
      "confidence":0.87,
      "needsReview":false
    }
  },
  "message":"..."
}
```

- 오류
  - `404 NOT_FOUND` — 해당 scan 없음
  - `401 UNAUTHORIZED`

---

### `GET /api/v1/problem-scans/{scanId}/summary`

- 설명: 앱용 요약(이미지 + 과목/단원/유형)
- 인증: Required
- Response (200) — 예시 요약 포함

---

## Problem (오답카드)

### `POST /api/v1/problems`

- 설명: scan 기반 최종 오답카드 생성
- 인증: Required
- Request (application/json)

```json
{
  "scanId":123,
  "finalUnitId":"U1",
  "finalTypeIds":["T1"],
  "answerFormat":"TEXT",
  "answerChoiceNo":null,
  "answerValue":"ans",
  "memoText":"sol"
}
```

- Response (200)

```json
{
  "status":200,
  "code":"SUC_...",
  "data":{
    "problemId":777,
    "scanId":123
  },
  "message":"..."
}
```

- 오류
  - `400 BAD_REQUEST` — 필드 오류
  - `404 NOT_FOUND` — scanId 미존재
  - `409 CONFLICT` — 이미 등록된 문제
  - `401 UNAUTHORIZED`

#### curl 예시

```bash
curl -X POST "https://api.example.com/api/v1/problems" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
-d '{"scanId":123,"finalUnitId":"U1","finalTypeIds":["T1"],"answerFormat":"TEXT","answerValue":"ans","memoText":"sol"}' -i
```

---

### `GET /api/v1/problems`

- 설명: 내 오답카드 목록 조회
- 인증: Required
- Query: `subjectId`, `unitId`, `typeId`, `sort`, `status`, `page`, `size`
- Response: `ApiResponse<Page<MyProblemSummary>>`

---

### `GET /api/v1/problems/{problemId}`

- 설명: 내 오답카드 상세 조회
- 인증: Required
- Path: `problemId`: Long
- Response: `ApiResponse<MyProblemDetail>`
- 오류: 404 (본인 소유 아님은 404 처리 권장), 401

---

### `PATCH /api/v1/problems/{problemId}`

- 설명: 오답카드 정답/풀이 수정
- 인증: Required
- Request (partial)

```json
{
  "answerChoiceNo":3,
  "answerValue":null,
  "memoText":"sol"
}
```

- Response: `ApiResponse<Void>`
- 오류: 400, 401, 404

---

### `POST /api/v1/problems/{problemId}/complete`

- 설명: 오답 완료 처리
- 인증: Required
- Request

```json
{
  "memoText":"sol"
}
```

- Response: `ApiResponse<Void>`
- 오류: 400, 401, 404

---

### `DELETE /api/v1/problems/{problemId}`

- 설명: 내 오답카드 삭제
- 인증: Required
- Path: `problemId`: Long
- Response: `ApiResponse<Void>`
- 오류: 401, 404

---

## ProblemStats

- `GET /api/v1/problems/stats/units` — 단원별 오답 통계
- `GET /api/v1/problems/stats/types` — 유형별 오답 통계
- 인증: Required

---

## 에러코드(대표)

- `VALIDATION_ERROR` (400)
- `NOT_FOUND` / `SCAN_NOT_FOUND` / `PROBLEM_NOT_FOUND` (404)
- `AUTH_UNAUTHORIZED` (401)
- `CONFLICT_DUPLICATE` (409)
- `PROCESSING_FAILED` (500)
