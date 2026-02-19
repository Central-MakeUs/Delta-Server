package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemListOrderResolverTest {

	@Test
	@DisplayName("정렬: RECENT면 createdAt desc, id desc")
	void resolve_whenRecent_thenOrdersByCreatedAtDescAndIdDesc() {
		// given
		ProblemListCountExpressions countExpressions = mock(ProblemListCountExpressions.class);
		ProblemListOrderResolver resolver = new ProblemListOrderResolver(countExpressions);
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(List.of(), List.of(), List.of(), null,
			ProblemStatusFilter.ALL);

		// when
		OrderSpecifier<?>[] orders = resolver.resolve(10L, cond, p);

		// then
		assertThat(orders).hasSize(2);
		assertThat(orders[0].getTarget().toString()).contains("problem.createdAt");
		assertThat(orders[0].getOrder()).isEqualTo(Order.DESC);
		assertThat(orders[1].getTarget().toString()).contains("problem.id");
		assertThat(orders[1].getOrder()).isEqualTo(Order.DESC);
		verifyNoInteractions(countExpressions);
	}

	@Test
	@DisplayName("정렬: OLDEST면 createdAt asc, id asc")
	void resolve_whenOldest_thenOrdersByCreatedAtAscAndIdAsc() {
		// given
		ProblemListCountExpressions countExpressions = mock(ProblemListCountExpressions.class);
		ProblemListOrderResolver resolver = new ProblemListOrderResolver(countExpressions);
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(List.of(), List.of(), List.of(), ProblemListSort.OLDEST,
			ProblemStatusFilter.ALL);

		// when
		OrderSpecifier<?>[] orders = resolver.resolve(10L, cond, p);

		// then
		assertThat(orders).hasSize(2);
		assertThat(orders[0].getTarget().toString()).contains("problem.createdAt");
		assertThat(orders[0].getOrder()).isEqualTo(Order.ASC);
		assertThat(orders[1].getTarget().toString()).contains("problem.id");
		assertThat(orders[1].getOrder()).isEqualTo(Order.ASC);
		verifyNoInteractions(countExpressions);
	}

	@Test
	@DisplayName("정렬: UNIT_MOST면 subject count desc + createdAt desc + id desc")
	void resolve_whenUnitMost_thenUsesSubjectCountDesc() {
		// given
		ProblemListCountExpressions countExpressions = mock(ProblemListCountExpressions.class);
		ProblemListOrderResolver resolver = new ProblemListOrderResolver(countExpressions);
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(List.of(), List.of(), List.of(), ProblemListSort.UNIT_MOST,
			ProblemStatusFilter.ALL);

		NumberExpression<Long> subjectCnt = Expressions.numberPath(Long.class, "subjectCnt");
		when(countExpressions.subjectCount(eq(10L), eq(cond), any())).thenReturn(subjectCnt);

		// when
		OrderSpecifier<?>[] orders = resolver.resolve(10L, cond, p);

		// then
		assertThat(orders).hasSize(3);
		assertThat(orders[0].getTarget().toString()).contains("subjectCnt");
		assertThat(orders[0].getOrder()).isEqualTo(Order.DESC);
		assertThat(orders[1].getTarget().toString()).contains("problem.createdAt");
		assertThat(orders[1].getOrder()).isEqualTo(Order.DESC);
		assertThat(orders[2].getTarget().toString()).contains("problem.id");
		assertThat(orders[2].getOrder()).isEqualTo(Order.DESC);
		verify(countExpressions).subjectCount(eq(10L), eq(cond), any());
	}

	@Test
	@DisplayName("정렬: TYPE_LEAST면 type count asc + createdAt desc + id desc")
	void resolve_whenTypeLeast_thenUsesTypeCountAsc() {
		// given
		ProblemListCountExpressions countExpressions = mock(ProblemListCountExpressions.class);
		ProblemListOrderResolver resolver = new ProblemListOrderResolver(countExpressions);
		ProblemListQuerySupport.Paths p = ProblemListQuerySupport.Paths.create();
		ProblemListCondition cond = new ProblemListCondition(List.of(), List.of(), List.of(),
			ProblemListSort.TYPE_LEAST,
			ProblemStatusFilter.ALL);

		NumberExpression<Long> typeCnt = Expressions.numberPath(Long.class, "typeCnt");
		when(countExpressions.typeCount(eq(10L), eq(cond), any())).thenReturn(typeCnt);

		// when
		OrderSpecifier<?>[] orders = resolver.resolve(10L, cond, p);

		// then
		assertThat(orders).hasSize(3);
		assertThat(orders[0].getTarget().toString()).contains("typeCnt");
		assertThat(orders[0].getOrder()).isEqualTo(Order.ASC);
		verify(countExpressions).typeCount(eq(10L), eq(cond), any());
	}
}
