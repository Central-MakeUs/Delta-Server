package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import com.querydsl.core.BooleanBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemListPredicateBuilderTest {

	private final ProblemListPredicateBuilder builder = new ProblemListPredicateBuilder();

	@Test
	@DisplayName("문제 목록 where: 기본으로 userId 조건이 포함됨")
	void buildMainWhere_alwaysHasUserId() {
		// given
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(null, null, null, ProblemListSort.RECENT,
			ProblemStatusFilter.ALL);

		// when
		BooleanBuilder where = builder.buildMainWhere(10L, cond, p);

		// then
		assertThat(where.getValue().toString()).contains("problem.user.id").contains("10");
	}

	@Test
	@DisplayName("문제 목록 where: unit/type/subject 필터가 있으면 조건이 추가됨")
	void buildMainWhere_whenFiltersPresent_thenAddsConditions() {
		// given
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition base = new ProblemListCondition(null, null, null, ProblemListSort.RECENT,
			ProblemStatusFilter.ALL);
		ProblemListCondition cond = new ProblemListCondition("S1", "U1", "T1", ProblemListSort.RECENT,
			ProblemStatusFilter.ALL);

		// when
		String baseExpr = builder.buildMainWhere(10L, base, p).getValue().toString();
		BooleanBuilder where = builder.buildMainWhere(10L, cond, p);
		String expr = where.getValue().toString();

		// then
		assertThat(expr).isNotEqualTo(baseExpr);
		assertThat(expr.length()).isGreaterThan(baseExpr.length());
	}

	@Test
	@DisplayName("문제 목록 where: status=SOLVED면 completedAt is not null")
	void buildMainWhere_whenSolved_thenCompletedAtNotNull() {
		// given
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(null, null, null, ProblemListSort.RECENT,
			ProblemStatusFilter.SOLVED);

		// when
		BooleanBuilder where = builder.buildMainWhere(10L, cond, p);

		// then
		assertThat(where.getValue().toString()).contains("problem.completedAt").contains("is not null");
	}

	@Test
	@DisplayName("문제 목록 where: status=UNSOLVED면 completedAt is null")
	void buildMainWhere_whenUnsolved_thenCompletedAtNull() {
		// given
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(null, null, null, ProblemListSort.RECENT,
			ProblemStatusFilter.UNSOLVED);

		// when
		BooleanBuilder where = builder.buildMainWhere(10L, cond, p);

		// then
		assertThat(where.getValue().toString()).contains("problem.completedAt").contains("is null");
	}

	@Test
	@DisplayName("문제 목록 count base where: status 조건은 의도적으로 제외")
	void buildCountBaseWhere_excludesStatusFilter() {
		// given
		ProblemListCondition cond = new ProblemListCondition("S1", null, null, ProblemListSort.RECENT,
			ProblemStatusFilter.SOLVED);

		// when
		BooleanBuilder where = builder.buildCountBaseWhere(10L, cond,
			cmc.delta.domain.problem.model.problem.QProblem.problem);

		// then
		assertThat(where.getValue().toString()).doesNotContain("completedAt");
	}
}
