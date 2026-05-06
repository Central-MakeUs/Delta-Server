package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardDailyAccessItem;
import java.time.YearMonth;
import java.util.List;

public interface DashboardMonthlyAccessQueryPort {

	List<DashboardDailyAccessItem> findDailyAccessByMonth(YearMonth yearMonth);
}
