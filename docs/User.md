# User Domain API

관련 코드:
- `src/main/java/cmc/delta/domain/user/adapter/in/UserController.java`
- `src/main/java/cmc/delta/domain/user/adapter/in/UserProfileImageController.java`

## User

Base URL: `/api/v1/users`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| USER-01 | GET | `/api/v1/users/me` | Required | 내 프로필 조회 | - | `ApiResponse<UserMeData>` |
| USER-02 | POST | `/api/v1/users/me/onboarding` | Required | 온보딩 완료 | `UserOnboardingRequest` (JSON) | `ApiResponse<Void>` |
| USER-03 | PATCH | `/api/v1/users/me` | Required | 닉네임 수정 | `UserNicknameUpdateRequest` (JSON) | `ApiResponse<Void>` |
| USER-04 | POST | `/api/v1/users/withdrawal` | Required | 회원 탈퇴 | - | `ApiResponse<Void>` |

## Profile Image

Base URL: `/api/v1/users/me`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| USER-05 | POST | `/api/v1/users/me/profile-image` | Required | 프로필 이미지 업로드/교체 | multipart `file` | `ApiResponse<UserProfileImageResult>` |
| USER-06 | GET | `/api/v1/users/me/profile-image` | Required | 프로필 이미지 조회 | - | `ApiResponse<UserProfileImageResult>` |
| USER-07 | DELETE | `/api/v1/users/me/profile-image` | Required | 프로필 이미지 삭제 | - | `ApiResponse<Void>` |
