package cmc.delta.domain.problem.adapter.in.worker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class ProblemKeyBackfillWorkerTest {

	@Test
	@DisplayName("problem key backfill: 원본 asset이 있으면 키를 채우고 scan을 detach한다")
	void runOnce_backfillsKeyAndDetachesScan() {
		// given
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Clock clock = Clock.fixed(Instant.parse("2026-02-07T00:00:00Z"), ZoneId.of("UTC"));
		ProblemJpaRepository problemRepository = mock(ProblemJpaRepository.class);
		AssetJpaRepository assetRepository = mock(AssetJpaRepository.class);
		StoragePort storagePort = mock(StoragePort.class);
		ProblemKeyBackfillWorker sut = new ProblemKeyBackfillWorker(tx, clock, problemRepository, assetRepository,
			storagePort);

		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(10L);
		User user = mock(User.class);
		when(user.getId()).thenReturn(8L);
		Unit unit = mock(Unit.class);
		ProblemType type = mock(ProblemType.class);
		when(type.getId()).thenReturn("T1");

		Problem p = Problem.create(user, scan, null, unit, type, RenderMode.LATEX, "md", AnswerFormat.TEXT, "a", 1,
			"s");
		when(problemRepository.findKeyBackfillCandidates(any())).thenReturn(List.of(p));

		Asset a = mock(Asset.class);
		when(a.getStorageKey()).thenReturn("s3/k1");
		when(assetRepository.findOriginalByScanId(10L)).thenReturn(Optional.of(a));
		when(storagePort.copyImage(eq("s3/k1"), anyString())).thenReturn("problem/k1");

		// when
		int processed = sut.runOnce(10);

		// then
		assertThat(processed).isEqualTo(1);
		assertThat(p.getOriginalStorageKey()).isEqualTo("problem/k1");
		assertThat(p.getScan()).isNull();
		verify(problemRepository).saveAndFlush(p);
	}

	@Test
	@DisplayName("problem key backfill: 원본 asset이 없으면 저장하지 않는다")
	void runOnce_whenAssetMissing_thenSkipsSave() {
		// given
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Clock clock = Clock.fixed(Instant.parse("2026-02-07T00:00:00Z"), ZoneId.of("UTC"));
		ProblemJpaRepository problemRepository = mock(ProblemJpaRepository.class);
		AssetJpaRepository assetRepository = mock(AssetJpaRepository.class);
		StoragePort storagePort = mock(StoragePort.class);
		ProblemKeyBackfillWorker sut = new ProblemKeyBackfillWorker(tx, clock, problemRepository, assetRepository,
			storagePort);

		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(10L);
		User user = mock(User.class);
		when(user.getId()).thenReturn(8L);
		Unit unit = mock(Unit.class);
		ProblemType type = mock(ProblemType.class);
		when(type.getId()).thenReturn("T1");

		Problem p = Problem.create(user, scan, null, unit, type, RenderMode.LATEX, "md", AnswerFormat.TEXT, "a", 1,
			"s");
		when(problemRepository.findKeyBackfillCandidates(any())).thenReturn(List.of(p));
		when(assetRepository.findOriginalByScanId(10L)).thenReturn(Optional.empty());

		// when
		int processed = sut.runOnce(10);

		// then
		assertThat(processed).isEqualTo(1);
		verify(problemRepository, never()).saveAndFlush(any());
		assertThat(p.getOriginalStorageKey()).isNull();
		assertThat(p.getScan()).isSameAs(scan);
	}
}
