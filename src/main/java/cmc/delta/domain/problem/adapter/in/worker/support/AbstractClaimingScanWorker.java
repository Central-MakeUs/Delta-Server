package cmc.delta.domain.problem.adapter.in.worker.support;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractClaimingScanWorker {

	private static final String MDC_TRACE_ID = "traceId";
	private static final String MDC_WORKER = "worker";

	private final Clock clock;
	private final TransactionTemplate tx;
	private final Executor executor;
	private final String workerName;

	private volatile long lastLockLeaseSeconds = 0L;

	protected AbstractClaimingScanWorker(Clock clock, TransactionTemplate tx, Executor executor, String workerName) {
		this.clock = Objects.requireNonNull(clock);
		this.tx = Objects.requireNonNull(tx);
		this.executor = Objects.requireNonNull(executor);
		this.workerName = Objects.requireNonNull(workerName);
	}

	protected final LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	protected final long lockLeaseSeconds() {
		return lastLockLeaseSeconds;
	}

	public final void runBatch(String lockOwner, int batchSize, long lockLeaseSeconds) {
		this.lastLockLeaseSeconds = lockLeaseSeconds;

		String traceId = UUID.randomUUID().toString().replace("-", "");
		MDC.put(MDC_TRACE_ID, traceId);
		MDC.put(MDC_WORKER, workerName);

		try {
			LocalDateTime batchNow = now();
			LocalDateTime staleBefore = batchNow.minusSeconds(lockLeaseSeconds);
			LocalDateTime lockedAt = batchNow;
			String lockToken = createLockToken();

			List<Long> ids = tx.execute(status -> {
				int claimed = claim(batchNow, staleBefore, lockOwner, lockToken, lockedAt, batchSize);
				if (claimed <= 0)
					return List.of();
				return findClaimedIds(lockOwner, lockToken, batchSize);
			});

			if (ids == null || ids.isEmpty()) {
				onNoCandidate(batchNow);
				return;
			}

			onClaimed(batchNow, ids.size());

			Map<String, String> parentMdc = MDC.getCopyOfContextMap();

			CompletableFuture<?>[] futures = ids.stream()
				.map(id -> CompletableFuture.runAsync(() -> {
					if (parentMdc != null) {
						MDC.setContextMap(parentMdc);
					}
					try {
						processOne(id, lockOwner, lockToken, batchNow);
					} catch (Exception e) {
						log.error("worker processOne unexpected error scanId={}", id, e);
					} finally {
						MDC.clear();
					}
				}, executor))
				.toArray(CompletableFuture[]::new);

			CompletableFuture.allOf(futures).join();

		} finally {
			MDC.remove(MDC_WORKER);
			MDC.remove(MDC_TRACE_ID);
		}
	}

	protected void onNoCandidate(LocalDateTime now) {}

	protected void onClaimed(LocalDateTime now, int count) {}

	protected abstract int claim(
		LocalDateTime now,
		LocalDateTime staleBefore,
		String lockOwner,
		String lockToken,
		LocalDateTime lockedAt,
		int limit);

	protected abstract List<Long> findClaimedIds(String lockOwner, String lockToken, int limit);

	protected abstract void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow);

	private String createLockToken() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
	}
}
