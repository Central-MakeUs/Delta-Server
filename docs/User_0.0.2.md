# User API

`Base URL`: `/api/v1/users`

담당자: User 파트

최종 수정일: 2026-01-25 (초안 갱신)

---

## 개요

- 내 프로필 조회, 온보딩 완료, 회원 탈퇴, 프로필 이미지 업로드/조회/삭제를 제공합니다.
- 인증: `Authorization: Bearer {accessToken}` (필수)

## 관련 코드

- 컨트롤러: `src/main/java/cmc/delta/domain/user/adapter/in/UserController.java`
- 프로필 이미지 컨트롤러: `src/main/java/cmc/delta/domain/user/adapter/in/UserProfileImageController.java`
- DTO 예시: `src/main/java/cmc/delta/domain/user/adapter/in/dto` 디렉토리

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

## 엔드포인트 — 상세

### `GET /api/v1/users/me`

- 설명: 로그인한 사용자의 프로필 조회
- 인증: Required
- Request: 없음 (Header에 `Authorization` 필요)
- Response (200)

```json
{
  "status": 200,
  "code": "SUC_USER_FETCHED",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "delta"
  },
  "message": "성공"
}
```

- 오류(대표)
  - `401 UNAUTHORIZED` — 인증 실패
  - `404 NOT_FOUND` — 사용자 없음 (예: `USER_NOT_FOUND`)

---

#### curl 예시

GET 내 프로필:
```bash
curl -X GET "https://api.example.com/api/v1/users/me" \
  -H "Authorization: Bearer {accessToken}" -i
```

온보딩 완료 요청:
```bash
curl -X POST "https://api.example.com/api/v1/users/me/onboarding" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{"name":"홍길동","birthDate":"2000-01-01","termsAgreed":true}' -i
```

### `POST /api/v1/users/me/onboarding`

- 설명: 온보딩(추가 정보 입력) — 가입 완료 처리
- 인증: Required
- Request (application/json)

```json
{
  "name": "홍길동",
  "birthDate": "2000-01-01",
  "termsAgreed": true
}
```

- Response (200)

```json
{
  "status": 200,
  "code": "SUC_ONBOARDING_COMPLETED",
  "data": null,
  "message": "온보딩이 완료되었습니다."
}
```

- 오류
  - `400 BAD_REQUEST` — 검증 실패 (예: `VALIDATION_ERROR`)
  - `401 UNAUTHORIZED` — 인증 실패

---

### `POST /api/v1/users/withdrawal`

- 설명: 회원 탈퇴 (현재 하드 삭제 정책)
- 인증: Required
- Request: 없음
- Response (200)

```json
{
  "status": 200,
  "code": "SUC_USER_WITHDRAWN",
  "data": null,
  "message": "회원 탈퇴가 완료되었습니다."
}
```

- 주의: 하드 삭제 정책 — 삭제 후 조회 시 `USER_NOT_FOUND` 처리
- 오류: `401`, `500`

---

## 프로필 이미지

`Base URL`: `/api/v1/users/me`

### `POST /api/v1/users/me/profile-image`

- 설명: 내 프로필 이미지 업로드/교체
- 인증: Required
- Content-Type: `multipart/form-data`
- Multipart:
  - `file`: file (필수)
- Response (200)

```json
{
  "status": 200,
  "code": "SUC_PROFILE_IMAGE_UPLOADED",
  "data": {
    "storageKey": "users/profile/xyz.jpg",
    "viewUrl": "https://...presigned...",
    "ttlSeconds": 60
  },
  "message": "업로드 성공"
}
```

- 오류: `400`(파일 누락/형식), `413`(파일 크기 초과), `401`

프로필 이미지 업로드(curl multipart) 예시:
```bash
curl -X POST "https://api.example.com/api/v1/users/me/profile-image" \
  -H "Authorization: Bearer {accessToken}" \
  -F "file=@/path/to/profile.jpg" -i
```

### `GET /api/v1/users/me/profile-image`

- 설명: 내 프로필 이미지 조회
- 인증: Required
- Response (200)
  - 이미지가 없으면 `data`는 `{ "storageKey": null, "viewUrl": null, "ttlSeconds": null }`

### `DELETE /api/v1/users/me/profile-image`

- 설명: 내 프로필 이미지 삭제
- 인증: Required
- Response (200)

---

## 에러코드(대표)

- `VALIDATION_ERROR` (400)
- `AUTH_UNAUTHORIZED` (401)
- `USER_NOT_FOUND` (404)
- `FILE_INVALID` / `FILE_TOO_LARGE` (400/413)
- `INTERNAL_ERROR` (500)

---

## 적용 참고

- 실제 에러코드와 메시지는 레포의 `ErrorCode` enum을 참조해 교체하세요.
