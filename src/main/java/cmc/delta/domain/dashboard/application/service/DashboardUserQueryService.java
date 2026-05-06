package cmc.delta.domain.dashboard.application.service;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.port.in.GetDashboardUsersUseCase;
import cmc.delta.domain.dashboard.application.port.out.DashboardUserQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUserQueryService implements GetDashboardUsersUseCase {

	private final DashboardUserQueryPort dashboardUserQueryPort;

	@Override
	public DashboardUsersResponse getUsers(int page, int size) {
		List<DashboardUserItem> content = dashboardUserQueryPort.findUsers(page, size);
		long totalElements = dashboardUserQueryPort.countUsers();
		int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
		return new DashboardUsersResponse(content, page, size, totalElements, totalPages);
	}
}
