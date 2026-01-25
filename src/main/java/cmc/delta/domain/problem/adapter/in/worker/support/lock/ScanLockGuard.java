package cmc.delta.domain.problem.adapter.in.worker.support.lock;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import org.springframework.stereotype.Component;

@Component
public class ScanLockGuard {

	private final ScanWorkRepository scanWorkRepository;

	public ScanLockGuard(ScanWorkRepository scanWorkRepository) {
		this.scanWorkRepository = scanWorkRepository;
	}

	public boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanWorkRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}
}
