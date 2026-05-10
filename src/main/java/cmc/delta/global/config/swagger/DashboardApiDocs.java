package cmc.delta.global.config.swagger;

public final class DashboardApiDocs {

	private DashboardApiDocs() {}

	public static final String GET_MONTHLY_ACCESS = """
		특정 달의 일별 접속자 수를 조회합니다.

		요청 파라미터:
		- year: 조회 연도 (기본값: 현재 연도)
		- month: 조회 월 1~12 (기본값: 현재 월)

		응답 필드:
		- year: 조회 연도
		- month: 조회 월
		- dailyAccess: 일별 접속자 목록 (접속자가 없는 날은 포함되지 않음)
		  - date: 날짜
		  - count: 해당 날의 유니크 접속자 수
		""";

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
