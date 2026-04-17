package cmc.delta.domain.problem.adapter.in.worker;

import cmc.delta.domain.problem.adapter.in.worker.properties.PurgeWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.AbstractExternalCallScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerIdentity;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.ScanPurgePersister;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class ScanPurgeWorker extends AbstractExternalCallScanWorker {

	private static final WorkerIdentity IDENTITY = new WorkerIdentity("purge", "PURGE", "worker:purge:backlog");

	private final ScanWorkRepository scanWorkRepository;
	private final AssetJpaRepository assetJpaRepository;
	private final StoragePort storagePort;
	private final ProblemRepositoryPort problemRepository;
	private final PurgeWorkerProperties properties;
	private final ScanPurgePersister persister;

	private static final ThreadLocal<Integer> retentionDaysHolder = new ThreadLocal<>();

	public ScanPurgeWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("purgeExecutor")
		Executor purgeExecutor,
		ScanWorkRepository scanWorkRepository,
		AssetJpaRepository assetJpaRepository,
		StoragePort storagePort,
		ProblemRepositoryPort problemRepository,
		PurgeWorkerProperties properties,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy,
		ScanPurgePersister persister) {
		super(clock, workerTxTemplate, purgeExecutor, IDENTITY, lockGuard, unlocker, backlogLogger, logPolicy);
		this.scanWorkRepository = scanWorkRepository;
		this.assetJpaRepository = assetJpaRepository;
		this.storagePort = storagePort;
		this.problemRepository = problemRepository;
		this.properties = properties;
		this.persister = persister;
	}

	public void runBatch(String lockOwner, int batchSize, long lockLeaseSeconds, int retentionDays) {
		retentionDaysHolder.set(retentionDays);
		try {
			super.runBatch(lockOwner, batchSize, lockLeaseSeconds);
		} finally {
			retentionDaysHolder.remove();
		}
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken,
		LocalDateTime lockedAt, int limit) {
		LocalDateTime cutoffCreatedAt = now.minusDays(currentRetentionDays());
		return scanWorkRepository.claimPurgeCandidates(cutoffCreatedAt, staleBefore, lockOwner, lockToken, lockedAt,
			limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanWorkRepository.findClaimedIds(lockOwner, lockToken, limit);
	}

	@Override
	protected long backlogLogMinutes() {
		return properties.backlogLogMinutes();
	}

	@Override
	protected long countBacklog(LocalDateTime now, LocalDateTime staleBefore) {
		LocalDateTime cutoffCreatedAt = now.minusDays(currentRetentionDays());
		return scanWorkRepository.countPurgeBacklog(cutoffCreatedAt, staleBefore);
	}

	private int currentRetentionDays() {
		Integer days = retentionDaysHolder.get();
		return days != null ? days : properties.retentionDays();
	}

	@Override
	protected void handleSuccess(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		List<Asset> assets = assetJpaRepository.findAllByScan_Id(scanId);
		int deletedCount = 0;
		for (Asset asset : assets) {
			if (problemRepository.existsByOriginalStorageKey(asset.getStorageKey())) {
				continue;
			}
			try {
				storagePort.deleteImage(asset.getStorageKey());
				deletedCount += 1;
			} catch (Exception e) {
				log.warn("{} purge - S3 삭제 실패 scanId={} storageKey={}", IDENTITY.label(), scanId,
					asset.getStorageKey(), e);
			}
		}

		if (!isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		persister.purgeIfLocked(scanId, lockOwner, lockToken);
		log.debug("{} purge 완료 scanId={} deletedAssets={}", IDENTITY.label(), scanId, deletedCount);
	}

	@Override
	protected FailureDecision decideFailure(Exception exception) {
		return FailureDecision.retryable(FailureReason.SCAN_NOT_FOUND);
	}

	@Override
	protected void persistFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow) {
		// purge 실패는 lock 만료 후 다음 배치에서 자동 재시도되므로 별도 상태 변경 없음
	}
}
