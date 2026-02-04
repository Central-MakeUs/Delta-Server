package cmc.delta.domain.problem.adapter.in.worker.support;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
public abstract class AbstractExternalCallScanWorker extends AbstractClaimingScanWorker {

	private final WorkerIdentity identity;

	private final ScanLockGuard lockGuard;
	private final ScanUnlocker unlocker;
	private final BacklogLogger backlogLogger;
	private final WorkerLogPolicy logPolicy;

	protected AbstractExternalCallScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		Executor executor,
		WorkerIdentity identity,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy) {
		super(clock, workerTxTemplate, executor, identity.name());
		this.identity = identity;
		this.lockGuard = lockGuard;
		this.unlocker = unlocker;
		this.backlogLogger = backlogLogger;
		this.logPolicy = logPolicy;
	}

	@Override
	protected final void onNoCandidate(LocalDateTime now) {
		log.debug("{} 워커 tick - 처리 대상 없음", identity.label());

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		backlogLogger.logIfDue(
			identity.backlogKey(),
			now,
			backlogLogMinutes(),
			() -> countBacklog(now, staleBefore),
			(backlog) -> log.info("{} 워커 - 처리 대상 없음 (backlog={})", identity.label(), backlog));
	}

	@Override
	protected final void onClaimed(LocalDateTime now, int count) {
		log.info("{} 워커 tick - 이번 배치 처리 대상={}건", identity.label(), count);
	}

	@Override
	protected final void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			handleSuccess(scanId, lockOwner, lockToken, batchNow);
		} catch (Exception exception) {
			FailureDecision decision = decideFailure(exception);
			persistFailed(scanId, lockOwner, lockToken, decision, batchNow);
			logFailure(scanId, decision, exception);
		} finally {
			unlocker.unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	protected final boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		return lockGuard.isOwned(scanId, lockOwner, lockToken);
	}

	private void logFailure(Long scanId, FailureDecision decision, Exception exception) {
		if (shouldSuppressStacktrace(exception)) {
			RestClientResponseException rest = (RestClientResponseException)exception;
			log.warn(
				"{} 호출 4xx scanId={} reason={} status={}",
				identity.label(),
				scanId,
				logPolicy.reasonCode(decision),
				rest.getRawStatusCode());
			return;
		}
		log.error("{} 처리 실패 scanId={} reason={}", identity.label(), scanId, logPolicy.reasonCode(decision), exception);
	}

	private boolean shouldSuppressStacktrace(Exception exception) {
		if (!(exception instanceof RestClientResponseException rest)) {
			return false;
		}
		return logPolicy.shouldSuppressStacktrace(rest);
	}

	protected abstract long backlogLogMinutes();

	protected abstract long countBacklog(LocalDateTime now, LocalDateTime staleBefore);

	protected abstract void handleSuccess(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow);

	protected abstract FailureDecision decideFailure(Exception exception);

	protected abstract void persistFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow);
}
