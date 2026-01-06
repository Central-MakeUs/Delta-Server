# Problem API (아직 완성 아님)

> Base URL: /api/v1/problems
>
>
> 인증: `Authorization: Bearer {accessToken}`
>
> 공통 응답 포맷: `status / code / data / message`
>
> 공통 헤더: `X-Trace-Id`
>

---

## 공통 규칙

### 공통 Response 형식

```json
{
"status":200,
"code":"SUC_...",
"data":{},
"message":"..."
}
```

### 공통 실패 Response 예시

```json
{
"status":401,
"code":"AUTH_010",
"data":null,
"message":"토큰이 필요합니다."
}
```

### 공통 에러코드

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | TOKEN_REQUIRED (AUTH_010) | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 토큰 만료/위조/검증 실패 |
| 403 | ACCESS_DENIED (AUTH_002) | 접근 권한이 없습니다 | 권한 부족 |
| 404 | RESOURCE_NOT_FOUND (RES_404) | 리소스를 찾을 수 없습니다 | 존재하지 않는 리소스 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |
| 400 | INVALID_REQUEST (REQ_001) | 요청이 올바르지 않습니다 | Validation 실패/요청 형식 오류 |
| 413 | PAYLOAD_TOO_LARGE (REQ_013) | 파일 크기가 너무 큽니다 | 업로드 용량 제한 초과(정책 시) |
| 415 | UNSUPPORTED_MEDIA_TYPE (REQ_014) | 지원하지 않는 파일 형식입니다 | 이미지 타입 제한(정책 시) |
| 502 | OCR_PROVIDER_FAIL (EXT_001) | OCR 처리에 실패했습니다 | 외부 OCR 장애/오류 |
| 502 | AI_PROVIDER_FAIL (EXT_002) | AI 분석에 실패했습니다 | 외부 AI 장애/오류 |

---

# 엔드포인트

## POST /api/v1/problems/scans

### 개요

문제 사진 1장을 업로드하고 스캔을 생성한다. OCR/AI 분석은 비동기로 진행된다.

### Request

Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | O | Access Token |
| Content-Type | multipart/form-data | O | 이미지 업로드 |

Multipart Form

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| image | file | O | 문제 사진(1장) |
| source | string | X | CAMERA / GALLERY |
| clientCrop | json(string) | X | 프론트 자르기 좌표(선택) |

clientCrop 예시

```json
{"x":120,"y":80,"w":900,"h":1200}
```

요청으로 받는 값

- image (file)
- source (string, optional)
- clientCrop (json string, optional)

### Response

성공 (201 Created)

```json
{
    "status":201,
    "code":"SUC_...",
    "data":{
    "scanId":123,
    "status":"UPLOADED",
    "assets":[
{
    "assetId":10,
    "assetType":"ORIGINAL",
    "url":"https://.../original.jpg",
    "width":1170,
    "height":2532
}
]
},
    "message":"..." 
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 400 | REQ_001 | 요청이 올바르지 않습니다 | image 누락/형식 오류 |
| 413 | REQ_013 | 파일 크기가 너무 큽니다 | 업로드 제한 초과 |
| 415 | REQ_014 | 지원하지 않는 파일 형식입니다 | 이미지 타입 제한 |

실패 예시 (415 UNSUPPORTED_MEDIA_TYPE)

```json
{
    "status":415,
    "code":"REQ_014",
    "data":null,
    "message":"지원하지 않는 파일 형식입니다."
}

```

---

## GET /api/v1/problems/scans/{scanId}

### 개요

스캔 단건 상태 및 OCR/AI 결과(초안)를 조회한다. 프론트는 이 API를 폴링해 다음 화면으로 이동한다.

### Request

Path Params

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| scanId | Long | O | 스캔 ID |

요청으로 받는 값

- scanId (path)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":{
    "scanId":123,
    "status":"AI_DONE",
    "originalImageUrl":"https://.../original.jpg",
    "ocr":{
    "plainText":"문제 텍스트...",
    "raw":{}
},
    "ai":{
    "predictedUnitId":"U-001",
    "predictedTypeId":"T-010",
    "confidence":0.87,
    "draft":{
    "problemMarkdown":"...",
    "answerFormat":"CHOICE",
    "choices":[
    {"choiceNo":1,"label":"1","text":"..."}
],
    "suggestedAnswer":{"choiceNo":3},
    "solutionSummary":"..."
}
},
    "assets":[
    {"assetId":10,"assetType":"ORIGINAL","url":"https://..."}
],
    "failReason":null 
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 404 | RES_404 | 리소스를 찾을 수 없습니다 | scanId 없음 |
| 403 | AUTH_002 | 접근 권한이 없습니다 | 타인 스캔 접근 |

실패 예시 (404 RESOURCE_NOT_FOUND)

```json
{
    "status":404,
    "code":"RES_404",
    "data":null,
    "message":"리소스를 찾을 수 없습니다."
}
```

---

## GET /api/v1/problems/meta/units

### 개요

단원 목록을 조회한다. (단원 선택 화면 데이터)

### Request

- 없음

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":{
    "units":[
    {"id":"U-001","name":"공통수학1","parentId":null,"sortOrder":0},
    {"id":"U-001-01","name":"다항식","parentId":"U-001","sortOrder":10}
]
    },
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |

실패 예시 (401 TOKEN_REQUIRED)

```json
{
    "status":401,
    "code":"AUTH_010",
    "data":null,
    "message":"토큰이 필요합니다."
}
```

---

## GET /api/v1/problems/meta/types

### 개요

유형(태그) 목록을 조회한다. (유형 선택 화면 데이터)

### Request

Query Params

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| unitId | String | X | 단원 기준 필터링(선택) |

요청으로 받는 값

- unitId (optional)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":{
    "types":[
    {"id":"T-010","name":"다항식","sortOrder":0,"isSystem":true}
]
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 400 | REQ_001 | 요청이 올바르지 않습니다 | unitId 형식 오류 |

실패 예시 (400 INVALID_REQUEST)

```json
{
    "status":400,
    "code":"REQ_001",
    "data":null,
    "message":"요청이 올바르지 않습니다."
}
```

---

## POST /api/v1/problems/meta/types

### 개요

사용자가 유형을 직접 추가한다. (유형 선택 화면의 '직접 추가하기')

### Request

Request Body

```json
{
    "name":"새 유형 이름"
}
```

요청으로 받는 값

- name (string, 1~100)

### Response

성공 (201 Created)

```json
{
    "status":201,
    "code":"SUC_...",
    "data":{
    "id":"T-U-999",
    "name":"새 유형 이름",
    "isSystem":false
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 400 | REQ_001 | 요청이 올바르지 않습니다 | name 비어있음/길이초과 |
| 409 | TYPE_DUPLICATED (PROB_010) | 이미 존재하는 유형입니다 | 사용자 커스텀 중복 |

실패 예시 (409 TYPE_DUPLICATED)

```json
{
    "status":409,
    "code":"PROB_010",
    "data":null,
    "message":"이미 존재하는 유형입니다."
}
```

---

## POST /api/v1/problems

### 개요

스캔 결과를 바탕으로 최종 문제를 등록한다. 단원/유형/정답/메모를 확정한다.

### Request

Request Body

```json
{
    "scanId":123,
    "finalUnitId":"U-001-01",
    "finalTypeId":"T-010",
    "problemMarkdown":"문제 본문(텍스트/수식 포함)",
    "answer":{
    "format":"CHOICE",
    "value":null,
    "choiceNo":3,
    "choiceLabel":"3"
    },
    "choices":[
    {"choiceNo":1,"label":"1","text":"..."},
    {"choiceNo":2,"label":"2","text":"..."},
    {"choiceNo":3,"label":"3","text":"..."},
    {"choiceNo":4,"label":"4","text":"..."},
    {"choiceNo":5,"label":"5","text":"..."}
    ],
    "solutionText":"풀이(선택)",
    "memo":"메모(선택)"
}
```

요청으로 받는 값

- scanId
- finalUnitId / finalTypeId
- problemMarkdown
- answer (format + 값)
- choices (객관식일 때)
- solutionText / memo (optional)

### Response

성공 (201 Created)

```json
{
    "status":201,
    "code":"SUC_...",
    "data":{
    "problemId":777,
    "scanId":123
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 400 | REQ_001 | 요청이 올바르지 않습니다 | 필드 누락/형식 오류 |
| 404 | RES_404 | 리소스를 찾을 수 없습니다 | scanId 없음 |
| 409 | SCAN_ALREADY_REGISTERED (PROB_011) | 이미 등록된 스캔입니다 | scanId 중복 등록 |
| 400 | INVALID_ANSWER (PROB_012) | 정답 형식이 올바르지 않습니다 | format과 값 불일치 |

실패 예시 (409 SCAN_ALREADY_REGISTERED)

```json
{
    "status":409,
    "code":"PROB_011",
    "data":null,
    "message":"이미 등록된 스캔입니다."
}
```

---

## GET /api/v1/problems

### 개요

오답 리스트를 조회한다. 단원/유형/오답완료 여부/정렬을 지원한다.

### Request

Query Params

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| unitId | String | X | 단원 필터(단일 선택) |
| typeIds | string(csv) | X | 유형 필터(복수 선택) 예: T-1,T-2 |
| done | boolean | X | 오답 완료 필터(true/false) |
| sort | string | X | recent / oldest / unit_most / type_most |
| cursor | Long | X | 커서 기반 페이징 |
| size | int | X | 기본 20 |

요청으로 받는 값

- unitId (optional)
- typeIds (optional)
- done (optional)
- sort (optional)
- cursor/size (optional)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":{
    "items":[
{
    "problemId":777,
    "unitId":"U-001-01",
    "typeId":"T-010",
    "done":false,
    "createdAt":"2026-01-05T12:34:56",
    "thumbnailUrl":"https://.../original.jpg"
}
],
    "nextCursor":700
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 400 | REQ_001 | 요청이 올바르지 않습니다 | sort/typeIds 형식 오류 |

실패 예시 (400 INVALID_REQUEST)

```json
{
    "status":400,
    "code":"REQ_001",
    "data":null,
    "message":"요청이 올바르지 않습니다."
}
```

---

## GET /api/v1/problems/{problemId}

### 개요

문제 상세를 조회한다. (이미지 확대, 분석 결과, 풀이/답, 메모 포함)

### Request

Path Params

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| problemId | Long | O | 문제 ID |

요청으로 받는 값

- problemId (path)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":{
    "problemId":777,
    "scanId":123,
    "unitId":"U-001-01",
    "typeId":"T-010",
    "problemMarkdown":"...",
    "answer":{
    "format":"CHOICE",
    "choiceNo":3,
    "choiceLabel":"3",
    "value":null
},
    "choices":[
    {"choiceNo":1,"label":"1","text":"..."}
],
    "solutionText":"...",
    "memo":"...",
    "assets":[
    {"assetType":"ORIGINAL","url":"https://..."}
],
    "createdAt":"2026-01-05T12:34:56",
    "updatedAt":"2026-01-05T12:34:56"
},
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 404 | RES_404 | 리소스를 찾을 수 없습니다 | problemId 없음 |
| 403 | AUTH_002 | 접근 권한이 없습니다 | 타인 문제 접근 |

실패 예시 (403 ACCESS_DENIED)

```json
{
    "status":403,
    "code":"AUTH_002",
    "data":null,
    "message":"접근 권한이 없습니다."
}
```

---

## PATCH /api/v1/problems/{problemId}

### 개요

문제 정보를 수정한다(단원/유형/정답/메모/풀이/오답완료).

### Request

Path Params

- problemId (Long)

Request Body (부분 수정)

```json
{
    "finalUnitId":"U-001-02",
    "finalTypeId":"T-020",
    "answer":{
    "format":"CHOICE",
    "choiceNo":2,
    "choiceLabel":"2",
    "value":null
},
    "solutionText":"수정된 풀이",
    "memo":"수정된 메모",
    "done":true
}
```

요청으로 받는 값

- problemId (path)
- finalUnitId/finalTypeId (optional)
- answer (optional)
- solutionText/memo (optional)
- done (optional)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":null,
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 404 | RES_404 | 리소스를 찾을 수 없습니다 | problemId 없음 |
| 403 | AUTH_002 | 접근 권한이 없습니다 | 타인 문제 수정 |
| 400 | PROB_012 | 정답 형식이 올바르지 않습니다 | format과 값 불일치 |

실패 예시 (400 INVALID_ANSWER)

```json
{
    "status":400,
    "code":"PROB_012",
    "data":null,
    "message":"정답 형식이 올바르지 않습니다."
}
```

---

## DELETE /api/v1/problems/{problemId}

### 개요

문제를 삭제한다(소프트 삭제 권장).

### Request

- problemId (Long)

### Response

성공 (200 OK)

```json
{
    "status":200,
    "code":"SUC_...",
    "data":null,
    "message":"..."
}
```

실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTH_010 | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTH_001 | 인증에 실패했습니다 | 토큰 검증 실패 |
| 404 | RES_404 | 리소스를 찾을 수 없습니다 | problemId 없음 |
| 403 | AUTH_002 | 접근 권한이 없습니다 | 타인 문제 삭제 |

실패 예시 (404 RESOURCE_NOT_FOUND)

```json
{
    "status":404,
    "code":"RES_404",
    "data":null,
    "message":"리소스를 찾을 수 없습니다."
}
```