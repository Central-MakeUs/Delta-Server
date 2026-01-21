package cmc.delta.domain.problem.adapter.out.persistence.scan.prediction;

import cmc.delta.domain.problem.model.scan.ProblemScanTypePrediction;
import cmc.delta.domain.problem.model.scan.ProblemScanTypePredictionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemScanTypePredictionJpaRepository
	extends JpaRepository<ProblemScanTypePrediction, ProblemScanTypePredictionId> {

	void deleteAllByScan_Id(Long scanId);

	List<ProblemScanTypePrediction> findAllByScan_IdOrderByRankNoAsc(Long scanId);
}
