package cmc.delta.domain.problem.application.worker.support;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractScanWorker {

	private final Clock clock;

	protected AbstractScanWorker(Clock clock) {
		if (clock == null) {
			throw new IllegalArgumentException("clock must not be null");
		}
		this.clock = clock;
	}

	protected final void runOnceInternal(String lockOwner) {
		LocalDateTime now = LocalDateTime.now(clock);

		Long scanId = pickCandidateId(now);
		if (scanId == null) {
			return;
		}

		boolean locked = false;
		try {
			locked = tryLock(scanId, lockOwner, now);
			if (!locked) {
				// 레이스로 다른 워커가 선점했을 수 있음
				return;
			}

			processLocked(scanId, now);
		} catch (Exception e) {
			handleFailure(scanId, now, e);
		} finally {
			if (locked) {
				try {
					unlock(scanId, lockOwner);
				} catch (Exception unlockEx) {
					// 락 해제 실패는 운영상 치명적
					log.error("워커 락 해제 실패 scanId={}", scanId, unlockEx);
				}
			}
		}
	}

	protected abstract Long pickCandidateId(LocalDateTime now);

	protected abstract boolean tryLock(Long scanId, String lockOwner, LocalDateTime now);

	protected abstract void processLocked(Long scanId, LocalDateTime now);

	protected abstract void handleFailure(Long scanId, LocalDateTime now, Exception e);

	protected abstract void unlock(Long scanId, String lockOwner);
}
