package cmc.delta.global.config.swagger;

public final class ProApiDocs {

	private ProApiDocs() {}

	public static final String TRACK_CHECKOUT_CLICK = """
		Pro 페이지 하단 '결제하러가기' 버튼 클릭을 기록합니다.

		사용 목적:
		- 아직 준비 중인 기능(결제/Pro) 버튼의 수요를 측정하기 위한 클릭 이벤트 카운팅

		집계 방식:
		- 클릭할 때마다 1건 저장 (총 클릭수 집계 가능)
		- uniqueUsers는 user_id distinct 기준으로 집계 (계정당 1회)
		""";

	public static final String CHECKOUT_CLICK_STATS = """
		Pro 페이지 하단 '결제하러가기' 버튼 클릭 통계를 조회합니다.

		응답 필드:
		- totalClicks: 전체 클릭 수 (클릭 이벤트 누적)
		- uniqueUsers: 클릭한 유니크 사용자 수 (계정당 1회 카운팅)
		""";
}
