package cmc.delta.domain.problem.application.port.out.prediction;

import java.math.BigDecimal;
import java.util.List;

public interface ScanTypePredictionReader {

	List<TypePredictionView> findByScanId(Long scanId);

	record TypePredictionView(String typeId, String typeName, int rankNo, BigDecimal confidence) {}
}
