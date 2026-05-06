package cmc.delta.global.config.swagger;

public final class DashboardApiDocs {

	private DashboardApiDocs() {}

	public static final String GET_USERS = """
		관리자 대시보드의 사용자 관리 목록을 조회합니다.

		응답 필드:
		- content: 사용자 목록
		  - userId: 사용자 ID
		  - nickname: 닉네임
		  - userRole: 사용자 권한 (USER / ADMIN)
		  - accessCount: 누적 접속 일수
		  - lastAccessDate: 마지막 접속일
		  - problemCount: 풀이한 문제 수
		- page: 현재 페이지 번호
		- size: 페이지 크기
		- totalElements: 전체 사용자 수
		- totalPages: 전체 페이지 수
		""";
}
