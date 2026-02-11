package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.prediction.ProblemScanTypePredictionJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ScanPurgePersister {

	private final TransactionTemplate workerTransactionTemplate;
	private final ScanWorkRepository scanWorkRepository;
	private final ScanRepository scanRepository;
	private final AssetJpaRepository assetJpaRepository;
	private final ProblemScanTypePredictionJpaRepository predictionRepository;
	private final ProblemRepositoryPort problemRepository;

	public ScanPurgePersister(
		TransactionTemplate workerTransactionTemplate,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository,
		AssetJpaRepository assetJpaRepository,
		ProblemScanTypePredictionJpaRepository predictionRepository,
		ProblemRepositoryPort problemRepository) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.scanWorkRepository = scanWorkRepository;
		this.scanRepository = scanRepository;
		this.assetJpaRepository = assetJpaRepository;
		this.predictionRepository = predictionRepository;
		this.problemRepository = problemRepository;
	}

	public void purgeIfLocked(Long scanId, String lockOwner, String lockToken) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) {
				return;
			}
			detachProblemIfExists(scanId);

			Optional<ProblemScan> optional = scanRepository.findById(scanId);
			if (optional.isEmpty()) {
				return;
			}

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
			Optional<Asset> original = assetJpaRepository.findOriginalByScanId(scanId);
			if (original.isEmpty()) {
				return;
			}
			problem.attachOriginalStorageKeyIfEmpty(original.get().getStorageKey());
		}

		problem.detachScan();
		problemRepository.save(problem);
	}

	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		return scanWorkRepository.existsLockedBy(scanId, lockOwner, lockToken) != null;
	}
}
