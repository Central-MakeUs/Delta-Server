package cmc.delta.domain.user.adapter.in;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.user.application.port.in.UserProfileImageUseCase;
import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserProfileImageControllerWebMvcTest {

	private MockMvc mvc;
	private UserProfileImageUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = mock(UserProfileImageUseCase.class);

		UserProfileImageController controller = new UserProfileImageController(useCase);
		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("POST /users/me/profile-image: multipart 업로드 + command 생성 후 usecase 호출")
	void upload_ok_callsUseCaseWithCommand() throws Exception {
		when(useCase.uploadMyProfileImage(eq(10L), any(ProfileImageUploadCommand.class)))
			.thenReturn(new UserProfileImageResult("k", "u", 60));

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"p.png",
			"image/png",
			"hello".getBytes()
		);

		mvc.perform(multipart("/api/v1/users/me/profile-image")
				.file(file)
				.requestAttr(ATTR, principal(10L))
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		ArgumentCaptor<ProfileImageUploadCommand> captor = ArgumentCaptor.forClass(ProfileImageUploadCommand.class);
		verify(useCase).uploadMyProfileImage(eq(10L), captor.capture());
		ProfileImageUploadCommand cmd = captor.getValue();
		assertThat(cmd.bytes()).isEqualTo("hello".getBytes());
		assertThat(cmd.contentType()).isEqualTo("image/png");
		assertThat(cmd.originalFilename()).isEqualTo("p.png");
	}

	@Test
	@DisplayName("GET /users/me/profile-image: usecase 호출")
	void get_ok_callsUseCase() throws Exception {
		when(useCase.getMyProfileImage(10L)).thenReturn(new UserProfileImageResult("k", "u", 60));

		mvc.perform(get("/api/v1/users/me/profile-image")
				.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(useCase).getMyProfileImage(10L);
	}

	@Test
	@DisplayName("DELETE /users/me/profile-image: usecase 호출")
	void delete_ok_callsUseCase() throws Exception {
		mvc.perform(delete("/api/v1/users/me/profile-image")
				.requestAttr(ATTR, principal(10L)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(useCase).deleteMyProfileImage(10L);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
