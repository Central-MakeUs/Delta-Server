package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemStatsRequestTest {

	@Test
	@DisplayName("ProblemStatsRequest: sort가 null이면 DEFAULT")
	void defaultsSort() {
		// when
		ProblemStatsRequest req = new ProblemStatsRequest(null, null, null, null);

		// then
		assertThat(req.sort()).isEqualTo(ProblemStatsSort.DEFAULT);
	}
}
