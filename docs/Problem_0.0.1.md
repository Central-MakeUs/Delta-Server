# Problem API (v1)

> Base URL: /api/v1/problems
>
>
> 인증: `Authorization: Bearer {accessToken}`
>
> 공통 응답: `status / code / data / message`
>
> 공통 헤더: `X-Trace-Id`
>

---

## 처리 흐름

1. 사용자 사진 업로드 → `POST /scans`
2. 서버는 **ProblemScan(작업카드)** 생성 + **원본 Asset(ORIGINAL)** S3 저장
3. 워커 비동기 처리
    - OCR 워커: `UPLOADED → OCR_DONE` 또는 `FAILED`
    - AI 워커: `OCR_DONE → AI_DONE` 또는 `FAILED`
4. 프론트는 `GET /scans/{scanId}`를 폴링해서 상태 확인
5. `AI_DONE`이면 초안 확인 후 `POST /`로 최종 문제 등록

---

## Scan Status

| Status | 의미 | 비고 |
| --- | --- | --- |
| UPLOADED | 업로드 완료(OCR 대기) | OCR 워커 claim 대상 |
| OCR_DONE | OCR 완료(AI 대기) | AI 워커 claim 대상 |
| AI_DONE | AI 분류/초안 완료 | 사용자 확정 등록 가능 |
| FAILED | 처리 실패(재시도 소진 또는 non-retryable) | failReason 제공 |

`READY` 같은 상태는 사용하지 않는다.

---

## failReason (대표)

### OCR

- `ASSET_NOT_FOUND`
- `SCAN_NOT_FOUND`
- `OCR_RATE_LIMIT`
- `OCR_CLIENT_4XX`
- `OCR_CLIENT_5XX`
- `OCR_NETWORK_ERROR`
- `OCR_FAILED`

### AI

- `OCR_TEXT_EMPTY`
- `SCAN_NOT_FOUND`
- `AI_RATE_LIMIT`
- `AI_CLIENT_4XX`
- `AI_CLIENT_5XX`
- `AI_NETWORK_ERROR`
- `AI_FAILED`

---

## 공통 Response

### 성공

```json
{
    "status":200,
    "code":"SUC_XXX",
    "data":{},
    "message":"..."
}

```

### 실패

```json
{
    "status":401,
    "code":"AUTH_010",
    "data":null,
    "message":"토큰이 필요합니다."
}

```

---

# Endpoints

## 1) POST `/api/v1/problems/scans`

### 개요

문제 사진 1장을 업로드하고 Scan을 생성한다. OCR/AI 분석은 비동기로 진행된다.

### Request

Headers

| Key | Value | Required |
| --- | --- | --- |
| Authorization | Bearer {accessToken} | O |
| Content-Type | multipart/form-data | O |

Multipart

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| image | file | O | 문제 사진 1장 |
| source | string | X | CAMERA / GALLERY |
| clientCrop | string(json) | X | crop 좌표(선택) |

### Response (201)

```json
{
    "status":201,
    "code":"SUC_SCAN_CREATED",
    "data":{
    "scanId":123,
    "status":"UPLOADED",
    "hasFigure":false,
    "renderMode":"LATEX",
    "assets":[
    {
    "assetId":10,
    "assetType":"ORIGINAL",
    "viewUrl":"https://...presigned...",
    "width":1170,
    "height":2532
    }
    ],
    "createdAt":"2026-01-15T21:10:00"
    },
    "message":"스캔이 생성되었습니다."
}

```

---

## 2) GET `/api/v1/problems/scans/{scanId}`

### 개요

Scan 단건 상태 + OCR/AI 결과(초안)를 조회한다.

### Response (200, AI_DONE 예시)

```json
{
    "status":200,
    "code":"SUC_SCAN_DETAIL",
    "data":{
    "scanId":123,
    "status":"AI_DONE",
    "hasFigure":false,
    "renderMode":"LATEX",
    "assets":[
    {
    "assetId":10,
    "assetType":"ORIGINAL",
    "viewUrl":"https://...presigned..."
    }
    ],
    "ocr":{
    "plainText":"....",
    "rawJson":"{...}",
    "attemptCount":1,
    "completedAt":"2026-01-15T21:10:10"
    },
    "ai":{
    "predictedUnit":{"id":"U_GEOM","name":"기하"},
    "predictedType":{"id":"T_GEOM","name":"도형"},
    "confidence":0.87,
    "needsReview":false,
    "unitCandidatesJson":"{...}",
    "typeCandidatesJson":"{...}",
    "draftJson":"{...}",
    "attemptCount":1,
    "completedAt":"2026-01-15T21:10:30"
    },
    "failReason":null,
    "nextRetryAt":null,
    "createdAt":"2026-01-15T21:10:00",
    "updatedAt":"2026-01-15T21:10:30"
    },
    "message":"조회 성공"
}

```

### Response (200, 처리중 예시 OCR_DONE)

```json
{
    "status":200,
    "code":"SUC_SCAN_DETAIL",
    "data":{
    "scanId":123,
    "status":"OCR_DONE",
    "failReason":null,
    "nextRetryAt":null
    },
    "message":"조회 성공"
}

```

### Response (200, FAILED 예시)

```json
{
    "status":200,
    "code":"SUC_SCAN_DETAIL",
    "data":{
    "scanId":123,
    "status":"FAILED",
    "failReason":"OCR_CLIENT_4XX",
    "nextRetryAt":null
    },
    "message":"조회 성공"
}

```

---

## 3) GET `/api/v1/problems/meta/units`

### 개요

Unit 트리(과목/단원)를 조회한다.

### Response (200)

```json
{
    "status":200,
    "code":"SUC_UNITS",
    "data":{
    "units":[
    {"id":"U_ROOT_COMMON","name":"공통수학","parentId":null,"sortOrder":0},
    {"id":"U_ALG_POLY","name":"다항식","parentId":"U_ROOT_COMMON","sortOrder":10}
    ]
    },
    "message":"조회 성공"
}

```

---

## 4) GET `/api/v1/problems/meta/types`

### 개요

ProblemType 목록을 조회한다. 사용자 커스텀 포함.

### Request (optional)

| Name | Type | Required |
| --- | --- | --- |
| unitId | String | X |

### Response (200)

```json
{
    "status":200,
    "code":"SUC_TYPES",
    "data":{
    "types":[
    {"id":"T_POLY","name":"다항식","sortOrder":0,"isSystem":true},
    {"id":"T_USER_1","name":"내가 만든 유형","sortOrder":999,"isSystem":false}
    ]
    },
    "message":"조회 성공"
}

```

---

## 5) POST `/api/v1/problems/meta/types`

### 개요

사용자 유형을 직접 추가한다.

### Request

```json
{"name":"새 유형"}

```

### Response (201)

```json
    {
    "status":201,
    "code":"SUC_TYPE_CREATED",
    "data":{
    "id":"T_USER_999",
    "name":"새 유형",
    "isSystem":false
    },
    "message":"생성 성공"
}

```

---

# 여기부터는 “완성 안 됐지만 이렇게 갈 확률이 높은” 예측 문서

## 6) POST `/api/v1/problems`

### 개요

Scan 결과를 바탕으로 최종 Problem을 생성한다.

사용자가 단원/유형/정답/본문을 확정한다.

### 정책(예측)

- `scanId`는 반드시 본인 소유
- `scan.status`는 **AI_DONE 이어야 함** (또는 OCR_DONE에서도 “수동 입력” 허용 여부는 정책)
- `scanId`는 **1번만 Problem에 연결 가능** (중복 등록 방지)
- 생성 성공 시 scan에 `problem_id` 연결(또는 `registered_at` 기록)

### Request (예측)

```json
{
    "scanId":123,
    "finalUnitId":"U_ALG_POLY",
    "finalTypeId":"T_POLY",
    "problemLatex":"문제 본문(LaTeX/텍스트 혼합)",
    "answer":{
    "format":"CHOICE",
    "choiceNo":3,
    "value":null
    },
    "choices":[
    {"choiceNo":1,"label":"1","text":"..."},
    {"choiceNo":2,"label":"2","text":"..."},
    {"choiceNo":3,"label":"3","text":"..."},
    {"choiceNo":4,"label":"4","text":"..."},
    {"choiceNo":5,"label":"5","text":"..."}
    ],
    "solutionLatex":"풀이(선택)",
    "memo":"메모(선택)"
}

```

### Response (201)

```json
{
    "status":201,
    "code":"SUC_PROBLEM_CREATED",
    "data":{
    "problemId":777,
    "scanId":123
    },
    "message":"문제가 등록되었습니다."
}

```

### 실패(예측)

| Status | Code | Description |
| --- | --- | --- |
| 404 | RES_404 | scanId 없음 |
| 403 | AUTH_002 | 타인 scan |
| 409 | PROB_011 | scan 이미 등록됨 |
| 400 | PROB_012 | answer 형식 불일치 |
| 400 | REQ_001 | 필드 누락 |

---

## 7) GET `/api/v1/problems`

### 개요

문제 리스트(오답노트 리스트)를 조회한다.

필터(단원/유형/완료) + 정렬 + 커서 페이징을 지원한다.

### Request (예측)

| Name | Type | Description |
| --- | --- | --- |
| unitId | String | 단원 필터 |
| typeIds | string(csv) | 유형 필터(T1,T2) |
| done | boolean | 오답 완료 여부 |
| sort | string | recent / oldest / unit / type |
| cursor | Long | 마지막 problemId 기반 |
| size | int | 기본 20 |

### Response (200, 예측)

```json
{
    "status":200,
    "code":"SUC_PROBLEM_LIST",
    "data":{
    "items":[
    {
    "problemId":777,
    "unit":{"id":"U_ALG_POLY","name":"다항식"},
    "type":{"id":"T_POLY","name":"다항식"},
    "done":false,
    "thumbnailUrl":"https://...presigned...",
    "createdAt":"2026-01-15T21:11:00"
    }
    ],
    "nextCursor":700,
    "hasNext":true
    },
    "message":"조회 성공"
}

```

### 구현 디테일(예측)

- 정렬이 `recent`면 `problemId desc` 또는 `createdAt desc`
- 커서는 `problemId`를 쓰는 게 가장 단순(안정적)
- thumbnail은 scan의 ORIGINAL asset presigned url 재사용 가능

---

## 8) GET `/api/v1/problems/{problemId}`

### 개요

문제 상세 조회(본문/정답/선지/풀이/메모/이미지)를 제공한다.

### Response (200, 예측)

```json
{
    "status":200,
    "code":"SUC_PROBLEM_DETAIL",
    "data":{
    "problemId":777,
    "scanId":123,
    "unit":{"id":"U_ALG_POLY","name":"다항식"},
    "type":{"id":"T_POLY","name":"다항식"},
    "problemLatex":"...",
    "answer":{"format":"CHOICE","choiceNo":3,"value":null},
    "choices":[
    {"choiceNo":1,"label":"1","text":"..."}
    ],
    "solutionLatex":"...",
    "memo":"...",
    "done":false,
    "assets":[
    {"assetType":"ORIGINAL","viewUrl":"https://...presigned..."}
    ],
    "createdAt":"2026-01-15T21:11:00",
    "updatedAt":"2026-01-15T21:11:00"
    },
    "message":"조회 성공"
}

```

---

## 9) PATCH `/api/v1/problems/{problemId}`

### 개요

문제 수정(단원/유형/정답/메모/풀이/done)

### Request (예측)

```json
{
    "finalUnitId":"U_ALG_POLY",
    "finalTypeId":"T_POLY",
    "answer":{"format":"SHORT","value":"3","choiceNo":null},
    "solutionLatex":"수정 풀이",
    "memo":"수정 메모",
    "done":true
}

```

### Response (200)

```json
{
    "status":200,
    "code":"SUC_PROBLEM_UPDATED",
    "data":null,
    "message":"수정되었습니다."
}

```

---

## 10) DELETE `/api/v1/problems/{problemId}`

### 개요

문제 삭제(소프트 삭제 권장)

### Response (200)

```json
{
    "status":200,
    "code":"SUC_PROBLEM_DELETED",
    "data":null,
    "message":"삭제되었습니다."
}

```