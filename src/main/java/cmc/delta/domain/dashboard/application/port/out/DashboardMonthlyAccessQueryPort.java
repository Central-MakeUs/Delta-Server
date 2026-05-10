package cmc.delta.domain.dashboard.application.port.out;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public interface DashboardMonthlyAccessQueryPort {

	Map<LocalDate, Long> findDailyAccessByMonth(YearMonth yearMonth);

	Map<LocalDate, Long> findDailyNewUsersByMonth(YearMonth yearMonth);
}
