# User API

> Base URL: `/api/v1/users`
>
> 담당자: User 파트
>
> 최종 수정일: 2026.01.25

---

## 기본 설명

- User는 **내 프로필 조회 / 온보딩 완료 / 회원 탈퇴 / 프로필 이미지 업로드/조회/삭제**를 제공한다.
- 인증이 필요한 API는 Authorization 헤더의 Access Token으로 인증한다.

관련 코드

- 컨트롤러: `src/main/java/cmc/delta/domain/user/adapter/in/UserController.java`
- 컨트롤러: `src/main/java/cmc/delta/domain/user/adapter/in/UserProfileImageController.java`
- 응답 DTO: `src/main/java/cmc/delta/domain/user/adapter/in/dto/response/UserMeData.java`
- 요청 DTO: `src/main/java/cmc/delta/domain/user/adapter/in/dto/request/UserOnboardingRequest.java`
- 요청 DTO: `src/main/java/cmc/delta/domain/user/application/port/in/dto/ProfileImageUploadCommand.java`
- 응답 DTO: `src/main/java/cmc/delta/domain/user/application/port/in/dto/UserProfileImageResult.java`

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

## 인증 규칙

- `Authorization: Bearer {accessToken}`

---

## 엔드포인트

## `GET` /api/v1/users/me

- 설명: 로그인된 사용자의 내 프로필 정보 조회
- 인증: Required

Response (200)

```json
{
  "status": 200,
  "code": "SUC_...",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "delta"
  },
  "message": "..."
}
```

---

## `POST` /api/v1/users/me/onboarding

- 설명: 추가정보 입력(가입 완료)
- 인증: Required

Request

```json
{
  "name": "홍길동",
  "birthDate": "2000-01-01",
  "termsAgreed": true
}
```

Response (200)

- `ApiResponse<Void>`

---

## `POST` /api/v1/users/withdrawal

- 설명: 회원 탈퇴
- 인증: Required

주의

- 현재 구현은 **하드 삭제 정책**(삭제 후 조회 시 USER_NOT_FOUND)이다.

Response (200)

- `ApiResponse<Void>`

---

## 프로필 이미지

> Base URL: `/api/v1/users/me`

## `POST` /api/v1/users/me/profile-image

- 설명: 내 프로필 이미지 업로드/교체
- 인증: Required
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
    "storageKey": "users/profile/...",
    "viewUrl": "https://...presigned...",
    "ttlSeconds": 60
  },
  "message": "..."
}
```

---

## `GET` /api/v1/users/me/profile-image

- 설명: 내 프로필 이미지 조회
- 인증: Required

Response (200)

- 이미지가 없으면 `data`는 `{ "storageKey": null, "viewUrl": null, "ttlSeconds": null }`

---

## `DELETE` /api/v1/users/me/profile-image

- 설명: 내 프로필 이미지 삭제
- 인증: Required

Response (200)

- `ApiResponse<Void>`
