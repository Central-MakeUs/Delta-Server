package cmc.delta.domain.problem.application.worker.support.persistence;

import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OcrScanPersister {

	private final TransactionTemplate workerTransactionTemplate;
	private final ProblemScanJpaRepository problemScanRepository;

	public OcrScanPersister(
		TransactionTemplate workerTransactionTemplate,
		ProblemScanJpaRepository problemScanRepository
	) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.problemScanRepository = problemScanRepository;
	}

	public void persistOcrSucceeded(
		Long scanId,
		String lockOwner,
		String lockToken,
		OcrResult ocrResult,
		LocalDateTime completedAt
	) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = problemScanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			scan.markOcrSucceeded(ocrResult.plainText(), ocrResult.rawJson(), completedAt);
		});
	}

	public void persistOcrFailed(
		Long scanId,
		String lockOwner,
		String lockToken,
		FailureDecision decision,
		LocalDateTime now
	) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = problemScanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			String reason = decision.reasonCode().code();
			scan.markOcrFailed(reason);

			if (!decision.retryable()) {
				scan.markFailed(reason);
				return;
			}

			scan.scheduleNextRetryForOcr(now);
		});
	}

	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = problemScanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}
}
