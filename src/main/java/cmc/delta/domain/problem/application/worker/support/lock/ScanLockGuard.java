package cmc.delta.domain.problem.application.worker.support.lock;

import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;

public class ScanLockGuard {

	private final ProblemScanJpaRepository scanRepository;

	public ScanLockGuard(ProblemScanJpaRepository scanRepository) {
		this.scanRepository = scanRepository;
	}

	public boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}
}
