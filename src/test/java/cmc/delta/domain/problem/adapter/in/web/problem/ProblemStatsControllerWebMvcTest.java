package cmc.delta.domain.problem.adapter.in.web.problem;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.application.port.in.problem.ProblemStatsUseCase;
import cmc.delta.domain.problem.application.support.query.ProblemStatsConditionFactory;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProblemStatsControllerWebMvcTest {

	private MockMvc mvc;

	private ProblemStatsUseCase statsUseCase;
	private ProblemStatsConditionFactory statsConditionFactory;

	@BeforeEach
	void setUp() {
		statsUseCase = mock(ProblemStatsUseCase.class);
		statsConditionFactory = mock(ProblemStatsConditionFactory.class);

		ProblemStatsController controller = new ProblemStatsController(statsUseCase, statsConditionFactory);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /problems/stats/units: modelAttribute 바인딩 + factory/from + usecase 호출")
	void unitStats_ok() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(statsConditionFactory.from(any())).thenReturn(null);
		when(statsUseCase.getUnitStats(eq(10L), any())).thenReturn(new ProblemStatsResponse<>(List.of()));

		// when & then
		mvc.perform(get("/api/v1/problems/stats/units")
				.requestAttr(ATTR, principal))
			.andExpect(status().isOk());

		verify(statsConditionFactory).from(any());
		verify(statsUseCase).getUnitStats(eq(10L), any());
	}

	@Test
	@DisplayName("GET /problems/stats/types: modelAttribute 바인딩 + factory/from + usecase 호출")
	void typeStats_ok() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(statsConditionFactory.from(any())).thenReturn(null);
		when(statsUseCase.getTypeStats(eq(10L), any())).thenReturn(new ProblemStatsResponse<>(List.of()));

		// when & then
		mvc.perform(get("/api/v1/problems/stats/types")
				.requestAttr(ATTR, principal))
			.andExpect(status().isOk());

		verify(statsConditionFactory).from(any());
		verify(statsUseCase).getTypeStats(eq(10L), any());
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
