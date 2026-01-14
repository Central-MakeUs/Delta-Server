package cmc.delta.domain.problem.application.worker.support;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public abstract class AbstractScanWorker {

	private final Clock clock;

	protected final LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	@Transactional
	public final void runOnce(String lockOwner) {
		LocalDateTime now = now();

		Long scanId = pickCandidateId(now);
		if (scanId == null) return;

		boolean locked = tryLock(scanId, lockOwner, now);
		if (!locked) return;

		try {
			processLocked(scanId, now);
		} catch (Exception e) {
			handleFailure(scanId, now, e);
		} finally {
			unlock(scanId, lockOwner);
		}
	}

	protected abstract Long pickCandidateId(LocalDateTime now);

	protected abstract boolean tryLock(Long scanId, String lockOwner, LocalDateTime now);

	protected abstract void processLocked(Long scanId, LocalDateTime now);

	protected abstract void handleFailure(Long scanId, LocalDateTime now, Exception e);

	protected abstract void unlock(Long scanId, String lockOwner);
}
