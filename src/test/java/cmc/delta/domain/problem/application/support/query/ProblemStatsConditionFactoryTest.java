package cmc.delta.domain.problem.application.support.query;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemStatsRequest;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemStatsConditionFactoryTest {

	private final ProblemStatsConditionFactory factory = new ProblemStatsConditionFactory();

	@Test
	@DisplayName("통계 조건 변환: id들은 trimToNull 처리되고 sort는 기본값")
	void from_trimsAndDefaults() {
		// given
		ProblemStatsRequest req = new ProblemStatsRequest(" S1 ", "  ", null, null);

		// when
		ProblemStatsCondition cond = factory.from(req);

		// then
		assertThat(cond.subjectId()).isEqualTo("S1");
		assertThat(cond.unitId()).isNull();
		assertThat(cond.typeId()).isNull();
		assertThat(cond.sort()).isEqualTo(ProblemStatsSort.DEFAULT);
	}
}
