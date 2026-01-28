package cmc.delta.domain.problem.application.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.mapper.problem.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemDetailResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.support.PageResult;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;

class ProblemQueryServiceImplTest {

	private ProblemListRequestValidator requestValidator;
	private ProblemQueryPort problemQueryPort;
	private ProblemTypeTagQueryPort problemTypeTagQueryPort;
	private StoragePort storagePort;
	private ProblemListMapper listMapper;
	private ProblemDetailMapper detailMapper;

	private ProblemQueryServiceImpl sut;

	@BeforeEach
	void setUp() {
		requestValidator = mock(ProblemListRequestValidator.class);
		problemQueryPort = mock(ProblemQueryPort.class);
		problemTypeTagQueryPort = mock(ProblemTypeTagQueryPort.class);
		storagePort = mock(StoragePort.class);
		listMapper = mock(ProblemListMapper.class);
		detailMapper = mock(ProblemDetailMapper.class);

		sut = new ProblemQueryServiceImpl(
			requestValidator,
			problemQueryPort,
			problemTypeTagQueryPort,
			storagePort,
			listMapper,
			detailMapper);
	}

	@Test
	@DisplayName("getMyProblemCardList: row.storageKey로 presigned url 발급 후 mapper로 변환한다")
	void list_success() {
		// given
		ProblemListCondition cond = mock(ProblemListCondition.class);
		PageQuery pageQuery = new PageQuery(0, 10);

		ProblemListRow row = mock(ProblemListRow.class);
		when(row.problemId()).thenReturn(1L);
		when(row.storageKey()).thenReturn("s3/k.png");
		when(storagePort.issueReadUrl("s3/k.png")).thenReturn("https://read/s3/k.png");

		when(problemQueryPort.findMyProblemList(eq(10L), eq(cond), eq(pageQuery)))
			.thenReturn(new PageResult<>(List.of(row), 0, 10, 1, 1));

		when(problemTypeTagQueryPort.findTypeTagsByProblemIds(List.of(1L))).thenReturn(List.of());

		ProblemListItemResponse base = new ProblemListItemResponse(
			1L,
			new CurriculumItemResponse("S1", "subject"),
			new CurriculumItemResponse("U1", "unit"),
			List.<CurriculumItemResponse>of(),
			new ProblemListItemResponse.PreviewImageResponse(1L, "s3/k.png", "https://read/s3/k.png"),
			false,
			LocalDateTime.now());
		when(listMapper.toResponse(row, "https://read/s3/k.png")).thenReturn(base);

		// when
		PagedResponse<ProblemListItemResponse> res = sut.getMyProblemCardList(10L, cond, pageQuery);

		// then
		assertThat(res).isNotNull();
		assertThat(res.content()).hasSize(1);
		assertThat(res.content().get(0).types()).isEmpty();
		verify(storagePort).issueReadUrl("s3/k.png");
	}

	@Test
	@DisplayName("getMyProblemDetail: storageKey로 viewUrl 발급 후 detail mapper로 변환한다")
	void detail_success() {
		// given
		ProblemDetailRow row = mock(ProblemDetailRow.class);
		when(row.storageKey()).thenReturn("s3/d.png");
		when(problemQueryPort.findMyProblemDetail(10L, 1L)).thenReturn(java.util.Optional.of(row));

		when(storagePort.issueReadUrl("s3/d.png")).thenReturn("https://read/s3/d.png");
		when(problemTypeTagQueryPort.findTypeTagsByProblemId(1L)).thenReturn(List.of());

		ProblemDetailResponse base = new ProblemDetailResponse(
			1L,
			new CurriculumItemResponse("S1", "subject"),
			new CurriculumItemResponse("U1", "unit"),
			List.of(new CurriculumItemResponse("T0", "old")),
			new ProblemDetailResponse.OriginalImageResponse(1L, "https://read/s3/d.png"),
			cmc.delta.domain.problem.model.enums.AnswerFormat.TEXT,
			null,
			"ans",
			"sol",
			false,
			null,
			LocalDateTime.now());
		when(detailMapper.toResponse(row, "https://read/s3/d.png")).thenReturn(base);

		// when
		ProblemDetailResponse res = sut.getMyProblemDetail(10L, 1L);

		// then
		assertThat(res.types()).isEmpty();
	}
}
