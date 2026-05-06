package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import java.util.List;

public interface DashboardUserQueryPort {

	List<DashboardUserItem> findUsers(int page, int size);

	long countUsers();
}
