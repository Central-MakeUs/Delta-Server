# Auth API

> Base URL: /api/v1/auth
>
>
> 담당자: Auth 파트
>
> 최종 수정일: 2026.01.04
>

---

## 기본 설명

### Auth

- Auth는 **로그인 / 토큰 재발급 / 로그아웃**을 통해 사용자의 인증 상태를 관리한다.
- 로그인/재발급 성공 시 **토큰은 응답 Body가 아니라 “응답 Header”로 내려간다.**
- 응답 Body는 성공/실패 모두 **고정 포맷**으로 내려간다: `status / code / data / message`

---

## 공통 규칙

### 공통 Response 형식 (모든 API 동일)

```json
{
"status":200,
"code":"SUC_...",
"data":{},
"message":"..."
}

```

- `status`: HTTP status code
- `code`: 정책 코드(SuccessCode/ErrorCode)
- `data`: 실제 payload (성공 시 DTO / 실패 시 보통 null)
- `message`: 정책 메시지

---

### 토큰 전달 규칙 (매우 중요)

- Access Token (응답 헤더)
    - `Authorization: Bearer {accessToken}`
- Refresh Token (요청/응답 헤더)
    - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}`
- 프론트에서 헤더를 읽기 위한 설정(응답 헤더)
    - `Access-Control-Expose-Headers: AuthHeaderConstants.EXPOSE_HEADERS_VALUE`

> 프론트 구현 시:
>
>
> 로그인/재발급 응답에서 토큰 헤더가 오면 **항상 최신 값으로 교체 저장**해야 한다.
>

---

### 공통 에러코드 (Auth 도메인에서 공통 사용)

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 400 | INVALID_REQUEST (REQ_001) | 요청이 올바르지 않습니다 | 필수값 누락/형식 오류/헤더 누락/validation 실패 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 인가코드/토큰 검증 실패, 만료, 위조 등 |
| 403 | ACCESS_DENIED (AUTH_002) | 접근 권한이 없습니다 | 권한 부족 (정책 적용 시) |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | 잘못된 HTTP Method 호출 |

실패 응답 예시(형태)

```json
{
"status":401,
"code":"AUTH_001",
"data":null,
"message":"인증에 실패했습니다."
}

```

---

# 엔드포인트

---

## `POST` /api/v1/auth/kakao

### 개요

| 항목 | 내용 |
| --- | --- |
| **설명** | 카카오 인가코드로 로그인하고 토큰을 발급한다 |
| **인증** | None |
| **권한** | ALL |

### Method 선택 이유

- `POST` : 인가코드를 통해 **로그인 세션(토큰)을 새로 발급**하는 행위이며, 결과적으로 서버 상태(토큰/세션)가 생성되므로 POST 사용.

---

### Request

### Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| Content-Type | application/json | O | JSON 요청 |

### Request Body (SocialLoginRequest)

```json
{
"code":"kakao_authorization_code"
}

```

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| code | String | O | 카카오 인가코드 |

---

### Response

### 성공 (200 OK)

### Response Headers

| Key | Example | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer eyJ... | O | Access Token |
| AuthHeaderConstants.REFRESH_TOKEN_HEADER | eyJ... | △ | Refresh Token (현재 구현상 null/blank면 생략될 수 있음) |
| Access-Control-Expose-Headers | Authorization, ... | O | 브라우저에서 토큰 헤더 읽기 허용 |

### Response Body (ApiResponse<SocialLoginData>)

```json
{
"status":200,
"code":"SUC_...",
"data":{
"...":"SocialLoginData fields"
},
"message":"..."
}

```

> SocialLoginData의 실제 필드는 cmc.delta.domain.auth.api.dto.response.SocialLoginData 정의를 따른다.
>

---

### 실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 400 | INVALID_REQUEST (REQ_001) | 요청이 올바르지 않습니다 | code 누락/빈값, JSON 파싱 실패, validation 실패 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 인가코드 만료/불일치/검증 실패 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |

---

## `POST` /api/v1/auth/reissue

### 개요

| 항목 | 내용 |
| --- | --- |
| **설명** | Refresh Token으로 Access/Refresh Token을 재발급한다 |
| **인증** | Optional (Refresh Token 기반) |
| **권한** | ALL |

### Method 선택 이유

- `POST` : 재발급은 “토큰 발급” 동작이며, 서버 측 검증/회전 정책에 따라 **새 토큰을 생성**하므로 POST 사용.

---

### Request

### Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| AuthHeaderConstants.REFRESH_TOKEN_HEADER | {refreshToken} | O | Refresh Token |

### Request Body

- 없음

---

### Response

### 성공 (200 OK)

### Response Headers

| Key | Example | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer eyJ... | O | New Access Token |
| AuthHeaderConstants.REFRESH_TOKEN_HEADER | eyJ... | O | New Refresh Token (항상 교체 저장 필요) |
| Access-Control-Expose-Headers | Authorization, ... | O | 브라우저에서 토큰 헤더 읽기 허용 |

### Response Body (ApiResponse<ActionResultData>)

```json
{
"status":200,
"code":"SUC_...",
"data":{
"success":true
},
"message":"..."
}

```

> ActionResultData.success() 형태로 성공 결과를 반환한다.
>
>
> (실제 필드는 `cmc.delta.domain.auth.api.dto.response.ActionResultData` 정의를 따른다)
>

---

### 실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 400 | INVALID_REQUEST (REQ_001) | 요청이 올바르지 않습니다 | Refresh 헤더 누락/빈값/형식 오류 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | Refresh 만료/위조/검증 실패, 세션 없음 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |

---

## `POST` /api/v1/auth/logout

### 개요

| 항목 | 내용 |
| --- | --- |
| **설명** | 현재 로그인 사용자의 토큰/세션을 무효화한다 |
| **인증** | Required |
| **권한** | USER |

### Method 선택 이유

- `POST` : 로그아웃은 “세션 무효화”라는 **상태 변경** 동작이며, 리소스 삭제(DELETE)로 보기 애매하고 정책/부작용(블랙리스트 등록 등)이 존재할 수 있어 POST 사용.

---

### Request

### Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | O | Access Token |

### Request Body

- 없음

---

### Response

### 성공 (200 OK)

### Response Body (ApiResponse<ActionResultData>)

```json
{
"status":200,
"code":"SUC_...",
"data":{
"success":true
},
"message":"..."
}

```

---

### 실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | Authorization 헤더 누락/만료/위조/검증 실패 |
| 403 | ACCESS_DENIED (AUTH_002) | 접근 권한이 없습니다 | 권한 부족(정책 적용 시) |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |