package cmc.delta.domain.problem.adapter.in.web.scan;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanCreateResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProblemScanControllerTest {

	@Mock ScanCommandUseCase scanCommandUseCase;
	@Mock ProblemScanQueryUseCase problemScanQueryUseCase;

	private ProblemScanController sut;

	@BeforeEach
	void setUp() {
		sut = new ProblemScanController(scanCommandUseCase, problemScanQueryUseCase);
	}

	@Test
	@DisplayName("create: usecase에 위임하고 성공 응답을 반환한다")
	void create_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		MultipartFile file = mock(MultipartFile.class);

		ScanCreateResult result = new ScanCreateResult(1L, 11L, "UPLOADED");
		when(scanCommandUseCase.createScan(eq(10L), any(CreateScanCommand.class))).thenReturn(result);

		// when
		ApiResponse<ProblemScanCreateResponse> res = sut.create(principal, file);

		// then
		assertThat(res).isNotNull();
		verify(scanCommandUseCase).createScan(eq(10L), argThat(cmd -> cmd != null && cmd.file() == file));
	}

	@Test
	@DisplayName("getScanDetail: usecase에 위임한다")
	void getDetail_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemScanDetailResponse data = mock(ProblemScanDetailResponse.class);
		when(problemScanQueryUseCase.getDetail(10L, 3L)).thenReturn(data);

		// when
		ApiResponse<ProblemScanDetailResponse> res = sut.getScanDetail(principal, 3L);

		// then
		assertThat(res).isNotNull();
		verify(problemScanQueryUseCase).getDetail(10L, 3L);
	}

	@Test
	@DisplayName("getScanSummary: usecase에 위임한다")
	void getSummary_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemScanSummaryResponse data = mock(ProblemScanSummaryResponse.class);
		when(problemScanQueryUseCase.getSummary(10L, 7L)).thenReturn(data);

		// when
		ApiResponse<ProblemScanSummaryResponse> res = sut.getScanSummary(principal, 7L);

		// then
		assertThat(res).isNotNull();
		verify(problemScanQueryUseCase).getSummary(10L, 7L);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
