package cmc.delta.domain.dashboard.application.service;

import cmc.delta.domain.dashboard.application.dto.DashboardDailyAccessItem;
import cmc.delta.domain.dashboard.application.dto.DashboardMonthlyAccessResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.port.in.DashboardQueryUseCase;
import cmc.delta.domain.dashboard.application.port.out.DashboardMonthlyAccessQueryPort;
import cmc.delta.domain.dashboard.application.port.out.DashboardUserQueryPort;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService implements DashboardQueryUseCase {

	private final DashboardUserQueryPort dashboardUserQueryPort;
	private final DashboardMonthlyAccessQueryPort dashboardMonthlyAccessQueryPort;

	@Override
	public DashboardUsersResponse getUsers(Pageable pageable) {
		List<DashboardUserItem> content = dashboardUserQueryPort.findUsers(pageable);
		long totalElements = dashboardUserQueryPort.countUsers();
		int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + pageable.getPageSize() - 1) / pageable.getPageSize());
		return new DashboardUsersResponse(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements, totalPages);
	}

	@Override
	public DashboardMonthlyAccessResponse getMonthlyAccess(YearMonth yearMonth) {
		List<DashboardDailyAccessItem> dailyAccess = dashboardMonthlyAccessQueryPort.findDailyAccessByMonth(yearMonth);
		return new DashboardMonthlyAccessResponse(yearMonth.getYear(), yearMonth.getMonthValue(), dailyAccess);
	}
}
