package cmc.delta.domain.problem.adapter.in.worker.support.lock;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class ScanUnlocker {

	private final TransactionTemplate workerTransactionTemplate;
	private final ScanWorkRepository scanWorkRepository;

	public ScanUnlocker(
		TransactionTemplate workerTransactionTemplate,
		ScanWorkRepository scanWorkRepository) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.scanWorkRepository = scanWorkRepository;
	}

	public void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			unlock(scanId, lockOwner, lockToken);
		} catch (Exception unlockException) {
			log.debug("best-effort unlock failed scanId={} lockOwner={}", scanId, lockOwner, unlockException);
		}
	}

	private void unlock(Long scanId, String lockOwner, String lockToken) {
		workerTransactionTemplate.executeWithoutResult(
			status -> scanWorkRepository.unlock(scanId, lockOwner, lockToken));
	}
}
