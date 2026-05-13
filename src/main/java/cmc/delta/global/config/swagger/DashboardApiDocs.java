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

	public static final String GET_PROBLEMS = """
		관리자 대시보드의 문제 등록 현황 목록을 조회합니다.

		요청 파라미터:
		- page: 페이지 번호 (기본값 0)
		- size: 페이지 크기 (기본값 20, 최대 100)

		응답 필드:
		- content: 문제 목록
		  - problemId: 문제 ID
		  - problemName: 문제명 (problem의 final unit 명)
		  - unitName: 단원명 (final unit의 parent unit 명, root unit이면 null)
		  - problemType: 문제 유형명
		  - aiSolutionCount: AI 풀이 요청 API 누적 호출 수 (캐시 적중 포함, 호출마다 +1)
		  - viewCount: 문제 상세조회 API 누적 호출 수
		  - registeredAt: 문제 등록 일시
		  - wrongAnswerCompleted: 오답 완료 여부 (completedAt 존재 여부)
		  - userRole: 등록한 사용자 권한 (USER / ADMIN)
		- page: 현재 페이지 번호
		- size: 페이지 크기
		- totalElements: 전체 문제 수
		- totalPages: 전체 페이지 수
		""";
}
