package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OcrScanPersister extends AbstractScanPersister {

	public OcrScanPersister(
		TransactionTemplate workerTx,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository) {
		super(workerTx, scanWorkRepository, scanRepository);
	}

	public void persistOcrSucceeded(
		Long scanId,
		String lockOwner,
		String lockToken,
		OcrResult ocrResult,
		LocalDateTime completedAt) {
		inLockedTx(scanId, lockOwner, lockToken,
			scan -> scan.markOcrSucceeded(ocrResult.plainText(), ocrResult.rawJson(), completedAt));
	}

	public void persistOcrFailed(
		Long scanId,
		String lockOwner,
		String lockToken,
		FailureDecision decision,
		LocalDateTime now) {
		inLockedTx(scanId, lockOwner, lockToken, scan -> applyFailure(scan, decision, now));
	}

	private void applyFailure(ProblemScan scan, FailureDecision decision, LocalDateTime now) {
		String reason = decision.reasonCode().code();
		scan.markOcrFailed(reason);

		if (!decision.retryable()) {
			scan.markFailed(reason);
			return;
		}

		if (decision.reasonCode() == FailureReason.OCR_RATE_LIMIT) {
			scan.scheduleNextRetryForOcr(now, resolveRetryAfterSeconds(decision));
			return;
		}

		scan.scheduleNextRetryForOcr(now);
	}
}
