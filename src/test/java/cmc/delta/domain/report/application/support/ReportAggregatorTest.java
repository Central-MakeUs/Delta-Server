package cmc.delta.domain.report.application.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportAggregate.WeakArea;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportAggregatorTest {

	@Test
	@DisplayName("단원 합계를 계산하고 미해결이 있는 단원만 취약도 순으로 정렬한다")
	void aggregatesTotalsAndRanksWeakUnits() {
		List<ProblemUnitStatsRow> units = List.of(
			// 미해결 6/8 (75%) → score = 6 * 1.75 = 10.5
			new ProblemUnitStatsRow("s1", "수학", "u1", "함수", 2, 6, 8),
			// 미해결 2/5 (40%) → score = 2 * 1.4 = 2.8
			new ProblemUnitStatsRow("s1", "수학", "u2", "미분", 3, 2, 5),
			// 미해결 0 → 취약 목록에서 제외
			new ProblemUnitStatsRow("s1", "수학", "u3", "적분", 4, 0, 4));

		ReportAggregate aggregate = ReportAggregator.aggregate(units, List.of());

		assertThat(aggregate.totalCount()).isEqualTo(17);
		assertThat(aggregate.solvedCount()).isEqualTo(9);
		assertThat(aggregate.unsolvedCount()).isEqualTo(8);

		assertThat(aggregate.weakUnits()).hasSize(2);
		WeakArea top = aggregate.weakUnits().get(0);
		assertThat(top.name()).isEqualTo("함수");
		assertThat(top.unsolvedRatio()).isEqualTo(0.75);
		assertThat(aggregate.weakUnits().get(1).name()).isEqualTo("미분");
	}

	@Test
	@DisplayName("유형도 동일하게 취약도 순으로 정렬한다")
	void ranksWeakTypes() {
		List<ProblemTypeStatsRow> types = List.of(
			new ProblemTypeStatsRow("t1", "계산실수", 1, 1, 2),
			new ProblemTypeStatsRow("t2", "개념이해", 0, 5, 5));

		ReportAggregate aggregate = ReportAggregator.aggregate(List.of(), types);

		assertThat(aggregate.weakTypes()).extracting(WeakArea::name)
			.containsExactly("개념이해", "계산실수");
	}
}
