package cmc.delta.domain.dashboard.application.port.in;

import cmc.delta.domain.dashboard.application.dto.DashboardMonthlyAccessResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemsResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import java.time.YearMonth;
import org.springframework.data.domain.Pageable;

public interface DashboardQueryUseCase {

	DashboardUsersResponse getUsers(Pageable pageable);

	DashboardMonthlyAccessResponse getMonthlyAccess(YearMonth yearMonth);

	DashboardProblemsResponse getProblems(Pageable pageable);
}
