package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DashboardUserQueryPort {

	List<DashboardUserItem> findUsers(Pageable pageable);

	long countUsers();
}
