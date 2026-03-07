# Global API

관련 코드:
- `src/main/java/cmc/delta/global/api/storage/StorageController.java`
- `src/main/java/cmc/delta/global/health/HealthController.java`

## Storage

Base URL: `/api/v1/storage`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| GLOB-01 | POST | `/api/v1/storage/images` | Public(컨트롤러 시그니처 기준) | 이미지 업로드(S3) | multipart `file`, query `directory?` | `ApiResponse<StorageUploadData>` |
| GLOB-02 | GET | `/api/v1/storage/images/presigned-get` | Public(컨트롤러 시그니처 기준) | Presigned GET URL 발급 | query `key`, `ttlSeconds?` | `ApiResponse<StoragePresignedGetData>` |
| GLOB-03 | DELETE | `/api/v1/storage/images` | Public(컨트롤러 시그니처 기준) | 이미지 삭제(S3) | query `key` | `ApiResponse<Void>` |

## Health

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| GLOB-04 | GET | `/health` | Public | 서비스 헬스체크 | - | `ResponseEntity<String>` (`"OK"`) |
