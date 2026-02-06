package cmc.delta.domain.problem.adapter.in.worker;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.properties.PurgeWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.ScanPurgePersister;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionTemplate;

class ScanPurgeWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ScanWorkRepository scanWorkRepository;
	private AssetJpaRepository assetJpaRepository;
	private StoragePort storagePort;
	private ScanLockGuard lockGuard;
	private ScanUnlocker unlocker;
	private ScanPurgePersister persister;
	private ProblemRepositoryPort problemRepository;

	private TestableScanPurgeWorker sut;

	@BeforeEach
	void setUp() {
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Executor direct = Runnable::run;

		scanWorkRepository = mock(ScanWorkRepository.class);
		assetJpaRepository = mock(AssetJpaRepository.class);
		storagePort = mock(StoragePort.class);
		lockGuard = mock(ScanLockGuard.class);
		unlocker = mock(ScanUnlocker.class);
		persister = mock(ScanPurgePersister.class);
		problemRepository = mock(ProblemRepositoryPort.class);

		PurgeWorkerProperties props = new PurgeWorkerProperties(3600000L, 50, 60L, 1, 60, 3);

		sut = new TestableScanPurgeWorker(
			Clock.systemDefaultZone(),
			tx,
			direct,
			scanWorkRepository,
			assetJpaRepository,
			storagePort,
			problemRepository,
			props,
			lockGuard,
			unlocker,
			mock(BacklogLogger.class),
			persister);
	}

	@Test
	@DisplayName("purge: 락을 보유하면 S3 삭제→DB purge→unlock 순으로 수행한다")
	void purge_success_deletesAndUnlocks() {
		// given
		Long scanId = 7L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 2, 7, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);
		when(problemRepository.existsByScanId(scanId)).thenReturn(false);
		when(problemRepository.existsByOriginalStorageKey(anyString())).thenReturn(false);

		Asset a1 = mock(Asset.class);
		Asset a2 = mock(Asset.class);
		when(a1.getStorageKey()).thenReturn("s3/k1");
		when(a2.getStorageKey()).thenReturn("s3/k2");
		when(assetJpaRepository.findAllByScan_Id(scanId)).thenReturn(List.of(a1, a2));

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		InOrder inOrder = inOrder(lockGuard, assetJpaRepository, storagePort, persister, unlocker);
		inOrder.verify(lockGuard).isOwned(scanId, OWNER, TOKEN);
		inOrder.verify(assetJpaRepository).findAllByScan_Id(scanId);
		inOrder.verify(storagePort).deleteImage("s3/k1");
		inOrder.verify(storagePort).deleteImage("s3/k2");
		inOrder.verify(lockGuard).isOwned(scanId, OWNER, TOKEN);
		inOrder.verify(persister).purgeIfLocked(scanId, OWNER, TOKEN);
		inOrder.verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("purge: S3 삭제 이후 락을 잃으면 DB purge는 건너뛰고 unlock은 수행한다")
	void purge_lockLost_afterS3_skipsDbPurge_butUnlocks() {
		// given
		Long scanId = 8L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 2, 7, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, false);
		when(problemRepository.existsByScanId(scanId)).thenReturn(false);
		when(problemRepository.existsByOriginalStorageKey(anyString())).thenReturn(false);

		Asset a1 = mock(Asset.class);
		when(a1.getStorageKey()).thenReturn("s3/k1");
		when(assetJpaRepository.findAllByScan_Id(scanId)).thenReturn(List.of(a1));

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		verify(storagePort).deleteImage("s3/k1");
		verify(persister, never()).purgeIfLocked(anyLong(), anyString(), anyString());
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("purge: storageKey가 오답카드에 참조되면 S3 삭제를 건너뛴다")
	void purge_whenStorageKeyReferenced_thenSkipsS3Delete() {
		// given
		Long scanId = 9L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 2, 7, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);
		when(problemRepository.existsByScanId(scanId)).thenReturn(false);
		when(problemRepository.existsByOriginalStorageKey("s3/k1")).thenReturn(true);

		Asset a1 = mock(Asset.class);
		when(a1.getStorageKey()).thenReturn("s3/k1");
		when(assetJpaRepository.findAllByScan_Id(scanId)).thenReturn(List.of(a1));

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		verify(storagePort, never()).deleteImage(anyString());
		verify(persister).purgeIfLocked(scanId, OWNER, TOKEN);
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	static final class TestableScanPurgeWorker extends ScanPurgeWorker {
		TestableScanPurgeWorker(
			Clock clock,
			TransactionTemplate workerTxTemplate,
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
			super(
				clock,
				workerTxTemplate,
				purgeExecutor,
				scanWorkRepository,
				assetJpaRepository,
				storagePort,
				problemRepository,
				properties,
				lockGuard,
				unlocker,
				backlogLogger,
				persister);
		}

		void processOnePublic(Long scanId, String owner, String token, LocalDateTime now) {
			super.processOne(scanId, owner, token, now);
		}
	}
}
