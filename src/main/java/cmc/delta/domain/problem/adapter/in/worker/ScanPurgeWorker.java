package cmc.delta.domain.problem.adapter.in.worker;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import cmc.delta.domain.problem.adapter.in.worker.properties.PurgeWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerIdentity;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.ScanPurgePersister;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.global.storage.port.out.StoragePort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScanPurgeWorker extends AbstractClaimingScanWorker {

	private static final WorkerIdentity IDENTITY = new WorkerIdentity("purge", "PURGE", "worker:purge:backlog");

	private final ScanWorkRepository scanWorkRepository;
	private final AssetJpaRepository assetJpaRepository;
	private final StoragePort storagePort;
	private final ProblemRepositoryPort problemRepository;
	private final PurgeWorkerProperties properties;

	private final ScanLockGuard lockGuard;
	private final ScanUnlocker unlocker;
	private final BacklogLogger backlogLogger;
	private final ScanPurgePersister persister;

	private volatile int lastRetentionDays;

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
		ScanPurgePersister persister) {
		super(clock, workerTxTemplate, purgeExecutor, IDENTITY.name());
		this.scanWorkRepository = Objects.requireNonNull(scanWorkRepository);
		this.assetJpaRepository = Objects.requireNonNull(assetJpaRepository);
		this.storagePort = Objects.requireNonNull(storagePort);
		this.problemRepository = Objects.requireNonNull(problemRepository);
		this.properties = Objects.requireNonNull(properties);
		this.lockGuard = Objects.requireNonNull(lockGuard);
		this.unlocker = Objects.requireNonNull(unlocker);
		this.backlogLogger = Objects.requireNonNull(backlogLogger);
		this.persister = Objects.requireNonNull(persister);
		this.lastRetentionDays = properties.retentionDays();
	}

	public void runBatch(String lockOwner, int batchSize, long lockLeaseSeconds, int retentionDays) {
		this.lastRetentionDays = retentionDays;
		super.runBatch(lockOwner, batchSize, lockLeaseSeconds);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		LocalDateTime cutoffCreatedAt = now.minusDays(lastRetentionDays);
		backlogLogger.logIfDue(
			IDENTITY.backlogKey(),
			now,
			properties.backlogLogMinutes(),
			() -> scanWorkRepository.countPurgeBacklog(cutoffCreatedAt, staleBefore),
			(backlog) -> log.debug("{} 워커 - purge 대상 없음 (backlog={})", IDENTITY.label(), backlog));
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken,
		LocalDateTime lockedAt, int limit) {
		LocalDateTime cutoffCreatedAt = now.minusDays(lastRetentionDays);
		return scanWorkRepository.claimPurgeCandidates(cutoffCreatedAt, staleBefore, lockOwner, lockToken, lockedAt,
			limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanWorkRepository.findClaimedIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			purgeOne(scanId, lockOwner, lockToken);
		} catch (Exception e) {
			log.error("{} purge 실패 scanId={}", IDENTITY.label(), scanId, e);
		} finally {
			unlocker.unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private boolean isOwned(Long scanId, String lockOwner, String lockToken) {
		return lockGuard.isOwned(scanId, lockOwner, lockToken);
	}

	private void purgeOne(Long scanId, String lockOwner, String lockToken) {
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
}
