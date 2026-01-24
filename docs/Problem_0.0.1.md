# Problem API

> Problem Base URL: `/api/v1/problems`
>
> ProblemScan Base URL: `/api/v1/problem-scans`
>
> ProblemStats Base URL: `/api/v1/problems/stats`
>
> 인증: `Authorization: Bearer {accessToken}`
>
> 최종 수정일: 2026.01.25

관련 코드

- 스캔: `src/main/java/cmc/delta/domain/problem/adapter/in/web/scan/ProblemScanController.java`
- 오답카드: `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemController.java`
- 통계: `src/main/java/cmc/delta/domain/problem/adapter/in/web/problem/ProblemStatsController.java`

---

## 처리 흐름 (현재 구현 기준)

1. 사진 업로드 → `POST /api/v1/problem-scans`
2. 서버는 `ProblemScan` 생성 + 원본 `Asset(ORIGINAL)` 저장
3. 워커 비동기 처리
  - OCR 워커: `UPLOADED → OCR_DONE` 또는 `FAILED`
  - AI 워커: `OCR_DONE → AI_DONE` 또는 `FAILED`
4. 프론트는 `GET /api/v1/problem-scans/{scanId}` 폴링
5. `AI_DONE`이면 `POST /api/v1/problems`로 최종 오답카드 생성

---

## Scan Status

| Status | 의미 |
| --- | --- |
| UPLOADED | 업로드 완료(OCR 대기) |
| OCR_DONE | OCR 완료(AI 대기) |
| AI_DONE | AI 분류/초안 완료 |
| FAILED | 처리 실패 |

failReason은 `FailureReason` 코드 문자열을 따른다.

- `src/main/java/cmc/delta/domain/problem/adapter/in/worker/support/failure/FailureReason.java`

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

## `POST` /api/v1/problem-scans

- 설명: 문제 스캔 생성(업로드 + scan/asset 생성)
- Content-Type: `multipart/form-data`

Multipart

| Key | Type | Required |
| --- | --- | --- |
| file | file | O |

Response (200)

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {
    "scanId": 123,
    "assetId": 10,
    "status": "UPLOADED"
  },
  "message": "..."
}
```

---

## `GET` /api/v1/problem-scans/{scanId}

- 설명: 스캔 상세 조회(원본 이미지 URL + OCR/AI 상태)

Response (200, 예시)

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {
    "scanId": 123,
    "status": "AI_DONE",
    "hasFigure": false,
    "renderMode": "LATEX",
    "originalImage": {
      "assetId": 10,
      "viewUrl": "https://...presigned...",
      "width": 1170,
      "height": 2532
    },
    "ocrPlainText": "...",
    "aiProblemLatex": "...",
    "aiSolutionLatex": "...",
    "ai": {
      "subjectId": "S1",
      "subjectName": "...",
      "unitId": "U1",
      "unitName": "...",
      "typeId": "T1",
      "typeName": "...",
      "confidence": 0.87,
      "needsReview": false,
      "predictedTypes": [],
      "unitCandidatesJson": "...",
      "typeCandidatesJson": "...",
      "aiDraftJson": "..."
    },
    "createdAt": "2026-01-15T21:10:00",
    "ocrCompletedAt": "2026-01-15T21:10:10",
    "aiCompletedAt": "2026-01-15T21:10:30",
    "failReason": null
  },
  "message": "..."
}
```

---

## `GET` /api/v1/problem-scans/{scanId}/summary

- 설명: 앱용 요약(이미지 + 과목/단원/유형)

Response (200)

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {
    "scanId": 123,
    "status": "AI_DONE",
    "originalImage": {
      "assetId": 10,
      "viewUrl": "https://...presigned..."
    },
    "classification": {
      "subject": { "id": "S1", "name": "..." },
      "unit": { "id": "U1", "name": "..." },
      "types": [],
      "needsReview": false
    }
  },
  "message": "..."
}
```

---

## Problem (오답카드)

## `POST` /api/v1/problems

- 설명: scan 기반 최종 오답카드 생성

Request

```json
{
  "scanId": 123,
  "finalUnitId": "U1",
  "finalTypeIds": ["T1"],
  "answerFormat": "TEXT",
  "answerChoiceNo": null,
  "answerValue": "ans",
  "solutionText": "sol"
}
```

Response (200)

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {
    "problemId": 777,
    "scanId": 123
  },
  "message": "..."
}
```

---

## `GET` /api/v1/problems

- 설명: 내 오답 카드 목록 조회
- Query: `MyProblemListRequest` (subjectId/unitId/typeId/sort/status/page/size)

---

## `GET` /api/v1/problems/{problemId}

- 설명: 내 오답카드 상세 조회

---

## `PATCH` /api/v1/problems/{problemId}

- 설명: 오답카드 정답/풀이 수정

Request

```json
{
  "answerChoiceNo": 3,
  "answerValue": null,
  "solutionText": "sol"
}
```

---

## `POST` /api/v1/problems/{problemId}/complete

- 설명: 오답 완료 처리

Request

```json
{
  "solutionText": "sol"
}
```

---

## ProblemStats

## `GET` /api/v1/problems/stats/units

- 설명: 단원별 오답 통계(완료/미완료)

## `GET` /api/v1/problems/stats/types

- 설명: 유형별 오답 통계(완료/미완료)
