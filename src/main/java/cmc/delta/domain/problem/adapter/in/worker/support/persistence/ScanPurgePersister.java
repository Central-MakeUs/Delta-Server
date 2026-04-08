package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.prediction.ProblemScanTypePredictionJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ScanPurgePersister extends AbstractScanPersister {

	private final AssetJpaRepository assetJpaRepository;
	private final ProblemScanTypePredictionJpaRepository predictionRepository;
	private final ProblemRepositoryPort problemRepository;

	public ScanPurgePersister(
		TransactionTemplate workerTx,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository,
		AssetJpaRepository assetJpaRepository,
		ProblemScanTypePredictionJpaRepository predictionRepository,
		ProblemRepositoryPort problemRepository) {
		super(workerTx, scanWorkRepository, scanRepository);
		this.assetJpaRepository = assetJpaRepository;
		this.predictionRepository = predictionRepository;
		this.problemRepository = problemRepository;
	}

	public void purgeIfLocked(Long scanId, String lockOwner, String lockToken) {
		inLockedTx(scanId, lockOwner, lockToken, scan -> {
			detachProblemIfExists(scanId);
			predictionRepository.deleteAllByScan_Id(scanId);
			assetJpaRepository.deleteAllByScan_Id(scanId);
			scanRepository.deleteById(scanId);
		});
	}

	private void detachProblemIfExists(Long scanId) {
		Optional<Problem> optionalProblem = problemRepository.findByScanId(scanId);
		if (optionalProblem.isEmpty()) {
			return;
		}

		Problem problem = optionalProblem.get();
		if (problem.getOriginalStorageKey() == null || problem.getOriginalStorageKey().isBlank()) {
			assetJpaRepository.findOriginalByScanId(scanId)
				.ifPresent(asset -> problem.attachOriginalStorageKeyIfEmpty(asset.getStorageKey()));
		}

		problem.detachScan();
		problemRepository.save(problem);
	}

}
