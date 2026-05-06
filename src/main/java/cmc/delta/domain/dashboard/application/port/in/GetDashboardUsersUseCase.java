package cmc.delta.domain.dashboard.application.port.in;

import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;

public interface GetDashboardUsersUseCase {

	DashboardUsersResponse getUsers(int page, int size);
}
