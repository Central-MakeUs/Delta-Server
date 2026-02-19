package cmc.delta.global.config.swagger;

public final class UserApiDocs {

	private UserApiDocs() {}

	public static final String COMPLETE_ONBOARDING = """
		추가 정보를 입력하여 회원가입을 완료합니다.

		사용 시점:
		- 카카오 로그인 직후 생성된 유저(UserStatus = ONBOARDING_REQUIRED)가 호출합니다.

		요청 필드:
		- nickname: 사용자 닉네임
		- termsAgreed: 필수 약관 동의 여부 (true 필수)

		동작/상태 정책(UserStatus):
		- ONBOARDING_REQUIRED: 추가 정보 입력 전 상태 (대부분 API 접근 제한)
		- ACTIVE: 가입 완료 상태
		- WITHDRAWN: 탈퇴 상태 (모든 API 접근 불가)
		- 상태 전이: ONBOARDING_REQUIRED → ACTIVE (본 API), ACTIVE → WITHDRAWN (탈퇴 API)

		에러 케이스:
		- 필수 필드 누락/형식 오류 또는 약관 미동의 → INVALID_REQUEST
		- 탈퇴한 사용자 → USER_WITHDRAWN
		""";
}
