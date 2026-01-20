# User API

> Base URL: /api/v1/users
>
>
> 담당자: User 파트
>
> 최종 수정일: 2026.01.05
>

---

## 기본 설명

- User는 내 프로필 조회 / 회원 탈퇴를 제공한다.
- 인증이 필요한 API는 Authorization 헤더의 Access Token으로 인증한다.
- 응답 Body는 성공/실패 모두 고정 포맷으로 내려간다: `status / code / data / message`
- 모든 응답에는 `X-Trace-Id` 헤더가 포함된다.

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

- status: HTTP status code
- code: 정책 코드(SuccessCode/ErrorCode)
- data: 실제 payload (성공 시 DTO / 실패 시 null)
- message: 정책 메시지

### 인증 규칙

- `Authorization: Bearer {accessToken}`

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
| 404 | USER_NOT_FOUND (USER_001) | 사용자를 찾을 수 없습니다 | 인증된 사용자 ID가 DB에 존재하지 않음 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | 잘못된 HTTP Method 호출 |

---

### DTO

### UserMeData

- 패키지: `cmc.delta.domain.user.adapter.in.web.dto.response.UserMeData`

| Field | Type | Description |
| --- | --- | --- |
| id | Long | 내부 사용자 ID |
| email | String | 이메일 (소셜 동의 기반, null 가능) |
| nickname | String | 닉네임 (소셜 동의 기반, null 가능) |

---

# GET /api/v1/users/me

## 개요

| 항목 | 내용 |
| --- | --- |
| 설명 | 로그인된 사용자의 내 프로필 정보를 조회한다 |
| 인증 | Required |

## Request

### Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | O | Access Token |

### Path Params

- 없음

### Query Params

- 없음

### Body

- 없음

## 요청으로 받는 값

- accessToken (Authorization 헤더)

## Response

### 성공 (200 OK)

```json
{
  "status":200,
  "code":"SUC_...",
  "data":{
    "id":1,
    "email":"user@example.com",
    "nickname":"delta"
  },
  "message":"..."
}
```

### 실패 예시 (404 USER_NOT_FOUND)

```json
  {
  "status":404,
  "code":"USER_001",
  "data":null,
  "message":"사용자를 찾을 수 없습니다."
  }
```

---

# POST /api/v1/users/withdrawal

## 개요

| 항목 | 내용 |
| --- | --- |
| 설명 | 현재 로그인 사용자의 회원 탈퇴를 수행한다 (소프트 탈퇴: 상태 변경) |
| 인증 | Required |

## Request

### Headers

| Key | Value | Required | Description |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | O | Access Token |

### Path Params

- 없음

### Query Params

- 없음

### Body

- 없음

## 요청으로 받는 값

- accessToken (Authorization 헤더)

## Response

### 성공 (200 OK)

```json
{
  "status":200,
  "code":"SUC_...",
  "data":null,
  "message":"..."
}
```

### 실패 예시 (403 USER_WITHDRAWN)

```json
{
  "status":403,
  "code":"USER_002",
  "data":null,
  "message":"탈퇴한 사용자입니다."
}
```