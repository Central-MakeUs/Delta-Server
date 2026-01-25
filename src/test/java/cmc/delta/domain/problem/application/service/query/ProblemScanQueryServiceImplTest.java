package cmc.delta.domain.problem.application.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.mapper.scan.*;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.*;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.*;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import org.junit.jupiter.api.*;

class ProblemScanQueryServiceImplTest {

	private ScanQueryPort scanQueryPort;
	private StoragePort storagePort;
	private ScanTypePredictionReader scanTypePredictionReader;

	private ProblemScanDetailValidator detailValidator;
	private UnitSubjectResolver subjectResolver;
	private ProblemScanDetailMapper detailMapper;

	private ProblemScanQueryValidator summaryValidator;
	private ProblemScanSummaryMapper summaryMapper;

	private ProblemScanQueryServiceImpl sut;

	@BeforeEach
	void setUp() {
		scanQueryPort = mock(ScanQueryPort.class);
		storagePort = mock(StoragePort.class);
		scanTypePredictionReader = mock(ScanTypePredictionReader.class);

		detailValidator = mock(ProblemScanDetailValidator.class);
		subjectResolver = mock(UnitSubjectResolver.class);
		detailMapper = mock(ProblemScanDetailMapper.class);

		summaryValidator = mock(ProblemScanQueryValidator.class);
		summaryMapper = mock(ProblemScanSummaryMapper.class);

		sut = new ProblemScanQueryServiceImpl(
			scanQueryPort,
			storagePort,
			scanTypePredictionReader,
			detailValidator,
			subjectResolver,
			detailMapper,
			summaryValidator,
			summaryMapper);
	}

	@Test
	@DisplayName("getSummary: viewUrl 발급 + 과목 resolve + summary mapper")
	void summary_success() {
		// given
		ScanListRow row = mock(ScanListRow.class);
		when(scanQueryPort.findListRow(10L, 1L)).thenReturn(java.util.Optional.of(row));

		when(row.getStorageKey()).thenReturn("s3/a.png");
		when(row.getUnitId()).thenReturn("U1");
		when(storagePort.issueReadUrl("s3/a.png")).thenReturn("https://read/s3/a.png");

		SubjectInfo subject = mock(SubjectInfo.class);
		when(subjectResolver.resolveByUnitId("U1")).thenReturn(subject);
		when(scanTypePredictionReader.findByScanId(1L)).thenReturn(List.of());

		ProblemScanSummaryResponse expected = mock(ProblemScanSummaryResponse.class);
		when(summaryMapper.toSummaryResponse(eq(row), eq("https://read/s3/a.png"), eq(subject), anyList()))
			.thenReturn(expected);

		// when
		ProblemScanSummaryResponse res = sut.getSummary(10L, 1L);

		// then
		assertThat(res).isSameAs(expected);
	}

	@Test
	@DisplayName("getDetail: viewUrl 발급 + 과목 resolve + detail mapper")
	void detail_success() {
		// given
		ScanDetailProjection p = mock(ScanDetailProjection.class);
		when(scanQueryPort.findDetail(10L, 1L)).thenReturn(java.util.Optional.of(p));

		when(p.getStorageKey()).thenReturn("s3/d.png");
		when(p.getPredictedUnitId()).thenReturn("U1");
		when(storagePort.issueReadUrl("s3/d.png")).thenReturn("https://read/s3/d.png");

		SubjectInfo subject = mock(SubjectInfo.class);
		when(subjectResolver.resolveByUnitId("U1")).thenReturn(subject);
		when(scanTypePredictionReader.findByScanId(1L)).thenReturn(List.of());

		ProblemScanDetailResponse.AiClassification ai = mock(ProblemScanDetailResponse.AiClassification.class);
		when(detailMapper.toAiClassification(eq(p), eq(subject), anyList())).thenReturn(ai);

		ProblemScanDetailResponse expected = mock(ProblemScanDetailResponse.class);
		when(detailMapper.toDetailResponse(p, "https://read/s3/d.png", ai)).thenReturn(expected);

		// when
		ProblemScanDetailResponse res = sut.getDetail(10L, 1L);

		// then
		assertThat(res).isSameAs(expected);
	}
}
