package cmc.delta.domain.dashboard.adapter.in.web.dto.request;

import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;

public record DashboardUsersRequest(
	Integer page,
	Integer size,
	DashboardUserSortBy sortBy,
	DashboardSortDirection sortDirection) {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public DashboardUsersRequest {
		if (page == null || page < 0)
			page = DEFAULT_PAGE;
		if (size == null || size <= 0)
			size = DEFAULT_SIZE;
		if (size > MAX_SIZE)
			size = MAX_SIZE;
		if (sortBy == null)
			sortBy = DashboardUserSortBy.ID;
		if (sortDirection == null)
			sortDirection = DashboardSortDirection.DESC;
	}
}
