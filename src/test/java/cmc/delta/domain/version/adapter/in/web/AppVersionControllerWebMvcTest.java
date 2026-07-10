package cmc.delta.domain.version.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.version.application.port.in.AppVersionQueryUseCase;
import cmc.delta.domain.version.application.port.in.result.AppVersionResponse;
import cmc.delta.domain.version.application.exception.VersionException;
import cmc.delta.global.error.ErrorLogWriter;
import cmc.delta.global.error.ErrorResponseFactory;
import cmc.delta.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppVersionControllerWebMvcTest {

	private MockMvc mvc;
	private AppVersionQueryUseCase appVersionQueryUseCase;

	@BeforeEach
	void setUp() {
		appVersionQueryUseCase = mock(AppVersionQueryUseCase.class);
		AppVersionController controller = new AppVersionController(appVersionQueryUseCase);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalExceptionHandler(mock(ErrorLogWriter.class), new ErrorResponseFactory()))
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("GET /app-version: 버전 정책과 업데이트 판단값 응답")
	void getAppVersion_ok_returnsPolicy() throws Exception {
		// given
		when(appVersionQueryUseCase.getAppVersion("1.0.0"))
			.thenReturn(new AppVersionResponse("1.0.0", "1.1.0", false, true));

		// when & then
		mvc.perform(get("/api/v1/app-version")
				.param("currentVersion", "1.0.0"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.code").value("S_200"))
			.andExpect(jsonPath("$.data.minimumVersion").value("1.0.0"))
			.andExpect(jsonPath("$.data.latestVersion").value("1.1.0"))
			.andExpect(jsonPath("$.data.forceUpdate").value(false))
			.andExpect(jsonPath("$.data.updateAvailable").value(true));

		verify(appVersionQueryUseCase).getAppVersion("1.0.0");
	}

	@Test
	@DisplayName("GET /app-version: currentVersion 누락 시 400")
	void getAppVersion_whenCurrentVersionMissing_thenBadRequest() throws Exception {
		// given
		when(appVersionQueryUseCase.getAppVersion(null)).thenThrow(VersionException.invalidVersion());

		// when & then
		mvc.perform(get("/api/v1/app-version"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("REQ_001"));
	}

	@Test
	@DisplayName("GET /app-version: currentVersion 형식 오류 시 400")
	void getAppVersion_whenCurrentVersionInvalid_thenBadRequest() throws Exception {
		// given
		when(appVersionQueryUseCase.getAppVersion("1.0")).thenThrow(VersionException.invalidVersion());

		// when & then
		mvc.perform(get("/api/v1/app-version")
				.param("currentVersion", "1.0"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("REQ_001"));
	}
}
