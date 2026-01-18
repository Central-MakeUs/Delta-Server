package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.application.query.support.ProblemQueryTestModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemQueryServiceImplTest {

	@Test
	@DisplayName("내 오답카드 목록 조회: 정상일 때 validate → repo → storage → mapper 흐름으로 조립된다")
	void getMyProblemCardList_success() {
		// given
		ProblemQueryTestModule m = ProblemQueryTestModule.successCase();

		// when
		m.call();

		// then
		m.thenSuccess();
	}

	@Test
	@DisplayName("내 오답카드 목록 조회: pagination 검증 실패면 repo/storage/mapper는 호출되지 않는다")
	void getMyProblemCardList_invalidPagination() {
		// given
		ProblemQueryTestModule m = ProblemQueryTestModule.invalidPaginationCase();

		// when
		m.callExpectThrow();

		// then
		m.thenInvalidPagination();
	}
}
