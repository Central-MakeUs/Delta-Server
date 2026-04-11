package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.function.Consumer;
import org.springframework.transaction.support.TransactionTemplate;

abstract class AbstractScanPersister {

	private static final long DEFAULT_RETRY_AFTER_SECONDS = 180L;

	protected final TransactionTemplate workerTx;
	protected final ScanWorkRepository scanWorkRepository;
	protected final ScanRepository scanRepository;

	protected AbstractScanPersister(
		TransactionTemplate workerTx,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository) {
		this.workerTx = workerTx;
		this.scanWorkRepository = scanWorkRepository;
		this.scanRepository = scanRepository;
	}

	protected void inLockedTx(
		Long scanId,
		String lockOwner,
		String lockToken,
		Consumer<ProblemScan> action) {
		workerTx.executeWithoutResult(tx -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) {
				return;
			}
			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) {
				return;
			}
			action.accept(scan);
		});
	}

	protected long resolveRetryAfterSeconds(FailureDecision decision) {
		Long retryAfter = decision.retryAfterSeconds();
		return retryAfter == null ? DEFAULT_RETRY_AFTER_SECONDS : retryAfter;
	}

	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		return scanWorkRepository.existsLockedBy(scanId, lockOwner, lockToken) != null;
	}
}
