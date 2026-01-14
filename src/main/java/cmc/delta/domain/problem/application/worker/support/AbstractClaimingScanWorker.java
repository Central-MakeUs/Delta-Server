package cmc.delta.domain.problem.application.worker.support;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractClaimingScanWorker {

	private final Clock clock;
	private final TransactionTemplate tx;
	private final Executor executor;

	protected AbstractClaimingScanWorker(Clock clock, TransactionTemplate tx, Executor executor) {
		this.clock = Objects.requireNonNull(clock);
		this.tx = Objects.requireNonNull(tx);
		this.executor = Objects.requireNonNull(executor);
	}

	protected final LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	public final void runBatch(String lockOwner, int batchSize, long lockLeaseSeconds) {
		LocalDateTime batchNow = now();
		LocalDateTime staleBefore = batchNow.minusSeconds(lockLeaseSeconds);
		LocalDateTime lockedAt = batchNow;
		String lockToken = createLockToken();

		List<Long> ids = tx.execute(status -> {
			int claimed = claim(batchNow, staleBefore, lockOwner, lockToken, lockedAt, batchSize);
			if (claimed <= 0) return List.of();
			return findClaimedIds(lockOwner, lockToken, batchSize);
		});

		if (ids == null || ids.isEmpty()) {
			onNoCandidate(batchNow);
			return;
		}

		onClaimed(batchNow, ids.size());

		CompletableFuture<?>[] futures = ids.stream()
			.map(id -> CompletableFuture.runAsync(() -> {
				try {
					processOne(id, lockOwner, lockToken, batchNow);
				} catch (Exception e) {
					// processOne 내부에서 처리하는게 원칙이지만, 안전망
					log.error("worker processOne unexpected error scanId={}", id, e);
				}
			}, executor))
			.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();
	}

	protected void onNoCandidate(LocalDateTime now) {}
	protected void onClaimed(LocalDateTime now, int count) {}

	protected abstract int claim(
		LocalDateTime now,
		LocalDateTime staleBefore,
		String lockOwner,
		String lockToken,
		LocalDateTime lockedAt,
		int limit
	);

	protected abstract List<Long> findClaimedIds(String lockOwner, String lockToken, int limit);

	/**
	 * 외부 호출(트랜잭션 밖) + DB 저장(txTemplate로 짧게)
	 */
	protected abstract void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow);

	private String createLockToken() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
	}
}
