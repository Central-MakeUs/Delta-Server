package cmc.delta.domain.dashboard.application.dto;

import java.util.List;

public record DashboardUsersResponse(
	List<DashboardUserItem> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
