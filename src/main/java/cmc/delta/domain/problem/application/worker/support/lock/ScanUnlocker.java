package cmc.delta.domain.problem.application.worker.support.lock;

import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class ScanUnlocker {

	private final TransactionTemplate workerTransactionTemplate;
	private final ScanRepository problemScanRepository;

	public ScanUnlocker(
		TransactionTemplate workerTransactionTemplate,
		ScanRepository problemScanRepository
	) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.problemScanRepository = problemScanRepository;
	}

	public void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			workerTransactionTemplate.executeWithoutResult(status ->
				problemScanRepository.unlock(scanId, lockOwner, lockToken)
			);
		} catch (Exception unlockException) {
			log.debug("best-effort unlock failed scanId={} lockOwner={}", scanId, lockOwner, unlockException);
		}
	}
}
