package cmc.delta.domain.dashboard.adapter.in.web.dto.request;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardUsersRequestTest {

	@Test
	@DisplayName("DashboardUsersRequest: page/size/sort 기본값을 적용한다")
	void request_defaults() {
		// when
		DashboardUsersRequest request = new DashboardUsersRequest(null, null, null, null);

		// then
		assertThat(request.page()).isZero();
		assertThat(request.size()).isEqualTo(20);
		assertThat(request.sortBy()).isEqualTo(DashboardUserSortBy.ID);
		assertThat(request.sortDirection()).isEqualTo(DashboardSortDirection.DESC);
	}

	@Test
	@DisplayName("DashboardUsersRequest: page/size 하한과 size 상한을 보정한다")
	void request_normalizesPageAndSize() {
		// when
		DashboardUsersRequest request = new DashboardUsersRequest(
			-1,
			101,
			DashboardUserSortBy.NICKNAME,
			DashboardSortDirection.ASC);

		// then
		assertThat(request.page()).isZero();
		assertThat(request.size()).isEqualTo(100);
		assertThat(request.sortBy()).isEqualTo(DashboardUserSortBy.NICKNAME);
		assertThat(request.sortDirection()).isEqualTo(DashboardSortDirection.ASC);
	}
}
