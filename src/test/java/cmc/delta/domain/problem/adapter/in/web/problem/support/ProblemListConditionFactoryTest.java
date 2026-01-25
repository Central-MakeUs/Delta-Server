package cmc.delta.domain.problem.adapter.in.web.problem.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemListConditionFactoryTest {

	private final ProblemListConditionFactory factory = new ProblemListConditionFactory();

	@Test
	@DisplayName("목록 조건 변환: id들은 trimToNull 처리되고 sort/status는 기본값")
	void from_trimsAndDefaults() {
		// given
		MyProblemListRequest req = new MyProblemListRequest("  ", "U1 ", null, null, null, null, null);

		// when
		ProblemListCondition cond = factory.from(req);

		// then
		assertThat(cond.subjectId()).isNull();
		assertThat(cond.unitId()).isEqualTo("U1");
		assertThat(cond.typeId()).isNull();
		assertThat(cond.sort()).isEqualTo(ProblemListSort.RECENT);
		assertThat(cond.status()).isEqualTo(ProblemStatusFilter.ALL);
	}
}
