package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DashboardUserQueryPort {

	List<DashboardUserItem> findUsers(Pageable pageable, DashboardUserSortBy sortBy,
		DashboardSortDirection sortDirection);

	long countUsers();
}
