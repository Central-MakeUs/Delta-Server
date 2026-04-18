package cmc.delta.domain.problem.adapter.out.persistence.scan.prediction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader.TypePredictionView;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter.TypePrediction;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.scan.ProblemScanTypePrediction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScanTypePredictionPersistenceAdapterTest {

	@Test
	@DisplayName("예측 저장: scan이 없으면 아무 것도 하지 않음")
	void replacePredictions_whenScanMissing_thenNoop() {
		// given
		ScanRepository scanRepository = mock(ScanRepository.class);
		ProblemTypeLoadPort typeReader = mock(ProblemTypeLoadPort.class);
		ProblemScanTypePredictionJpaRepository predictionRepository = mock(
			ProblemScanTypePredictionJpaRepository.class);
		ScanTypePredictionPersistenceAdapter sut = new ScanTypePredictionPersistenceAdapter(
			scanRepository,
			typeReader,
			predictionRepository);

		when(scanRepository.findById(1L)).thenReturn(Optional.empty());

		// when
		sut.replacePredictions(1L, List.of(new TypePrediction("T1", 1, BigDecimal.ONE)));

		// then
		verify(predictionRepository, never()).deleteAllByScan_Id(any());
		verify(predictionRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("예측 저장: predictions가 null이면 기존 삭제 후 빈 리스트 저장")
	void replacePredictions_whenPredictionsNull_thenDeletesAndSavesEmpty() {
		// given
		ScanRepository scanRepository = mock(ScanRepository.class);
		ProblemTypeLoadPort typeReader = mock(ProblemTypeLoadPort.class);
		ProblemScanTypePredictionJpaRepository predictionRepository = mock(
			ProblemScanTypePredictionJpaRepository.class);
		ScanTypePredictionPersistenceAdapter sut = new ScanTypePredictionPersistenceAdapter(
			scanRepository,
			typeReader,
			predictionRepository);

		ProblemScan scan = mock(ProblemScan.class);
		when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));

		// when
		sut.replacePredictions(1L, null);

		// then
		verify(predictionRepository).deleteAllByScan_Id(1L);
		verify(predictionRepository).saveAll(List.of());
	}

	@Test
	@DisplayName("예측 저장: 최대 5개까지만 저장하고, 존재하지 않는 type은 스킵")
	void replacePredictions_limitsAndSkipsMissingTypes() {
		// given
		ScanRepository scanRepository = mock(ScanRepository.class);
		ProblemTypeLoadPort typeReader = mock(ProblemTypeLoadPort.class);
		ProblemScanTypePredictionJpaRepository predictionRepository = mock(
			ProblemScanTypePredictionJpaRepository.class);
		ScanTypePredictionPersistenceAdapter sut = new ScanTypePredictionPersistenceAdapter(
			scanRepository,
			typeReader,
			predictionRepository);

		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(1L);
		when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));

		// top 5까지만 고려: T1~T5, T6는 무시
		List<TypePrediction> preds = List.of(
			new TypePrediction("T1", 1, new BigDecimal("0.9")),
			new TypePrediction("T2", 2, new BigDecimal("0.8")),
			new TypePrediction("T3", 3, new BigDecimal("0.7")),
			new TypePrediction("T4", 4, new BigDecimal("0.6")),
			new TypePrediction("T5", 5, new BigDecimal("0.5")),
			new TypePrediction("T6", 6, new BigDecimal("0.4")));

		ProblemType t1 = mockType("T1");
		ProblemType t2 = mockType("T2");
		ProblemType t3 = mockType("T3");
		// T4는 없는 타입
		ProblemType t5 = mockType("T5");

		// 구현체는 findByIds로 IN절 일괄 조회 — T4는 결과에 포함되지 않음
		when(typeReader.findByIds(List.of("T1", "T2", "T3", "T4", "T5")))
			.thenReturn(List.of(t1, t2, t3, t5));
		// T6는 호출되면 안 됨(상위 5개 제한)

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<List<ProblemScanTypePrediction>> captor = org.mockito.ArgumentCaptor
			.forClass(List.class);

		// when
		sut.replacePredictions(1L, preds);

		// then
		verify(predictionRepository).deleteAllByScan_Id(1L);
		verify(predictionRepository).saveAll(captor.capture());

		List<ProblemScanTypePrediction> saved = captor.getValue();
		// T4 스킵되므로 4개
		assertThat(saved).hasSize(4);
		assertThat(saved).extracting(ProblemScanTypePrediction::getRankNo)
			.containsExactly(1, 2, 3, 4);
		assertThat(saved).extracting(p -> p.getType().getId()).containsExactly("T1", "T2", "T3", "T5");
		verify(typeReader, never()).findById("T6");
	}

	@Test
	@DisplayName("예측 조회: entity를 view로 매핑")
	void findByScanId_mapsToViews() {
		// given
		ScanRepository scanRepository = mock(ScanRepository.class);
		ProblemTypeLoadPort typeReader = mock(ProblemTypeLoadPort.class);
		ProblemScanTypePredictionJpaRepository predictionRepository = mock(
			ProblemScanTypePredictionJpaRepository.class);
		ScanTypePredictionPersistenceAdapter sut = new ScanTypePredictionPersistenceAdapter(
			scanRepository,
			typeReader,
			predictionRepository);

		ProblemType type = mock(ProblemType.class);
		when(type.getId()).thenReturn("T1");
		when(type.getName()).thenReturn("유형");

		ProblemScanTypePrediction row = mock(ProblemScanTypePrediction.class);
		when(row.getType()).thenReturn(type);
		when(row.getRankNo()).thenReturn(1);
		when(row.getConfidence()).thenReturn(new BigDecimal("0.9"));

		when(predictionRepository.findAllByScan_IdOrderByRankNoAsc(1L)).thenReturn(List.of(row));

		// when
		List<TypePredictionView> out = sut.findByScanId(1L);

		// then
		assertThat(out).containsExactly(new TypePredictionView("T1", "유형", 1, new BigDecimal("0.9")));
	}

	private ProblemType mockType(String id) {
		ProblemType t = mock(ProblemType.class);
		when(t.getId()).thenReturn(id);
		return t;
	}
}
