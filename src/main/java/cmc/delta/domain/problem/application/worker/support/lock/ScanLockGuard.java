package cmc.delta.domain.problem.application.worker.support.lock;

import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class ScanLockGuard {

	private final ProblemScanJpaRepository problemScanRepository;

	public ScanLockGuard(ProblemScanJpaRepository problemScanRepository) {
		this.problemScanRepository = problemScanRepository;
	}

	public boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		Integer exists = problemScanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}
}
