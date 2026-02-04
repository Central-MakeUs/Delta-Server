package cmc.delta.domain.user.adapter.in;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.user.adapter.in.support.MultipartFileReader;
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

	private static final long USER_ID = 10L;
	private static final int TTL_SECONDS = 60;
	private static final String STORAGE_KEY = "k";
	private static final String URL = "u";
	private static final String FILE_FIELD = "file";
	private static final String FILE_NAME = "p.png";
	private static final String CONTENT_TYPE = "image/png";
	private static final String FILE_CONTENT = "hello";

	private MockMvc mvc;
	private UserProfileImageUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = mock(UserProfileImageUseCase.class);

		MultipartFileReader fileReader = new MultipartFileReader();
		UserProfileImageController controller = new UserProfileImageController(useCase, fileReader);
		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("POST /users/me/profile-image: multipart 업로드 + command 생성 후 usecase 호출")
	void upload_ok_callsUseCaseWithCommand() throws Exception {
		when(useCase.uploadMyProfileImage(eq(USER_ID), any(ProfileImageUploadCommand.class)))
			.thenReturn(new UserProfileImageResult(STORAGE_KEY, URL, TTL_SECONDS));

		MockMultipartFile file = new MockMultipartFile(
			FILE_FIELD,
			FILE_NAME,
			CONTENT_TYPE,
			FILE_CONTENT.getBytes()
		);

		mvc.perform(multipart("/api/v1/users/me/profile-image")
				.file(file)
				.requestAttr(ATTR, principal(USER_ID))
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		ArgumentCaptor<ProfileImageUploadCommand> captor = ArgumentCaptor.forClass(ProfileImageUploadCommand.class);
		verify(useCase).uploadMyProfileImage(eq(USER_ID), captor.capture());
		ProfileImageUploadCommand cmd = captor.getValue();
		assertThat(cmd.bytes()).isEqualTo(FILE_CONTENT.getBytes());
		assertThat(cmd.contentType()).isEqualTo(CONTENT_TYPE);
		assertThat(cmd.originalFilename()).isEqualTo(FILE_NAME);
	}

	@Test
	@DisplayName("GET /users/me/profile-image: usecase 호출")
	void get_ok_callsUseCase() throws Exception {
		when(useCase.getMyProfileImage(USER_ID)).thenReturn(new UserProfileImageResult(STORAGE_KEY, URL, TTL_SECONDS));

		mvc.perform(get("/api/v1/users/me/profile-image")
				.requestAttr(ATTR, principal(USER_ID)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(useCase).getMyProfileImage(USER_ID);
	}

	@Test
	@DisplayName("DELETE /users/me/profile-image: usecase 호출")
	void delete_ok_callsUseCase() throws Exception {
		mvc.perform(delete("/api/v1/users/me/profile-image")
			.requestAttr(ATTR, principal(USER_ID)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(useCase).deleteMyProfileImage(USER_ID);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
