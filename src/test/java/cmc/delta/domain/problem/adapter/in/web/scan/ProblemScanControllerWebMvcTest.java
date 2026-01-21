package cmc.delta.domain.problem.adapter.in.web.scan;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProblemScanControllerWebMvcTest {

	private MockMvc mvc;
	private ScanCommandUseCase scanCommandUseCase;
	private ProblemScanQueryUseCase problemScanQueryUseCase;

	@BeforeEach
	void setUp() {
		scanCommandUseCase = mock(ScanCommandUseCase.class);
		problemScanQueryUseCase = mock(ProblemScanQueryUseCase.class);

		ProblemScanController controller = new ProblemScanController(scanCommandUseCase, problemScanQueryUseCase);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("POST /problem-scans: multipart(file) 바인딩 + usecase 호출")
	void create_multipart_ok() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());
		when(scanCommandUseCase.createScan(eq(10L), any(CreateScanCommand.class)))
			.thenReturn(new ScanCreateResult(1L, 11L, "UPLOADED"));

		ArgumentCaptor<CreateScanCommand> captor = ArgumentCaptor.forClass(CreateScanCommand.class);

		// when & then
		mvc.perform(multipart("/api/v1/problem-scans")
				.file(file)
				.requestAttr(ATTR, principal)
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(scanCommandUseCase).createScan(eq(10L), captor.capture());
		Assertions.assertSame(file, captor.getValue().file());
	}

	@Test
	@DisplayName("POST /problem-scans: file 파트 없으면 400 + usecase 미호출")
	void create_missingFile_400() throws Exception {
		// given
		UserPrincipal principal = principal(10L);

		// when & then
		mvc.perform(multipart("/api/v1/problem-scans")
				.requestAttr(ATTR, principal)
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(scanCommandUseCase);
	}

	@Test
	@DisplayName("GET /problem-scans/{scanId}: pathvariable 바인딩 + usecase 호출")
	void getDetail_ok() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(problemScanQueryUseCase.getDetail(10L, 7L)).thenReturn(null);

		// when & then
		mvc.perform(get("/api/v1/problem-scans/{scanId}", 7L)
				.requestAttr(ATTR, principal))
			.andExpect(status().isOk());

		verify(problemScanQueryUseCase).getDetail(10L, 7L);
	}

	@Test
	@DisplayName("GET /problem-scans/{scanId}/summary: pathvariable 바인딩 + usecase 호출")
	void getSummary_ok() throws Exception {
		// given
		UserPrincipal principal = principal(10L);
		when(problemScanQueryUseCase.getSummary(10L, 7L)).thenReturn(null);

		// when & then
		mvc.perform(get("/api/v1/problem-scans/{scanId}/summary", 7L)
				.requestAttr(ATTR, principal))
			.andExpect(status().isOk());

		verify(problemScanQueryUseCase).getSummary(10L, 7L);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}

