package cmc.delta.domain.problem.application.port.out.prediction;

import java.math.BigDecimal;
import java.util.List;

public interface ScanTypePredictionWriter {

	void replacePredictions(Long scanId, List<TypePrediction> predictions);

	record TypePrediction(String typeId, int rankNo, BigDecimal confidence) {
	}
}
