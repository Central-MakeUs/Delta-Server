package cmc.delta.global.config.swagger;

public final class AuthApiDocs {

	private AuthApiDocs() {}

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

	public static final String APPLE_EXCHANGE = """
		애플 로그인 시 발급된 1회용 loginKey를 교환하여 Access/Refresh 토큰을 전달합니다.

		요청:
		- POST /api/v1/auth/apple/exchange
		- query/form: loginKey (프론트가 Apple redirect 이후 전달받은 loginKey)

		동작:
		- Redis에 저장된 loginKey를 조회(소비)합니다. 유효하지 않거나 만료된 경우 에러를 반환합니다.
		- 저장된 토큰을 응답 헤더(Authorization, X-Refresh-Token)로 내려줍니다.

		응답:
		- header:
		  - Authorization: Bearer {accessToken}
		  - X-Refresh-Token: {refreshToken} (있을 때만)
		  - Access-Control-Expose-Headers: Authorization, X-Refresh-Token, X-Trace-Id
		- body.data:
		  - email, nickname, isNewUser
		""";
}
