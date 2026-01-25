package cmc.delta.domain.problem.adapter.out.persistence.scan.prediction;

import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.scan.ProblemScanTypePrediction;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScanTypePredictionPersistenceAdapter implements ScanTypePredictionWriter, ScanTypePredictionReader {

	private static final int MAX_PREDICTED_TYPES = 5;

	private final ScanRepository scanRepository;
	private final ProblemTypeLoadPort problemTypeReader;
	private final ProblemScanTypePredictionJpaRepository predictionRepository;

	@Override
	@Transactional
	public void replacePredictions(Long scanId, List<TypePrediction> predictions) {
		ProblemScan scan = scanRepository.findById(scanId).orElse(null);
		if (scan == null)
			return;

		predictionRepository.deleteAllByScan_Id(scanId);

		List<TypePrediction> top = predictions == null ? List.of()
			: predictions.stream().limit(MAX_PREDICTED_TYPES).toList();

		List<ProblemScanTypePrediction> rows = new ArrayList<>();
		int rank = 1;
		for (TypePrediction p : top) {
			ProblemType type = problemTypeReader.findById(p.typeId()).orElse(null);
			if (type == null)
				continue;

			rows.add(new ProblemScanTypePrediction(scan, type, rank++, p.confidence()));
		}

		predictionRepository.saveAll(rows);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TypePredictionView> findByScanId(Long scanId) {
		return predictionRepository.findAllByScan_IdOrderByRankNoAsc(scanId).stream()
			.map(p -> new TypePredictionView(
				p.getType().getId(),
				p.getType().getName(),
				p.getRankNo(),
				p.getConfidence()))
			.toList();
	}
}
