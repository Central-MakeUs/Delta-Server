package cmc.delta.domain.problem.application.worker.support.lock;

import cmc.delta.domain.problem.persistence.scan.ScanRepository;

import org.springframework.stereotype.Component;

@Component
public class ScanLockGuard {

	private final ScanRepository problemScanRepository;

	public ScanLockGuard(ScanRepository problemScanRepository) {
		this.problemScanRepository = problemScanRepository;
	}

	public boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		Integer exists = problemScanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}
}
