package cmc.delta.domain.curriculum.adapter.in.web.type;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeActivationRequest;
import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeCreateRequest;
import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeUpdateRequest;
import cmc.delta.domain.curriculum.application.port.in.type.ProblemTypeUseCase;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;
import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.global.config.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProblemTypeControllerWebMvcTest {

	private MockMvc mvc;
	private ProblemTypeUseCase problemTypeUseCase;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		problemTypeUseCase = mock(ProblemTypeUseCase.class);
		objectMapper = new ObjectMapper().findAndRegisterModules();

		ProblemTypeController controller = new ProblemTypeController(problemTypeUseCase);
		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /problem-types: includeInactive=false로 호출")
	void getMyTypes_default_callsUseCase() throws Exception {
		when(problemTypeUseCase.getMyTypes(10L, false))
			.thenReturn(new ProblemTypeListResponse(List.of()));

		mvc.perform(get("/api/v1/problem-types")
				.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(problemTypeUseCase).getMyTypes(10L, false);
	}

	@Test
	@DisplayName("GET /problem-types?includeInactive=true: includeInactive=true로 호출")
	void getMyTypes_includeInactive_callsUseCase() throws Exception {
		when(problemTypeUseCase.getMyTypes(10L, true))
			.thenReturn(new ProblemTypeListResponse(List.of()));

		mvc.perform(get("/api/v1/problem-types")
				.param("includeInactive", "true")
				.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk());

		verify(problemTypeUseCase).getMyTypes(10L, true);
	}

	@Test
	@DisplayName("POST /problem-types: request 전달 + usecase 호출")
	void createCustomType_ok_callsUseCase() throws Exception {
		ProblemTypeCreateRequest req = new ProblemTypeCreateRequest("서술형");
		when(problemTypeUseCase.createCustomType(eq(10L), any()))
			.thenReturn(new ProblemTypeItemResponse("T_C_x", "서술형", true, true, 7));

		mvc.perform(post("/api/v1/problem-types")
				.requestAttr(ATTR, principal(10L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(req)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(problemTypeUseCase).createCustomType(eq(10L), any());
	}

	@Test
	@DisplayName("PATCH /problem-types/{id}/activation: request 전달 + usecase 호출")
	void setActive_ok_callsUseCase() throws Exception {
		ProblemTypeActivationRequest req = new ProblemTypeActivationRequest(false);

		mvc.perform(patch("/api/v1/problem-types/T_C_x/activation")
				.requestAttr(ATTR, principal(10L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(req)))
			.andExpect(status().isOk());

		verify(problemTypeUseCase).setActive(eq(10L), eq("T_C_x"), any());
	}

	@Test
	@DisplayName("PATCH /problem-types/{id}: request 전달 + usecase 호출")
	void updateCustomType_ok_callsUseCase() throws Exception {
		ProblemTypeUpdateRequest req = new ProblemTypeUpdateRequest("새이름", 3);
		when(problemTypeUseCase.updateCustomType(eq(10L), eq("T_C_x"), any()))
			.thenReturn(new ProblemTypeItemResponse("T_C_x", "새이름", true, true, 3));

		mvc.perform(patch("/api/v1/problem-types/T_C_x")
				.requestAttr(ATTR, principal(10L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(req)))
			.andExpect(status().isOk());

		verify(problemTypeUseCase).updateCustomType(eq(10L), eq("T_C_x"), any());
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
