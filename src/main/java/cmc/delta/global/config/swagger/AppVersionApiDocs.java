package cmc.delta.global.config.swagger;

public final class AppVersionApiDocs {

	private AppVersionApiDocs() {}

	public static final String GET_APP_VERSION = """
		앱 버전 정책을 조회합니다.

		사용 시점:
		- 앱 실행 직후 또는 로그인 전, 클라이언트가 현재 앱 버전을 서버 정책과 비교할 때 호출합니다.
		- 인증 없이 호출할 수 있습니다.

		요청 파라미터:
		- currentVersion: 현재 설치된 앱 버전입니다. major.minor.patch 형식으로 전달합니다. 예) 1.2.3

		응답 필드:
		- minimumVersion: 서비스 이용을 허용하는 최소 앱 버전입니다.
		- latestVersion: 스토어에 배포된 최신 앱 버전입니다.
		- forceUpdate: currentVersion이 minimumVersion보다 낮으면 true입니다. 클라이언트는 강제 업데이트 화면을 표시해야 합니다.
		- updateAvailable: currentVersion이 latestVersion보다 낮으면 true입니다. 클라이언트는 선택 업데이트 안내를 표시할 수 있습니다.

		에러 케이스:
		- currentVersion 누락 또는 형식 오류 → INVALID_REQUEST
		""";
}
