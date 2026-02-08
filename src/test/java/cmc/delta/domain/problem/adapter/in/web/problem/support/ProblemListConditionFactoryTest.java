package cmc.delta.domain.problem.adapter.in.web.problem.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemListConditionFactoryTest {

	private final ProblemListConditionFactory factory = new ProblemListConditionFactory();

	@Test
	@DisplayName("목록 조건 변환: id들은 trimToNull 처리되고 sort/status는 기본값")
	void from_trimsAndDefaults() {
		// given
		MyProblemListRequest req = new MyProblemListRequest(List.of("  "), List.of("U1 "), List.of(), null, null, null,
			null);

		// when
		ProblemListCondition cond = factory.from(req);

		// then
		assertThat(cond.subjectIds()).isEmpty();
		assertThat(cond.unitIds()).containsExactly("U1");
		assertThat(cond.typeIds()).isEmpty();
		assertThat(cond.sort()).isEqualTo(ProblemListSort.RECENT);
		assertThat(cond.status()).isEqualTo(ProblemStatusFilter.ALL);
	}

	@Test
	@DisplayName("목록 조건 변환: Swagger가 배열을 JSON 문자열로 보내도 정상적으로 확장된다")
	void from_expandsJsonArrayLike() {
		// given
		MyProblemListRequest req = new MyProblemListRequest(
			List.of("[\"U_GEOM\"]"),
			List.of("[\"U_GM_CONIC\",\"U_GM_VECTOR\"]"),
			List.of(),
			null,
			null,
			null,
			null);

		// when
		ProblemListCondition cond = factory.from(req);

		// then
		assertThat(cond.subjectIds()).containsExactly("U_GEOM");
		assertThat(cond.unitIds()).containsExactly("U_GM_CONIC", "U_GM_VECTOR");
	}
}
