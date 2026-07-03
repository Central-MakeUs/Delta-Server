package cmc.delta.domain.dashboard.adapter.in.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.dashboard.application.port.in.DashboardQueryUseCase;
import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerWebMvcTest {

	private MockMvc mvc;
	private DashboardQueryUseCase dashboardQueryUseCase;

	@BeforeEach
	void setUp() {
		dashboardQueryUseCase = mock(DashboardQueryUseCase.class);

		DashboardController controller = new DashboardController(dashboardQueryUseCase);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /admin/dashboard/users: sortBy/sortDirection 바인딩 + usecase 호출")
	void getUsers_ok_bindsSortOptions() throws Exception {
		// given
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		// when & then
		mvc.perform(get("/api/v1/admin/dashboard/users")
			.param("page", "1")
			.param("size", "10")
			.param("sortBy", "NICKNAME")
			.param("sortDirection", "ASC"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(dashboardQueryUseCase).getUsers(
			pageableCaptor.capture(),
			eq(DashboardUserSortBy.NICKNAME),
			eq(DashboardSortDirection.ASC));

		Pageable pageable = pageableCaptor.getValue();
		assertThat(pageable.getPageNumber()).isEqualTo(1);
		assertThat(pageable.getPageSize()).isEqualTo(10);
	}

	@Test
	@DisplayName("GET /admin/dashboard/users: 정렬 파라미터 생략 시 기본 정렬로 usecase 호출")
	void getUsers_ok_usesDefaultSortOptions() throws Exception {
		// when & then
		mvc.perform(get("/api/v1/admin/dashboard/users"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(dashboardQueryUseCase).getUsers(
			any(Pageable.class),
			eq(DashboardUserSortBy.ID),
			eq(DashboardSortDirection.DESC));
	}

	@Test
	@DisplayName("GET /admin/dashboard/problems/{problemId}: pathvariable 바인딩 + usecase 호출")
	void getProblemDetail_ok_bindsPath() throws Exception {
		// when & then
		mvc.perform(get("/api/v1/admin/dashboard/problems/{problemId}", 5L))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(dashboardQueryUseCase).getProblemDetail(5L);
	}
}
