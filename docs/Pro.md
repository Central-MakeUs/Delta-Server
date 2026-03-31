# Pro Domain API

Base URL: `/api/v1/pro`

관련 코드:
- `src/main/java/cmc/delta/domain/pro/adapter/in/web/ProCheckoutClickController.java`

| ID | Method | Path | 인증 | 기능 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| PRO-01 | POST | `/api/v1/pro/checkout-click` | Required | Pro 결제 버튼 클릭 기록 | - | `ApiResponse<Void>` |
| PRO-02 | GET | `/api/v1/pro/checkout-click/stats` | Public(컨트롤러 시그니처 기준) | Pro 결제 버튼 클릭 통계 조회 | - | `ApiResponse<ProCheckoutClickStatsResponse>` |
