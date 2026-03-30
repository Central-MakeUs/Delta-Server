package cmc.delta.domain.problem.application.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.mapper.scan.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanGroupSummaryResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader.TypePredictionView;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.ProblemScanQueryValidator;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.global.storage.port.out.StoragePort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanGroupQueryServiceImplTest {

	private ScanQueryPort scanQueryPort;
	private StoragePort storagePort;
	private ScanTypePredictionReader scanTypePredictionReader;
	private UnitSubjectResolver subjectResolver;
	private ProblemScanQueryValidator summaryValidator;
	private ProblemScanSummaryMapper summaryMapper;

	private ProblemScanGroupQueryServiceImpl sut;

	@BeforeEach
	void setUp() {
		scanQueryPort = mock(ScanQueryPort.class);
		storagePort = mock(StoragePort.class);
		scanTypePredictionReader = mock(ScanTypePredictionReader.class);
		subjectResolver = mock(UnitSubjectResolver.class);
		summaryValidator = mock(ProblemScanQueryValidator.class);
		summaryMapper = mock(ProblemScanSummaryMapper.class);

		sut = new ProblemScanGroupQueryServiceImpl(
			scanQueryPort,
			storagePort,
			scanTypePredictionReader,
			subjectResolver,
			summaryValidator,
			summaryMapper);
	}

	@Test
	@DisplayName("getGroupSummary: groupId가 응답에 그대로 포함된다")
	void getGroupSummary_preservesGroupId() {
		// given
		long userId = 10L;
		long groupId = 42L;
		when(scanQueryPort.findListRowsByGroupId(userId, groupId)).thenReturn(List.of());

		// when
		ProblemScanGroupSummaryResponse result = sut.getGroupSummary(userId, groupId);

		// then
		assertThat(result.groupId()).isEqualTo(42L);
	}

	@Test
	@DisplayName("getGroupSummary: 조회된 행 수만큼 요약 응답이 생성된다")
	void getGroupSummary_returnsSummaryPerRow() {
		// given
		long userId = 10L;
		long groupId = 42L;
		ScanListRow row1 = buildRow(1L, "key1");
		ScanListRow row2 = buildRow(2L, "key2");
		when(scanQueryPort.findListRowsByGroupId(userId, groupId)).thenReturn(List.of(row1, row2));

		ProblemScanSummaryResponse summary1 = mock(ProblemScanSummaryResponse.class);
		ProblemScanSummaryResponse summary2 = mock(ProblemScanSummaryResponse.class);
		givenBuildSummaryOk(row1, summary1);
		givenBuildSummaryOk(row2, summary2);

		// when
		ProblemScanGroupSummaryResponse result = sut.getGroupSummary(userId, groupId);

		// then
		assertThat(result.scans()).containsExactly(summary1, summary2);
	}

	@Test
	@DisplayName("getGroupSummary: 그룹 내 스캔이 없으면 빈 리스트를 반환한다")
	void getGroupSummary_whenNoRows_returnsEmptyScans() {
		// given
		when(scanQueryPort.findListRowsByGroupId(anyLong(), anyLong())).thenReturn(List.of());

		// when
		ProblemScanGroupSummaryResponse result = sut.getGroupSummary(10L, 99L);

		// then
		assertThat(result.scans()).isEmpty();
	}

	@Test
	@DisplayName("getGroupSummary: 각 행마다 validateHasOriginalAsset이 호출된다")
	void getGroupSummary_validatesEachRowAsset() {
		// given
		long userId = 10L;
		long groupId = 5L;
		ScanListRow row1 = buildRow(1L, "key1");
		ScanListRow row2 = buildRow(2L, "key2");
		when(scanQueryPort.findListRowsByGroupId(userId, groupId)).thenReturn(List.of(row1, row2));

		givenBuildSummaryOk(row1, mock(ProblemScanSummaryResponse.class));
		givenBuildSummaryOk(row2, mock(ProblemScanSummaryResponse.class));

		// when
		sut.getGroupSummary(userId, groupId);

		// then
		verify(summaryValidator).validateHasOriginalAsset(row1);
		verify(summaryValidator).validateHasOriginalAsset(row2);
	}

	@Test
	@DisplayName("getGroupSummary: validator 예외 발생 시 그대로 전파된다")
	void getGroupSummary_whenValidationFails_throwsException() {
		// given
		ScanListRow row = buildRow(1L, null); // storageKey null → 검증 실패 시나리오
		when(scanQueryPort.findListRowsByGroupId(anyLong(), anyLong())).thenReturn(List.of(row));
		doThrow(new cmc.delta.domain.problem.application.exception.ProblemException(
			cmc.delta.global.error.ErrorCode.PROBLEM_ASSET_NOT_FOUND))
			.when(summaryValidator).validateHasOriginalAsset(row);

		// when & then
		assertThatThrownBy(() -> sut.getGroupSummary(10L, 42L))
			.isInstanceOf(cmc.delta.domain.problem.application.exception.ProblemException.class);
	}

	@Test
	@DisplayName("getGroupSummary: storagePort.issueReadUrl이 각 행의 storageKey로 호출된다")
	void getGroupSummary_issuesReadUrlPerRow() {
		// given
		long userId = 10L;
		long groupId = 7L;
		ScanListRow row = buildRow(1L, "scans/a.png");
		when(scanQueryPort.findListRowsByGroupId(userId, groupId)).thenReturn(List.of(row));
		givenBuildSummaryOk(row, mock(ProblemScanSummaryResponse.class));

		// when
		sut.getGroupSummary(userId, groupId);

		// then
		verify(storagePort).issueReadUrl("scans/a.png");
	}

	@Test
	@DisplayName("getGroupSummary: summaryMapper.toSummaryResponse에 올바른 인자가 전달된다")
	void getGroupSummary_passesCorrectArgumentsToMapper() {
		// given
		long userId = 10L;
		long groupId = 7L;
		ScanListRow row = buildRow(1L, "scans/a.png");
		when(scanQueryPort.findListRowsByGroupId(userId, groupId)).thenReturn(List.of(row));

		String viewUrl = "https://cdn.example.com/scans/a.png";
		SubjectInfo subject = new SubjectInfo("sub1", "수학");
		List<TypePredictionView> predictions = List.of(
			new TypePredictionView("t1", "방정식", 1, BigDecimal.valueOf(0.9)));
		List<CurriculumItemResponse> types = List.of(new CurriculumItemResponse("t1", "방정식"));

		when(storagePort.issueReadUrl("scans/a.png")).thenReturn(viewUrl);
		when(subjectResolver.resolveByUnitId(row.getUnitId())).thenReturn(subject);
		when(scanTypePredictionReader.findByScanId(1L)).thenReturn(predictions);
		when(summaryMapper.toSummaryResponse(eq(row), eq(viewUrl), eq(subject), anyList()))
			.thenReturn(mock(ProblemScanSummaryResponse.class));

		// when
		sut.getGroupSummary(userId, groupId);

		// then
		verify(summaryMapper).toSummaryResponse(eq(row), eq(viewUrl), eq(subject),
			argThat(list -> list.size() == 1 &&
				list.get(0).id().equals("t1") &&
				list.get(0).name().equals("방정식")));
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	private ScanListRow buildRow(long scanId, String storageKey) {
		return new ScanListRow(
			scanId, 10L, ScanStatus.AI_DONE,
			scanId * 10, storageKey,
			"unit1", "미적분", "type1", "극한",
			false, null);
	}

	private void givenBuildSummaryOk(ScanListRow row, ProblemScanSummaryResponse response) {
		when(storagePort.issueReadUrl(row.getStorageKey())).thenReturn("https://cdn/" + row.getStorageKey());
		when(subjectResolver.resolveByUnitId(row.getUnitId())).thenReturn(SubjectInfo.empty());
		when(scanTypePredictionReader.findByScanId(row.getScanId())).thenReturn(List.of());
		when(summaryMapper.toSummaryResponse(eq(row), anyString(), any(SubjectInfo.class), anyList()))
			.thenReturn(response);
	}
}
