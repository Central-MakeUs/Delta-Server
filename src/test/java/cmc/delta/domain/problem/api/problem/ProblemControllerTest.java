package cmc.delta.domain.problem.api.problem;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.support.ProblemControllerTestModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemControllerTest {

	@Test
	@DisplayName("오답카드 생성: principal.userId와 request를 ProblemService로 위임한다")
	void createWrongAnswerCard_delegatesToService() {
		// given
		ProblemControllerTestModule m = ProblemControllerTestModule.createCase();

		// when
		m.callCreate();

		// then
		m.thenCreateCalled();
	}

	@Test
	@DisplayName("내 오답카드 목록 조회: condition/pageable을 구성해 ProblemQueryService로 위임한다")
	void getMyProblemList_delegatesToQueryService() {
		// given
		ProblemControllerTestModule m = ProblemControllerTestModule.listCase();

		// when
		m.callList("S1", "U1", "T1", ProblemListSort.RECENT, 0, 20);

		// then
		m.thenListCalled();
	}
}
