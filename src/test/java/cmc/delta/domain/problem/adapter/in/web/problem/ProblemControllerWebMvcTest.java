package cmc.delta.domain.problem.adapter.in.web.problem;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemScrollRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.support.ProblemListConditionFactory;
import cmc.delta.domain.problem.application.port.in.problem.ProblemCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProblemControllerWebMvcTest {

	private MockMvc mvc;

	private ProblemCommandUseCase problemCommandUseCase;
	private ProblemQueryUseCase problemQueryUseCase;
	private ProblemListConditionFactory conditionFactory;

	@BeforeEach
	void setUp() {
		problemCommandUseCase = mock(ProblemCommandUseCase.class);
		problemQueryUseCase = mock(ProblemQueryUseCase.class);
		conditionFactory = mock(ProblemListConditionFactory.class);

		ProblemController controller = new ProblemController(problemCommandUseCase, problemQueryUseCase,
			conditionFactory);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /problems: page/size 바인딩 + factory/from 호출 + usecase 호출")
	void list_ok_bindsPageQuery() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(conditionFactory.from(any(MyProblemListRequest.class))).thenReturn(null);
		when(problemQueryUseCase.getMyProblemCardList(eq(10L), any(), any(PageQuery.class))).thenReturn(null);

		ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);

		// when & then
		mvc.perform(get("/api/v1/problems")
			.param("page", "1")
			.param("size", "20")
			.requestAttr(ATTR, principal))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(conditionFactory).from(any(MyProblemListRequest.class));
		verify(problemQueryUseCase).getMyProblemCardList(eq(10L), any(), pageQueryCaptor.capture());

		PageQuery pageQuery = pageQueryCaptor.getValue();
		Assertions.assertEquals(1, pageQuery.page());
		Assertions.assertEquals(20, pageQuery.size());
	}

	@Test
	@DisplayName("GET /problems/scroll: lastId/lastCreatedAt/size 바인딩 + factory/from 호출 + usecase 호출")
	void scroll_ok_bindsCursorQuery() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(conditionFactory.from(any(MyProblemScrollRequest.class))).thenReturn(null);
		when(problemQueryUseCase.getMyProblemCardListCursor(eq(10L), any(), any(CursorQuery.class))).thenReturn(null);

		ArgumentCaptor<CursorQuery> cursorCaptor = ArgumentCaptor.forClass(CursorQuery.class);

		// when & then
		mvc.perform(get("/api/v1/problems/scroll")
			.param("lastId", "123")
			.param("lastCreatedAt", "2026-01-28T12:00:00")
			.param("size", "20")
			.requestAttr(ATTR, principal))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(conditionFactory).from(any(MyProblemScrollRequest.class));
		verify(problemQueryUseCase).getMyProblemCardListCursor(eq(10L), any(), cursorCaptor.capture());

		CursorQuery cursorQuery = cursorCaptor.getValue();
		Assertions.assertEquals(123L, cursorQuery.lastId());
		Assertions.assertEquals(20, cursorQuery.size());
	}

	@Test
	@DisplayName("POST /problems/{id}/complete: JSON 바인딩(solutionText) + usecase 호출")
	void complete_ok_bindsBody() throws Exception {
		// given
		UserPrincipal principal = principal(10L);

		// when & then
		mvc.perform(post("/api/v1/problems/{problemId}/complete", 5L)
			.requestAttr(ATTR, principal)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"solutionText\":\"ans\"}"))
			.andExpect(status().isOk());

		verify(problemCommandUseCase).completeWrongAnswerCard(10L, 5L, "ans");
	}

	@Test
	@DisplayName("GET /problems/{id}: pathvariable 바인딩 + usecase 호출")
	void detail_ok_bindsPath() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(problemQueryUseCase.getMyProblemDetail(10L, 5L)).thenReturn(null);

		// when & then
		mvc.perform(get("/api/v1/problems/{problemId}", 5L)
			.requestAttr(ATTR, principal))
			.andExpect(status().isOk());

		verify(problemQueryUseCase).getMyProblemDetail(10L, 5L);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
