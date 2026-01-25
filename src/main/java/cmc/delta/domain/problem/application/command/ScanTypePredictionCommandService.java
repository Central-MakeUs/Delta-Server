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

	private final ScanTypePredictionWriter predictionWriter;

	@Transactional
	public void replacePredictedTypes(Long scanId, List<PredictedType> predictedTypes) {
		List<PredictedType> top = predictedTypes == null ? List.of()
			: predictedTypes.stream().limit(MAX_PREDICTED_TYPES).toList();

		List<ScanTypePredictionWriter.TypePrediction> rows = new ArrayList<>();
		for (int i = 0; i < top.size(); i++) {
			PredictedType t = top.get(i);
			rows.add(new ScanTypePredictionWriter.TypePrediction(t.typeId(), i + 1, t.confidence()));
		}

		predictionWriter.replacePredictions(scanId, rows);
	}

	public record PredictedType(String typeId, BigDecimal confidence) {
	}
}
