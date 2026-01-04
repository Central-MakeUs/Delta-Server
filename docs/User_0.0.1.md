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

### User

- User는 **내 프로필 조회 / 회원 탈퇴**를 제공한다.
- 인증이 필요한 API는 **Authorization 헤더의 Access Token**을 통해 인증한다.
- 응답 Body는 성공/실패 모두 **고정 포맷**으로 내려간다: `status / code / data / message`

### User는 완전 초기단계라서 변경 될 가능성이 매우 높습니다.

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

### 인증 규칙

- Access Token (요청 헤더)
    - `Authorization: Bearer {accessToken}`

> 프론트 구현 시:
>
>
> 인증이 필요한 API 호출에는 `Authorization` 헤더를 반드시 포함해야 한다.
>

---

### 공통 에러코드 (User 도메인에서 공통 사용)

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | TOKEN_REQUIRED (AUTH_010) | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 토큰 만료/위조/검증 실패 |
| 403 | ACCESS_DENIED (AUTH_002) | 접근 권한이 없습니다 | 권한 부족 (정책 적용 시) |
| 404 | USER_NOT_FOUND (USER_001) | 사용자를 찾을 수 없습니다 | 인증된 사용자 ID가 DB에 존재하지 않음 |
| 403 | USER_WITHDRAWN (USER_002) | 탈퇴한 사용자입니다 | 탈퇴한 사용자 접근 차단 (정책 적용 시) |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | 잘못된 HTTP Method 호출 |

실패 응답 예시(형태)

```json
{
"status":401,
"code":"AUTH_010",
"data":null,
"message":"토큰이 필요합니다."
}

```

---

# DTO

## UserMeData

- 패키지: `cmc.delta.domain.user.api.dto.response.UserMeData`

| Field | Type | Description |
| --- | --- | --- |
| id | Long | 내부 사용자 ID |
| email | String | 이메일 (소셜 제공 동의 기반, null 가능) |
| nickname | String | 닉네임 (소셜 제공 동의 기반, null 가능) |

> 실제 반환 값은 DB에 저장된 User 엔티티 값을 기반으로 한다.
>

---

# 엔드포인트

---

## `GET` /api/v1/users/me

### 개요

| 항목 | 내용 |
| --- | --- |
| **설명** | 로그인된 사용자의 내 프로필 정보를 조회한다 |
| **인증** | Required |
| **권한** | USER |

### Method 선택 이유

- `GET` : 내 프로필을 조회하는 **조회(Read)** 동작이므로 GET 사용.

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

### Response Body (ApiResponse<UserMeData>)

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

---

### 실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | TOKEN_REQUIRED (AUTH_010) | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 토큰 만료/위조/검증 실패 |
| 404 | USER_NOT_FOUND (USER_001) | 사용자를 찾을 수 없습니다 | DB에 사용자 없음 |
| 403 | USER_WITHDRAWN (USER_002) | 탈퇴한 사용자입니다 | 탈퇴 사용자 차단 정책 적용 시 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |

---

## `POST` /api/v1/users/withdrawal

### 개요

| 항목 | 내용 |
| --- | --- |
| **설명** | 현재 로그인 사용자의 회원 탈퇴를 수행한다 (소프트 탈퇴: 상태 변경) |
| **인증** | Required |
| **권한** | USER |

### Method 선택 이유

- `POST` : 탈퇴는 “상태 변경(Withdraw)” 동작이며, 단순 리소스 삭제(DELETE)로 보기 애매하고 정책/부작용이 붙을 수 있어 POST 사용.

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

### Response Body (ApiResponse<Void>)

- `data`는 항상 `null` 이다.

```json
{
"status":200,
"code":"SUC_...",
"data":null,
"message":"..."
}

```

> 회원 탈퇴는 현재 User.status를 WITHDRAWN으로 변경하는 소프트 탈퇴 방식이다.
>
>
> (DB row는 삭제되지 않는다)
>

---

### 실패 케이스

| Status | Code | Message | Description |
| --- | --- | --- | --- |
| 401 | TOKEN_REQUIRED (AUTH_010) | 토큰이 필요합니다 | Authorization 헤더 누락 |
| 401 | AUTHENTICATION_FAILED (AUTH_001) | 인증에 실패했습니다 | 토큰 만료/위조/검증 실패 |
| 404 | USER_NOT_FOUND (USER_001) | 사용자를 찾을 수 없습니다 | DB에 사용자 없음 |
| 403 | USER_WITHDRAWN (USER_002) | 탈퇴한 사용자입니다 | 이미 탈퇴한 사용자 |
| 405 | METHOD_NOT_ALLOWED (REQ_002) | 허용되지 않은 메서드입니다 | Method 오류 |

---

## 구현 메모 (운영/정책)

- 회원 탈퇴는 soft delete 형태로 처리하며, 탈퇴 사용자의 접근 차단 정책은 서버 정책에 따라 달라질 수 있다.
- 인증 실패(401) 응답은 `RestAuthenticationEntryPoint`에서 공통 처리된다.
- 접근 권한 부족(403) 응답은 `RestAccessDeniedHandler`에서 공통 처리된다.