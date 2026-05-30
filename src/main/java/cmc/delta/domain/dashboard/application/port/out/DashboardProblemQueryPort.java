package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardProblemItem;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DashboardProblemQueryPort {

	List<DashboardProblemItem> findProblems(Pageable pageable);

	long countProblems();
}
