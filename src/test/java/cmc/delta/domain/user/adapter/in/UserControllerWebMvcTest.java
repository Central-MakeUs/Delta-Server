package cmc.delta.domain.user.adapter.in;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.port.in.UserUseCase;
import cmc.delta.global.config.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerWebMvcTest {

	private MockMvc mvc;
	private UserUseCase userUseCase;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		userUseCase = mock(UserUseCase.class);
		objectMapper = new ObjectMapper().findAndRegisterModules();

		UserController controller = new UserController(userUseCase);
		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /users/me: usecase 호출")
	void getMyProfile_ok_callsUseCase() throws Exception {
		when(userUseCase.getMyProfile(10L)).thenReturn(new UserMeData(10L, "user@example.com", "delta"));

		mvc.perform(get("/api/v1/users/me")
				.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(userUseCase).getMyProfile(10L);
	}

	@Test
	@DisplayName("POST /users/me/onboarding: request 전달 + usecase 호출")
	void completeOnboarding_ok_callsUseCase() throws Exception {
		UserOnboardingRequest req = new UserOnboardingRequest("홍길동", LocalDate.of(2000, 1, 1), true);

		mvc.perform(post("/api/v1/users/me/onboarding")
			.requestAttr(ATTR, principal(10L))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsBytes(req)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(userUseCase).completeOnboarding(eq(10L), any(UserOnboardingRequest.class));
	}

	@Test
	@DisplayName("POST /users/withdrawal: usecase 호출")
	void withdraw_ok_callsUseCase() throws Exception {
		mvc.perform(post("/api/v1/users/withdrawal")
			.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(userUseCase).withdrawAccount(10L);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
