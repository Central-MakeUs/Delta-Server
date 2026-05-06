package cmc.delta.domain.dashboard.application.port.in;

import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import org.springframework.data.domain.Pageable;

public interface GetDashboardUsersUseCase {

	DashboardUsersResponse getUsers(Pageable pageable);
}
