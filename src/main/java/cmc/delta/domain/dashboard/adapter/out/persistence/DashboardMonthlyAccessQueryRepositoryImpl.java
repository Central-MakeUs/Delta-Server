package cmc.delta.domain.dashboard.adapter.out.persistence;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.dashboard.application.dto.DashboardDailyAccessItem;
import cmc.delta.domain.dashboard.application.port.out.DashboardMonthlyAccessQueryPort;
import cmc.delta.domain.stats.model.QUserDailyAccess;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardMonthlyAccessQueryRepositoryImpl implements DashboardMonthlyAccessQueryPort {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<DashboardDailyAccessItem> findDailyAccessByMonth(YearMonth yearMonth) {
		QUserDailyAccess access = QUserDailyAccess.userDailyAccess;
		LocalDate start = yearMonth.atDay(1);
		LocalDate end = yearMonth.atEndOfMonth();

		return queryFactory
			.select(constructor(DashboardDailyAccessItem.class,
				access.accessDate,
				access.userId.countDistinct()))
			.from(access)
			.where(access.accessDate.between(start, end))
			.groupBy(access.accessDate)
			.orderBy(access.accessDate.asc())
			.fetch();
	}
}
