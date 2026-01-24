# Auth API

> Base URL: `/api/v1/auth`
>
> 담당자: Auth 파트
>
> 최종 수정일: 2026.01.25

---

## 기본 설명

- Auth는 **소셜 로그인 / 토큰 재발급 / 로그아웃**을 제공한다.
- 로그인/재발급 성공 시 토큰은 **응답 Header**로 내려간다.

## 토큰 전달 규칙 (중요)

- Access Token (응답 Header)
  - `Authorization: Bearer {accessToken}`
- Refresh Token (요청/응답 Header)
  - 헤더 키: `AuthHeaderConstants.REFRESH_TOKEN_HEADER`
- 브라우저에서 헤더를 읽기 위한 설정(응답 Header)
  - `Access-Control-Expose-Headers: AuthHeaderConstants.EXPOSE_HEADERS_VALUE`

관련 코드

- 컨트롤러: `src/main/java/cmc/delta/domain/auth/adapter/in/web/SocialAuthController.java`
- 컨트롤러: `src/main/java/cmc/delta/domain/auth/adapter/in/web/AuthTokenController.java`
- 로그인 응답 DTO: `src/main/java/cmc/delta/domain/auth/application/port/in/social/SocialLoginData.java`

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

## 엔드포인트

## `POST` /api/v1/auth/kakao

- 설명: 카카오 인가코드로 로그인 후 토큰 발급
- 인증: None

Request

```json
{
  "code": "kakao_authorization_code"
}
```

Response (200)

- Response Headers
  - `Authorization: Bearer {accessToken}`
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}` (구현에 따라 생략될 수 있음)

- Response Body: `ApiResponse<SocialLoginData>`

`SocialLoginData`

| Field | Type | Description |
| --- | --- | --- |
| email | String | 이메일(동의 기반, null 가능) |
| nickname | String | 닉네임(동의 기반, null 가능) |
| isNewUser | boolean | 신규 가입 여부 |

---

## `POST` /api/v1/auth/apple

- 설명: 애플 콜백(form_post) 처리 후 로그인
- 인증: None
- Content-Type: `application/x-www-form-urlencoded`

Request (form)

| Key | Required | Description |
| --- | --- | --- |
| code | O | 애플 인가코드 |
| user | X | 애플 user JSON(선택) |

Response (200)

- Response Headers
  - `Authorization: Bearer {accessToken}`
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}` (구현에 따라 생략될 수 있음)

- Response Body: `ApiResponse<SocialLoginData>`

---

## `POST` /api/v1/auth/reissue

- 설명: Refresh Token으로 Access/Refresh 토큰 재발급
- 인증: Optional (Refresh Token 기반)

Request Headers

- `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}`

Response (200)

- Response Headers
  - `Authorization: Bearer {accessToken}`
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}`

- Response Body: `ApiResponse<Void>`

---

## `POST` /api/v1/auth/logout

- 설명: 현재 로그인 사용자의 토큰/세션 무효화
- 인증: Required

Request Headers

- `Authorization: Bearer {accessToken}`

Response (200)

- Response Body: `ApiResponse<Void>`
