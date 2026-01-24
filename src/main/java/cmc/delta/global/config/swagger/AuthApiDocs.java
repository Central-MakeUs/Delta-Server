package cmc.delta.global.config.swagger;

public final class AuthApiDocs {

	private AuthApiDocs() {
	}

	public static final String KAKAO_LOGIN = """
		카카오 인가코드로 로그인합니다.

		요청:
		- POST /api/v1/auth/kakao
		- body: { "code": "카카오 인가코드" }

		동작:
		- 인가코드(code)로 카카오 토큰 교환 → 유저 프로필 조회
		- (provider=KAKAO, providerUserId) 기준으로 소셜 계정 연동/신규 생성
		- Access/Refresh 토큰을 응답 헤더로 내려줍니다.

		응답:
		- header:
		  - Authorization: Bearer {accessToken}
		  - X-Refresh-Token: {refreshToken} (있을 때만)
		  - Access-Control-Expose-Headers: Authorization, X-Refresh-Token, X-Trace-Id
		- body.data:
		  - email, nickname, isNewUser
		""";

	public static final String APPLE_FORM_POST_CALLBACK = """
		애플 로그인 콜백(form_post) 처리 후 로그인합니다. (서버 콜백 전용)

		요청:
		- POST /api/v1/auth/apple
		- Content-Type: application/x-www-form-urlencoded
		- form:
		  - code: 애플 authorization code (필수)
		  - user: 애플 user JSON (선택, 보통 최초 1회만 내려올 수 있음)

		주의:
		- 이 엔드포인트는 일반적인 앱/Swagger에서 직접 호출하는 용도가 아니라,
		  Apple authorize 과정에서 redirect_uri로 지정되어 Apple이 직접 POST하는 콜백입니다.
		- redirect_uri는 Apple Developer Console의 Return URL과 100% 동일해야 합니다.

		동작:
		- code로 애플 토큰 교환 → id_token 검증 → sub(=providerUserId) 추출
		- user JSON이 있으면 name/email을 파싱하여 프로필 저장에 활용
		- (provider=APPLE, providerUserId) 기준으로 소셜 계정 연동/신규 생성
		- Access/Refresh 토큰을 응답 헤더로 내려줍니다.

		응답:
		- header:
		  - Authorization: Bearer {accessToken}
		  - X-Refresh-Token: {refreshToken} (있을 때만)
		  - Access-Control-Expose-Headers: Authorization, X-Refresh-Token, X-Trace-Id
		- body.data:
		  - email, nickname, isNewUser
		""";

	public static final String APPLE_QUERY_CODE_CALLBACK = """
		애플 인가코드(code)를 텍스트로 반환합니다. (코드 복사용/디버그용)

		요청:
		- GET /api/v1/auth/oauth/apple/callback?code=...

		용도:
		- response_mode=query 형태로 authorize를 호출했을 때,
		  브라우저 주소창에 나타난 code를 서버에서 그대로 출력하여 복사하기 쉽도록 합니다.
		- Swagger에서 code를 직접 넣어 테스트할 때(아래 APPLE_LOGIN_BY_CODE) 함께 사용합니다.

		주의:
		- 운영 로그인 플로우에는 보통 form_post 콜백(APPLE_FORM_POST_CALLBACK)을 사용합니다.
		""";

	public static final String APPLE_LOGIN_BY_CODE = """
		애플 인가코드(code)로 로그인합니다. (Swagger/테스트용)

		요청:
		- POST /api/v1/auth/apple/code
		- body: { "code": "애플 인가코드" }

		전제:
		- 보통 GET /api/v1/auth/oauth/apple/callback(코드 복사용)에서 code를 얻어
		  이 API에 입력하는 테스트 흐름에 사용합니다.

		동작:
		- code로 애플 토큰 교환 → id_token 검증 → sub(=providerUserId) 추출
		- (provider=APPLE, providerUserId) 기준으로 소셜 계정 연동/로그인 처리
		- Access/Refresh 토큰을 응답 헤더로 내려줍니다.

		응답:
		- header:
		  - Authorization: Bearer {accessToken}
		  - X-Refresh-Token: {refreshToken} (있을 때만)
		  - Access-Control-Expose-Headers: Authorization, X-Refresh-Token, X-Trace-Id
		- body.data:
		  - email, nickname, isNewUser
		""";
}
