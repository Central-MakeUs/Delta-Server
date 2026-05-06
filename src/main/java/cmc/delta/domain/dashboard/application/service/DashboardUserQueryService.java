package cmc.delta.domain.dashboard.application.service;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.port.in.GetDashboardUsersUseCase;
import cmc.delta.domain.dashboard.application.port.out.DashboardUserQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUserQueryService implements GetDashboardUsersUseCase {

	private final DashboardUserQueryPort dashboardUserQueryPort;

	@Override
	public DashboardUsersResponse getUsers(Pageable pageable) {
		List<DashboardUserItem> content = dashboardUserQueryPort.findUsers(pageable);
		long totalElements = dashboardUserQueryPort.countUsers();
		int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + pageable.getPageSize() - 1) / pageable.getPageSize());
		return new DashboardUsersResponse(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements, totalPages);
	}
}
