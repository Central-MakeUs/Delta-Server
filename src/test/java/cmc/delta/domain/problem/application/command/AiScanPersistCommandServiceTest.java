package cmc.delta.domain.problem.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.application.command.support.TypeCandidatesParser;
import cmc.delta.domain.problem.application.command.support.TypeCandidatesParser.TypeCandidate;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter.TypePrediction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiScanPersistCommandServiceTest {

	private AiScanPersister persister;
	private ScanTypePredictionWriter predictionWriter;
	private TypeCandidatesParser typeCandidatesParser;
	private AiScanPersistCommandService sut;

	@BeforeEach
	void setUp() {
		persister = mock(AiScanPersister.class);
		predictionWriter = mock(ScanTypePredictionWriter.class);
		typeCandidatesParser = mock(TypeCandidatesParser.class);
		sut = new AiScanPersistCommandService(persister, predictionWriter, typeCandidatesParser);
	}

	@Test
	@DisplayName("persistAiSucceeded: JSON 파싱 성공 시 파싱된 candidates로 predictions를 저장한다")
	void persistAiSucceeded_withParsedCandidates_savesParsedPredictions() {
		// given
		Long scanId = 1L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		AiCurriculumResult result = aiResult("T1", 0.9, "[{...}]");

		List<TypeCandidate> parsed = List.of(
			new TypeCandidate("T1", BigDecimal.valueOf(0.9)),
			new TypeCandidate("T2", BigDecimal.valueOf(0.7))
		);
		when(typeCandidatesParser.parseTypeCandidates(any())).thenReturn(parsed);

		// when
		sut.persistAiSucceeded(scanId, "owner", "token", result, now);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());

		List<TypePrediction> saved = captor.getValue();
		assertThat(saved).hasSize(2);
		assertThat(saved.get(0).typeId()).isEqualTo("T1");
		assertThat(saved.get(0).rankNo()).isEqualTo(1);
		assertThat(saved.get(1).typeId()).isEqualTo("T2");
		assertThat(saved.get(1).rankNo()).isEqualTo(2);
	}

	@Test
	@DisplayName("persistAiSucceeded: JSON 파싱 결과가 비어있으면 predictedTypeId로 fallback된다")
	void persistAiSucceeded_whenParsedEmpty_fallbackToPredictedTypeId() {
		// given
		Long scanId = 2L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		AiCurriculumResult result = aiResult("FALLBACK-TYPE", 0.75, "[]");

		when(typeCandidatesParser.parseTypeCandidates(any())).thenReturn(List.of());

		// when
		sut.persistAiSucceeded(scanId, "owner", "token", result, now);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());

		List<TypePrediction> saved = captor.getValue();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).typeId()).isEqualTo("FALLBACK-TYPE");
		assertThat(saved.get(0).rankNo()).isEqualTo(1);
		assertThat(saved.get(0).confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.75));
	}

	@Test
	@DisplayName("persistAiSucceeded: JSON 파싱 결과가 비어있고 predictedTypeId도 null이면 빈 predictions를 저장한다")
	void persistAiSucceeded_whenParsedEmptyAndNullTypeId_savesEmpty() {
		// given
		Long scanId = 3L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		AiCurriculumResult result = aiResultWithNullTypeId(0.5, "[]");

		when(typeCandidatesParser.parseTypeCandidates(any())).thenReturn(List.of());

		// when
		sut.persistAiSucceeded(scanId, "owner", "token", result, now);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());
		assertThat(captor.getValue()).isEmpty();
	}

	@Test
	@DisplayName("persistAiSucceeded: JSON 파싱 결과가 비어있고 predictedTypeId가 blank이면 빈 predictions를 저장한다")
	void persistAiSucceeded_whenParsedEmptyAndBlankTypeId_savesEmpty() {
		// given
		Long scanId = 4L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		AiCurriculumResult result = new AiCurriculumResult(
			true, null, null, "   ", 0.5, null, null, "[]", null);

		when(typeCandidatesParser.parseTypeCandidates(any())).thenReturn(List.of());

		// when
		sut.persistAiSucceeded(scanId, "owner", "token", result, now);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());
		assertThat(captor.getValue()).isEmpty();
	}

	@Test
	@DisplayName("persistAiSucceeded: persister.persistAiSucceeded를 반드시 먼저 호출한다")
	void persistAiSucceeded_callsPersisterFirst() {
		// given
		Long scanId = 5L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		AiCurriculumResult result = aiResult("T1", 0.9, null);

		when(typeCandidatesParser.parseTypeCandidates(any())).thenReturn(List.of());

		// when
		sut.persistAiSucceeded(scanId, "owner", "token", result, now);

		// then
		var inOrder = inOrder(persister, predictionWriter);
		inOrder.verify(persister).persistAiSucceeded(eq(scanId), eq("owner"), eq("token"), eq(result), eq(now));
		inOrder.verify(predictionWriter).replacePredictions(eq(scanId), any());
	}

	@Test
	@DisplayName("persistAiFailed: persister에 그대로 위임한다")
	void persistAiFailed_delegatesToPersister() {
		// given
		Long scanId = 6L;
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		FailureDecision decision = FailureDecision.retryable(FailureReason.SCAN_NOT_FOUND);

		// when
		sut.persistAiFailed(scanId, "owner", "token", decision, now);

		// then
		verify(persister).persistAiFailed(scanId, "owner", "token", decision, now);
		verifyNoInteractions(predictionWriter);
	}

	private AiCurriculumResult aiResult(String typeId, double confidence, String typeCandidatesJson) {
		return new AiCurriculumResult(
			true, null, null, typeId, confidence, null, null, typeCandidatesJson, null);
	}

	private AiCurriculumResult aiResultWithNullTypeId(double confidence, String typeCandidatesJson) {
		return new AiCurriculumResult(
			true, null, null, null, confidence, null, null, typeCandidatesJson, null);
	}
}
