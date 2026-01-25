package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OcrScanPersister {

	private final TransactionTemplate workerTransactionTemplate;
	private final ScanWorkRepository scanWorkRepository;
	private final ScanRepository scanRepository;

	public OcrScanPersister(
		TransactionTemplate workerTransactionTemplate,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.scanWorkRepository = scanWorkRepository;
		this.scanRepository = scanRepository;
	}

	public void persistOcrSucceeded(
		Long scanId,
		String lockOwner,
		String lockToken,
		OcrResult ocrResult,
		LocalDateTime completedAt) {
		inWorkerTxIfLocked(scanId, lockOwner, lockToken,
			scan -> scan.markOcrSucceeded(ocrResult.plainText(), ocrResult.rawJson(), completedAt));
	}

	public void persistOcrFailed(
		Long scanId,
		String lockOwner,
		String lockToken,
		FailureDecision decision,
		LocalDateTime now) {
		inWorkerTxIfLocked(scanId, lockOwner, lockToken, scan -> applyFailure(scan, decision, now));
	}

	private void inWorkerTxIfLocked(
		Long scanId,
		String lockOwner,
		String lockToken,
		java.util.function.Consumer<ProblemScan> action) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken))
				return;

			Optional<ProblemScan> optional = scanRepository.findById(scanId);
			if (optional.isEmpty())
				return;

			action.accept(optional.get());
		});
	}

	private void applyFailure(ProblemScan scan, FailureDecision decision, LocalDateTime now) {
		String reason = decision.reasonCode().code();
		scan.markOcrFailed(reason);

		if (!decision.retryable()) {
			scan.markFailed(reason);
			return;
		}

		if (decision.reasonCode() == FailureReason.OCR_RATE_LIMIT) {
			long delay = (decision.retryAfterSeconds() == null) ? 180L : decision.retryAfterSeconds().longValue();
			scan.scheduleNextRetryForOcr(now, delay);
			return;
		}

		scan.scheduleNextRetryForOcr(now);
	}

	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		return scanWorkRepository.existsLockedBy(scanId, lockOwner, lockToken) != null;
	}
}
