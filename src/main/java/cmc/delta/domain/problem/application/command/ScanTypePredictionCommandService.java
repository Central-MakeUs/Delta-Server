package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScanTypePredictionCommandService {

	private static final int MAX_PREDICTED_TYPES = 5;
	private static final int FIRST_RANK = 1;

	private final ScanTypePredictionWriter predictionWriter;

	@Transactional
	public void replacePredictedTypes(Long scanId, List<PredictedType> predictedTypes) {
		List<PredictedType> top = limitTypes(predictedTypes);
		List<ScanTypePredictionWriter.TypePrediction> rows = buildPredictions(top);
		predictionWriter.replacePredictions(scanId, rows);
	}

	private List<PredictedType> limitTypes(List<PredictedType> predictedTypes) {
		if (predictedTypes == null) {
			return List.of();
		}
		return predictedTypes.stream().limit(MAX_PREDICTED_TYPES).toList();
	}

	private List<ScanTypePredictionWriter.TypePrediction> buildPredictions(List<PredictedType> predictedTypes) {
		List<ScanTypePredictionWriter.TypePrediction> rows = new ArrayList<>();
		for (int i = 0; i < predictedTypes.size(); i++) {
			PredictedType predictedType = predictedTypes.get(i);
			rows.add(new ScanTypePredictionWriter.TypePrediction(
				predictedType.typeId(),
				i + FIRST_RANK,
				predictedType.confidence()));
		}
		return rows;
	}

	public record PredictedType(String typeId, BigDecimal confidence) {
	}
}
