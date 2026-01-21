package cmc.delta.domain.problem.application.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.dto.ProblemDetailRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListRow;
import cmc.delta.domain.problem.application.mapper.problem.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.PageImpl;

class ProblemQueryServiceImplTest {

	private ProblemListRequestValidator requestValidator;
	private ProblemQueryPort problemQueryPort;
	private StoragePort storagePort;
	private ProblemListMapper listMapper;
	private ProblemDetailMapper detailMapper;

	private ProblemQueryServiceImpl sut;

	@BeforeEach
	void setUp() {
		requestValidator = mock(ProblemListRequestValidator.class);
		problemQueryPort = mock(ProblemQueryPort.class);
		storagePort = mock(StoragePort.class);
		listMapper = mock(ProblemListMapper.class);
		detailMapper = mock(ProblemDetailMapper.class);

		sut = new ProblemQueryServiceImpl(
			requestValidator,
			problemQueryPort,
			storagePort,
			listMapper,
			detailMapper
		);
	}

	@Test
	@DisplayName("getMyProblemCardList: row.storageKey로 presigned url 발급 후 mapper로 변환한다")
	void list_success() {
		// given
		ProblemListCondition cond = mock(ProblemListCondition.class);
		Pageable pageable = PageRequest.of(0, 10);

		ProblemListRow row = mock(ProblemListRow.class);
		when(row.storageKey()).thenReturn("s3/k.png");
		when(storagePort.issueReadUrl("s3/k.png")).thenReturn("https://read/s3/k.png");

		when(problemQueryPort.findMyProblemList(eq(10L), eq(cond), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(row), pageable, 1));

		ProblemListItemResponse item = mock(ProblemListItemResponse.class);
		when(listMapper.toResponse(row, "https://read/s3/k.png")).thenReturn(item);

		// when
		PagedResponse<ProblemListItemResponse> res = sut.getMyProblemCardList(10L, cond, pageable);

		// then
		assertThat(res).isNotNull();
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

		ProblemDetailResponse expected = mock(ProblemDetailResponse.class);
		when(detailMapper.toResponse(row, "https://read/s3/d.png")).thenReturn(expected);

		// when
		ProblemDetailResponse res = sut.getMyProblemDetail(10L, 1L);

		// then
		assertThat(res).isSameAs(expected);
	}
}
