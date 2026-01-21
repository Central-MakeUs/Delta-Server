package cmc.delta.domain.problem.application.port.out.persistence;

import java.math.BigDecimal;
import java.util.List;

public interface ScanTypePredictionPort {

	void replacePredictions(Long scanId, List<TypePrediction> predictions);

	List<TypePredictionView> findByScanId(Long scanId);

	record TypePrediction(String typeId, int rankNo, BigDecimal confidence) {}
	record TypePredictionView(String typeId, String typeName, int rankNo, BigDecimal confidence) {}
}
