package cmc.delta.domain.problem.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.command.ScanTypePredictionCommandService.PredictedType;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter.TypePrediction;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScanTypePredictionCommandServiceTest {

	private ScanTypePredictionWriter predictionWriter;
	private ScanTypePredictionCommandService sut;

	@BeforeEach
	void setUp() {
		predictionWriter = mock(ScanTypePredictionWriter.class);
		sut = new ScanTypePredictionCommandService(predictionWriter);
	}

	@Test
	@DisplayName("replacePredictedTypes: rank는 1부터 순서대로 부여된다")
	void replacePredictedTypes_rankStartsAtOne() {
		// given
		Long scanId = 1L;
		List<PredictedType> input = List.of(
			new PredictedType("T1", BigDecimal.valueOf(0.9)),
			new PredictedType("T2", BigDecimal.valueOf(0.7)),
			new PredictedType("T3", BigDecimal.valueOf(0.5))
		);

		// when
		sut.replacePredictedTypes(scanId, input);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());

		List<TypePrediction> saved = captor.getValue();
		assertThat(saved).hasSize(3);
		assertThat(saved.get(0).rankNo()).isEqualTo(1);
		assertThat(saved.get(1).rankNo()).isEqualTo(2);
		assertThat(saved.get(2).rankNo()).isEqualTo(3);
	}

	@Test
	@DisplayName("replacePredictedTypes: typeId와 confidence가 그대로 전달된다")
	void replacePredictedTypes_preservesTypeIdAndConfidence() {
		// given
		Long scanId = 2L;
		BigDecimal confidence = BigDecimal.valueOf(0.85);
		List<PredictedType> input = List.of(new PredictedType("TYPE-A", confidence));

		// when
		sut.replacePredictedTypes(scanId, input);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());

		TypePrediction saved = captor.getValue().get(0);
		assertThat(saved.typeId()).isEqualTo("TYPE-A");
		assertThat(saved.confidence()).isEqualByComparingTo(confidence);
	}

	@Test
	@DisplayName("replacePredictedTypes: 5개 초과 입력 시 상위 5개만 저장된다")
	void replacePredictedTypes_limitsToFive() {
		// given
		Long scanId = 3L;
		List<PredictedType> input = List.of(
			new PredictedType("T1", BigDecimal.valueOf(0.9)),
			new PredictedType("T2", BigDecimal.valueOf(0.8)),
			new PredictedType("T3", BigDecimal.valueOf(0.7)),
			new PredictedType("T4", BigDecimal.valueOf(0.6)),
			new PredictedType("T5", BigDecimal.valueOf(0.5)),
			new PredictedType("T6", BigDecimal.valueOf(0.4))  // 6번째 → 잘려야 함
		);

		// when
		sut.replacePredictedTypes(scanId, input);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());

		List<TypePrediction> saved = captor.getValue();
		assertThat(saved).hasSize(5);
		assertThat(saved).extracting(TypePrediction::typeId)
			.containsExactly("T1", "T2", "T3", "T4", "T5");
	}

	@Test
	@DisplayName("replacePredictedTypes: 정확히 5개이면 전부 저장된다")
	void replacePredictedTypes_exactlyFiveIsNotTrimmed() {
		// given
		Long scanId = 4L;
		List<PredictedType> input = List.of(
			new PredictedType("T1", BigDecimal.valueOf(0.9)),
			new PredictedType("T2", BigDecimal.valueOf(0.8)),
			new PredictedType("T3", BigDecimal.valueOf(0.7)),
			new PredictedType("T4", BigDecimal.valueOf(0.6)),
			new PredictedType("T5", BigDecimal.valueOf(0.5))
		);

		// when
		sut.replacePredictedTypes(scanId, input);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());
		assertThat(captor.getValue()).hasSize(5);
	}

	@Test
	@DisplayName("replacePredictedTypes: 빈 리스트 입력 시 빈 리스트로 replacePredictions가 호출된다")
	void replacePredictedTypes_emptyList_callsWriterWithEmpty() {
		// given
		Long scanId = 5L;

		// when
		sut.replacePredictedTypes(scanId, List.of());

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());
		assertThat(captor.getValue()).isEmpty();
	}

	@Test
	@DisplayName("replacePredictedTypes: null 입력 시 빈 리스트로 replacePredictions가 호출된다")
	void replacePredictedTypes_nullList_callsWriterWithEmpty() {
		// given
		Long scanId = 6L;

		// when
		sut.replacePredictedTypes(scanId, null);

		// then
		ArgumentCaptor<List<TypePrediction>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionWriter).replacePredictions(eq(scanId), captor.capture());
		assertThat(captor.getValue()).isEmpty();
	}
}
