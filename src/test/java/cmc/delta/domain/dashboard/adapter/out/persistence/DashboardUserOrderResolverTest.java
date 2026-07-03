package cmc.delta.domain.dashboard.adapter.out.persistence;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import cmc.delta.domain.user.model.QUser;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardUserOrderResolverTest {

	private final DashboardUserOrderResolver resolver = new DashboardUserOrderResolver();
	private final QUser user = QUser.user;
	private final NumberExpression<Long> accessCount = Expressions.numberPath(Long.class, "accessCount");
	private final DateExpression<LocalDate> lastAccessDate = Expressions.datePath(LocalDate.class, "lastAccessDate");
	private final NumberExpression<Long> problemCount = Expressions.numberPath(Long.class, "problemCount");

	@Test
	@DisplayName("정렬: 기본값은 ID DESC")
	void resolve_defaultOrdersByIdDesc() {
		// when
		OrderSpecifier<?>[] orders = resolver.resolve(null, null, user, accessCount, lastAccessDate, problemCount);

		// then
		assertThat(orders).hasSize(1);
		assertThat(orders[0].getTarget().toString()).contains("user.id");
		assertThat(orders[0].getOrder()).isEqualTo(Order.DESC);
	}

	@Test
	@DisplayName("정렬: NICKNAME ASC 후 user.id DESC")
	void resolve_nicknameAsc() {
		// when
		OrderSpecifier<?>[] orders = resolver.resolve(
			DashboardUserSortBy.NICKNAME,
			DashboardSortDirection.ASC,
			user,
			accessCount,
			lastAccessDate,
			problemCount);

		// then
		assertThat(orders).hasSize(2);
		assertThat(orders[0].getTarget().toString()).contains("nickname");
		assertThat(orders[0].getOrder()).isEqualTo(Order.ASC);
		assertThat(orders[1].getTarget().toString()).contains("user.id");
		assertThat(orders[1].getOrder()).isEqualTo(Order.DESC);
	}

	@Test
	@DisplayName("정렬: ACCESS_COUNT DESC 후 user.id DESC")
	void resolve_accessCountDesc() {
		// when
		OrderSpecifier<?>[] orders = resolver.resolve(
			DashboardUserSortBy.ACCESS_COUNT,
			DashboardSortDirection.DESC,
			user,
			accessCount,
			lastAccessDate,
			problemCount);

		// then
		assertThat(orders).hasSize(2);
		assertThat(orders[0].getTarget().toString()).contains("accessCount");
		assertThat(orders[0].getOrder()).isEqualTo(Order.DESC);
		assertThat(orders[1].getTarget().toString()).contains("user.id");
		assertThat(orders[1].getOrder()).isEqualTo(Order.DESC);
	}

	@Test
	@DisplayName("정렬: LAST_ACCESS_DATE ASC/DESC 모두 null last 플래그를 먼저 둔다")
	void resolve_lastAccessDateUsesNullLastFlag() {
		// when
		OrderSpecifier<?>[] ascOrders = resolver.resolve(
			DashboardUserSortBy.LAST_ACCESS_DATE,
			DashboardSortDirection.ASC,
			user,
			accessCount,
			lastAccessDate,
			problemCount);
		OrderSpecifier<?>[] descOrders = resolver.resolve(
			DashboardUserSortBy.LAST_ACCESS_DATE,
			DashboardSortDirection.DESC,
			user,
			accessCount,
			lastAccessDate,
			problemCount);

		// then
		assertThat(ascOrders).hasSize(3);
		assertThat(descOrders).hasSize(3);
		assertThat(ascOrders[0].getTarget().toString()).contains("is null");
		assertThat(descOrders[0].getTarget().toString()).contains("is null");
		assertThat(ascOrders[0].getOrder()).isEqualTo(Order.ASC);
		assertThat(descOrders[0].getOrder()).isEqualTo(Order.ASC);
		assertThat(ascOrders[1].getOrder()).isEqualTo(Order.ASC);
		assertThat(descOrders[1].getOrder()).isEqualTo(Order.DESC);
	}

	@Test
	@DisplayName("정렬: PROBLEM_COUNT ASC 후 user.id DESC")
	void resolve_problemCountAsc() {
		// when
		OrderSpecifier<?>[] orders = resolver.resolve(
			DashboardUserSortBy.PROBLEM_COUNT,
			DashboardSortDirection.ASC,
			user,
			accessCount,
			lastAccessDate,
			problemCount);

		// then
		assertThat(orders).hasSize(2);
		assertThat(orders[0].getTarget().toString()).contains("problemCount");
		assertThat(orders[0].getOrder()).isEqualTo(Order.ASC);
		assertThat(orders[1].getTarget().toString()).contains("user.id");
		assertThat(orders[1].getOrder()).isEqualTo(Order.DESC);
	}
}
