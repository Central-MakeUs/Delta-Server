package cmc.delta.domain.dashboard.application.dto;

import java.util.List;

public record DashboardProblemsResponse(
	List<DashboardProblemItem> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
