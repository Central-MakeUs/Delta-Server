package cmc.delta.domain.problem.adapter.in.worker.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

class AbstractExternalCallScanWorkerContractTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ScanLockGuard lockGuard;
	private ScanUnlocker unlocker;
	private WorkerLogPolicy logPolicy;

	private FailureDecision decision;

	private TestWorker sut;

	@BeforeEach
	void setUp() {
		lockGuard = mock(ScanLockGuard.class);
		unlocker = mock(ScanUnlocker.class);
		logPolicy = mock(WorkerLogPolicy.class);

		decision = mock(FailureDecision.class);

		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Executor direct = Runnable::run;

		sut = new TestWorker(
			Clock.systemDefaultZone(),
			tx,
			direct,
			new WorkerIdentity("t", "TEST", "worker:test:backlog"),
			lockGuard,
			unlocker,
			mock(BacklogLogger.class),
			logPolicy);
		sut.setDecision(decision);
	}

	@Test
	@DisplayName("processOne: 시작 시 락이 없으면 즉시 종료하고 unlock도 호출하지 않는다")
	void lockLost_returnsImmediately() {
		// given
		Long scanId = 1L;
		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(false);

		// when
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now());

		// then
		verifyNoInteractions(unlocker);
		assertThat(sut.handleCalled).isFalse();
		assertThat(sut.persistFailedCalled).isFalse();
	}

	@Test
	@DisplayName("processOne: RestClientResponseException(4xx)면 suppress 분기를 타고 persistFailed + unlock을 보장한다")
	void rest4xx_suppressed_persistFailed_and_unlock() {
		// given
		Long scanId = 2L;
		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true);

		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null);

		when(logPolicy.shouldSuppressStacktrace(ex)).thenReturn(true);
		sut.setToThrow(ex);

		// when
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now());

		// then
		verify(logPolicy).shouldSuppressStacktrace(ex);
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
		assertThat(sut.persistFailedCalled).isTrue();
	}

	@Test
	@DisplayName("processOne: 일반 예외면 persistFailed + unlock을 보장한다")
	void runtimeException_persistFailed_and_unlock() {
		// given
		Long scanId = 3L;
		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true);

		RuntimeException ex = new IllegalStateException("boom");
		sut.setToThrow(ex);

		when(logPolicy.reasonCode(any())).thenReturn("TEST");

		sut.processOnePublic(scanId, OWNER, TOKEN, LocalDateTime.now());

		// then
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
		assertThat(sut.persistFailedCalled).isTrue();

		verify(logPolicy, never()).shouldSuppressStacktrace(any());

		verify(logPolicy).reasonCode(any());

		// 필요하면 더 깐깐하게
		verifyNoMoreInteractions(logPolicy);
	}

	private static final class TestWorker extends AbstractExternalCallScanWorker {

		private Exception toThrow;
		private FailureDecision decision;

		private boolean handleCalled = false;
		private boolean persistFailedCalled = false;

		TestWorker(
			Clock clock,
			TransactionTemplate tx,
			Executor executor,
			WorkerIdentity identity,
			ScanLockGuard lockGuard,
			ScanUnlocker unlocker,
			BacklogLogger backlogLogger,
			WorkerLogPolicy logPolicy) {
			super(clock, tx, executor, identity, lockGuard, unlocker, backlogLogger, logPolicy);
		}

		void setToThrow(Exception e) {
			this.toThrow = e;
		}

		void setDecision(FailureDecision d) {
			this.decision = d;
		}

		@Override
		protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken,
			LocalDateTime lockedAt, int limit) {
			return 0;
		}

		@Override
		protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
			return List.of();
		}

		@Override
		protected long backlogLogMinutes() {
			return 1;
		}

		@Override
		protected long countBacklog(LocalDateTime now, LocalDateTime staleBefore) {
			return 0;
		}

		@Override
		protected void handleSuccess(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
			handleCalled = true;
			if (toThrow != null) {
				if (toThrow instanceof RuntimeException re)
					throw re;
				throw new RuntimeException(toThrow);
			}
		}

		@Override
		protected FailureDecision decideFailure(Exception exception) {
			return decision;
		}

		@Override
		protected void persistFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
			LocalDateTime batchNow) {
			persistFailedCalled = true;
		}

		void processOnePublic(Long scanId, String owner, String token, LocalDateTime now) {
			super.processOne(scanId, owner, token, now);
		}
	}
}
