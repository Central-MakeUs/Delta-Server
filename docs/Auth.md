# Auth Domain API

Base URL: `/api/v1/auth`

관련 코드:
- `src/main/java/cmc/delta/domain/auth/adapter/in/web/SocialAuthController.java`
- `src/main/java/cmc/delta/domain/auth/adapter/in/web/AuthTokenController.java`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| AUTH-01 | POST | `/api/v1/auth/kakao` | Public | 카카오 인가코드 로그인 | `KakaoLoginRequest` (JSON) | `ApiResponse<SocialLoginData>` + 토큰 헤더 |
| AUTH-02 | POST | `/api/v1/auth/apple` | Public | Apple form_post 콜백 처리 | form `code`, `user?` | `303 See Other` + `Location` |
| AUTH-03 | POST | `/api/v1/auth/apple/exchange` | Public | loginKey 교환 | query `loginKey` | `ApiResponse<SocialLoginData>` + 토큰 헤더 |
| AUTH-04 | POST | `/api/v1/auth/reissue` | Public(토큰 필요) | 토큰 재발급 | refresh token header | `ApiResponse<Void>` + 재발급 토큰 헤더 |
| AUTH-05 | POST | `/api/v1/auth/logout` | Required | 로그아웃 | access token header | `ApiResponse<Void>` |
