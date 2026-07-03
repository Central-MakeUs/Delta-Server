package cmc.delta.domain.dashboard.application.port.in;

import cmc.delta.domain.dashboard.application.dto.DashboardMonthlyAccessResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemDetailResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemsResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import java.time.YearMonth;
import org.springframework.data.domain.Pageable;

public interface DashboardQueryUseCase {

	DashboardUsersResponse getUsers(Pageable pageable, DashboardUserSortBy sortBy, DashboardSortDirection sortDirection);

	DashboardMonthlyAccessResponse getMonthlyAccess(YearMonth yearMonth);

	DashboardProblemsResponse getProblems(Pageable pageable);

	DashboardProblemDetailResponse getProblemDetail(Long problemId);
}
