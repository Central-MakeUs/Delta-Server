package cmc.delta.domain.dashboard.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.dashboard.application.port.in.DashboardQueryUseCase;
import org.junit.jupiter.api.*;
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
	@DisplayName("GET /admin/dashboard/problems/{problemId}: pathvariable 바인딩 + usecase 호출")
	void getProblemDetail_ok_bindsPath() throws Exception {
		// when & then
		mvc.perform(get("/api/v1/admin/dashboard/problems/{problemId}", 5L))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(dashboardQueryUseCase).getProblemDetail(5L);
	}
}
