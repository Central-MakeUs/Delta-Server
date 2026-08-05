package cmc.delta.domain.dashboard.adapter.out.persistence;

import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import cmc.delta.domain.user.model.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class DashboardUserOrderResolver {

	public OrderSpecifier<?>[] resolve(
		DashboardUserSortBy sortBy,
		DashboardSortDirection sortDirection,
		QUser user,
		NumberExpression<Long> accessCount,
		DateExpression<LocalDate> lastAccessDate,
		NumberExpression<Long> problemCount) {
		DashboardUserSortBy resolvedSortBy = (sortBy == null) ? DashboardUserSortBy.ID : sortBy;
		DashboardSortDirection resolvedDirection = (sortDirection == null) ? DashboardSortDirection.DESC
			: sortDirection;

		return resolveOrderSpecifiers(resolvedSortBy, resolvedDirection, user, accessCount, lastAccessDate,
			problemCount);
	}

	private OrderSpecifier<?>[] resolveOrderSpecifiers(
		DashboardUserSortBy resolvedSortBy,
		DashboardSortDirection resolvedDirection,
		QUser user,
		NumberExpression<Long> accessCount,
		DateExpression<LocalDate> lastAccessDate,
		NumberExpression<Long> problemCount) {
		return switch (resolvedSortBy) {
			case ID -> new OrderSpecifier<?>[] {orderUserId(user, resolvedDirection)};
			case NICKNAME -> new OrderSpecifier<?>[] {applyDirection(user.nickname, resolvedDirection), user.id.desc()};
			case ACCESS_COUNT ->
				new OrderSpecifier<?>[] {applyDirection(accessCount, resolvedDirection), user.id.desc()};
			case LAST_ACCESS_DATE -> new OrderSpecifier<?>[] {
				lastAccessDate.isNull().asc(),
				applyDirection(lastAccessDate, resolvedDirection),
				user.id.desc()
			};
			case PROBLEM_COUNT ->
				new OrderSpecifier<?>[] {applyDirection(problemCount, resolvedDirection), user.id.desc()};
		};
	}

	private OrderSpecifier<Long> orderUserId(QUser user, DashboardSortDirection sortDirection) {
		if (sortDirection == DashboardSortDirection.ASC) {
			return user.id.asc();
		}
		return user.id.desc();
	}

	private OrderSpecifier<?> applyDirection(
		ComparableExpressionBase<?> expression,
		DashboardSortDirection sortDirection) {
		if (sortDirection == DashboardSortDirection.ASC) {
			return expression.asc();
		}
		return expression.desc();
	}
}
