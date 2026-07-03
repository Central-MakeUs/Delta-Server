package cmc.delta.domain.dashboard.application.port.out;

import cmc.delta.domain.dashboard.application.dto.DashboardProblemDetailRow;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface DashboardProblemQueryPort {

	List<DashboardProblemItem> findProblems(Pageable pageable);

	long countProblems();

	Optional<DashboardProblemDetailRow> findProblemDetail(Long problemId);
}
