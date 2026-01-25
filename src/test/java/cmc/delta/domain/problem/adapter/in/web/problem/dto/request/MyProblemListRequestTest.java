package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MyProblemListRequestTest {

	@Test
	@DisplayName("MyProblemListRequest: sort/status/page/size 기본값 및 상한/하한 적용")
	void defaultsAndBounds() {
		// when
		MyProblemListRequest req1 = new MyProblemListRequest(null, null, null, null, null, null, null);
		MyProblemListRequest req2 = new MyProblemListRequest(null, null, null, ProblemListSort.OLDEST, ProblemStatusFilter.SOLVED, -1, 200);

		// then
		assertThat(req1.sort()).isEqualTo(ProblemListSort.RECENT);
		assertThat(req1.status()).isEqualTo(ProblemStatusFilter.ALL);
		assertThat(req1.page()).isEqualTo(0);
		assertThat(req1.size()).isEqualTo(20);

		assertThat(req2.sort()).isEqualTo(ProblemListSort.OLDEST);
		assertThat(req2.status()).isEqualTo(ProblemStatusFilter.SOLVED);
		assertThat(req2.page()).isEqualTo(0);
		assertThat(req2.size()).isEqualTo(100);
	}
}
