# Auth API

`Base URL`: `/api/v1/auth`

담당자: Auth 파트

최종 수정일: 2026-01-25 (초안 갱신)

---

## 개요

- 소셜 로그인(카카오/애플), 토큰 재발급, 로그아웃을 제공합니다.
- 로그인 및 재발급 성공 시 Access Token은 응답 Header(`Authorization`), Refresh Token은 지정 헤더(`AuthHeaderConstants.REFRESH_TOKEN_HEADER`)로 내려갑니다.

## 토큰 전달 규칙 (중요)

- Access Token (응답 Header)
  - `Authorization: Bearer {accessToken}`
- Refresh Token (요청/응답 Header)
  - 헤더 키: `AuthHeaderConstants.REFRESH_TOKEN_HEADER`
- 브라우저에서 헤더를 읽기 위한 노출(응답 Header)
  - `Access-Control-Expose-Headers: AuthHeaderConstants.EXPOSE_HEADERS_VALUE`

## 관련 코드

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

### `POST /api/v1/auth/kakao`

- 설명: 카카오 인가코드로 로그인 후 토큰 발급
- 인증: 없음
- Request (application/json)

```json
{
  "code": "kakao_authorization_code"
}
```

- Response (200) — Headers
  - `Authorization: Bearer {accessToken}`
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}` (구현에 따라 생략될 수 있음)

- Response Body: `ApiResponse<SocialLoginData>`

`SocialLoginData` 예시

```json
{
  "email": "user@example.com",
  "nickname": "delta",
  "isNewUser": false
}
```

#### 오류(대표)

- `400 BAD_REQUEST` — `code` 누락/형식 오류
- `401 UNAUTHORIZED` — 공급자에서 받은 code가 유효하지 않음
- `502/503` — 외부 OAuth 공급자 오류

---

#### curl 예시

```bash
curl -X POST "https://api.example.com/api/v1/auth/kakao" \
  -H "Content-Type: application/json" \
  -d '{"code":"kakao_authorization_code"}' -i
```

성공 시 응답 헤더 예시:

```
HTTP/1.1 200 OK
Authorization: Bearer eyJhbGciOiJI...
AuthHeaderConstants.REFRESH_TOKEN_HEADER: eyJhbGciOiJI... (optional)
```

### `POST /api/v1/auth/apple`

- 설명: Apple form_post 콜백 처리 후 로그인
- 인증: 없음
- Content-Type: `application/x-www-form-urlencoded`
- Request (form)

```
code=<authorization_code>
user=<optional_user_json>
```

- Response: Authorization 헤더 + `ApiResponse<SocialLoginData>`
- 오류: 카카오와 유사 (provider error / validation)

---

### `POST /api/v1/auth/reissue`

- 설명: Refresh Token으로 Access/Refresh 토큰 재발급
- 인증: Optional (Refresh Token 기반)
- Request Headers
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}`
- Response (200) — Headers
  - `Authorization: Bearer {accessToken}`
  - `AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}`
- Response Body: `ApiResponse<Void>`

#### 오류(대표)

- `401 UNAUTHORIZED` — refresh token 만료/무효 (예: `AUTH_REFRESH_EXPIRED`)
- `400 BAD_REQUEST` — header 누락

---

#### curl 예시

```bash
curl -X POST "https://api.example.com/api/v1/auth/reissue" \
  -H "AuthHeaderConstants.REFRESH_TOKEN_HEADER: {refreshToken}" -i
```


### `POST /api/v1/auth/logout`

- 설명: 현재 로그인 사용자의 토큰/세션 무효화
- 인증: Required (`Authorization: Bearer {accessToken}`)
- Response: `ApiResponse<Void>`

#### 오류(대표)

- `401 UNAUTHORIZED` — 토큰 미제공/무효
- `500 INTERNAL_ERROR` — 내부 처리 중 오류

---

## 비고

- 에러코드 표기는 레포지토리의 `ErrorCode` enum으로 대체하세요.
- 브라우저에서 토큰을 읽으려면 응답에 `AuthHeaderConstants.EXPOSE_HEADERS_VALUE`가 포함되어야 합니다.
