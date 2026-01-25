package cmc.delta.domain.problem.adapter.in.worker;

import static cmc.delta.domain.problem.adapter.in.worker.support.WorkerFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.AiFailureDecider;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.adapter.in.worker.support.prompt.AiCurriculumPromptBuilder;
import cmc.delta.domain.problem.adapter.in.worker.support.validation.AiScanValidator;
import cmc.delta.domain.problem.adapter.in.worker.support.validation.AiScanValidator.AiValidatedInput;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.application.port.in.worker.AiScanPersistUseCase;
import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionTemplate;

class AiScanWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ScanRepository scanRepository;
	private AiScanValidator validator;
	private AiCurriculumPromptBuilder promptBuilder;
	private AiClient aiClient;
	private AiScanPersistUseCase persister;

	private ScanLockGuard lockGuard;
	private ScanUnlocker unlocker;

	private AiFailureDecider failureDecider;

	private TestableAiScanWorker sut;

	@BeforeEach
	void setUp() {
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Executor direct = Runnable::run;

		scanRepository = mock(ScanRepository.class);
		validator = mock(AiScanValidator.class);
		promptBuilder = mock(AiCurriculumPromptBuilder.class);
		aiClient = mock(AiClient.class);
		persister = mock(AiScanPersistUseCase.class);

		lockGuard = mock(ScanLockGuard.class);
		unlocker = mock(ScanUnlocker.class);

		failureDecider = mock(AiFailureDecider.class);

		sut = new TestableAiScanWorker(
			Clock.systemDefaultZone(),
			tx,
			direct,
			scanRepository,
			mock(cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository.class),
			aiClient,
			new AiWorkerProperties(2000L, 10, 30L, 1, 1),
			lockGuard,
			unlocker,
			mock(BacklogLogger.class),
			mock(WorkerLogPolicy.class),
			failureDecider,
			validator,
			promptBuilder,
			persister);
	}

	@Test
	@DisplayName("성공: scan 조회→validate→prompt build→aiClient→persistAiSucceeded→unlock 순으로 수행된다")
	void success_persistsAndUnlocks() {
		// given
		Long scanId = 1L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);

		ProblemScan scan = ocrDone(user(10L), "some text");
		when(scanRepository.findById(scanId)).thenReturn(Optional.of(scan));

		AiValidatedInput input = new AiValidatedInput(10L, "some text");
		when(validator.validateAndNormalize(scanId, scan)).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(promptBuilder.build(10L, "some text")).thenReturn(prompt);

		AiCurriculumResult ai = aiResult("U1", "T1", 0.90);
		when(aiClient.classifyCurriculum(prompt)).thenReturn(ai);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		InOrder inOrder = inOrder(scanRepository, validator, promptBuilder, aiClient, persister, unlocker);

		inOrder.verify(scanRepository).findById(scanId);
		inOrder.verify(validator).validateAndNormalize(scanId, scan);
		inOrder.verify(promptBuilder).build(10L, "some text");
		inOrder.verify(aiClient).classifyCurriculum(prompt);
		inOrder.verify(persister).persistAiSucceeded(scanId, OWNER, TOKEN, ai, batchNow);
		inOrder.verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);

		verify(persister, never()).persistAiFailed(anyLong(), anyString(), anyString(), any(FailureDecision.class),
			any());
	}

	@Test
	@DisplayName("외부 호출 후 락을 잃으면 저장은 하지 않지만 unlock은 수행한다")
	void lockLost_afterExternalCall_skipsPersist_butUnlocks() {
		// given
		Long scanId = 1L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, false);

		ProblemScan scan = ocrDone(user(10L), "some text");
		when(scanRepository.findById(scanId)).thenReturn(Optional.of(scan));

		when(validator.validateAndNormalize(scanId, scan))
			.thenReturn(new AiValidatedInput(10L, "some text"));

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(promptBuilder.build(10L, "some text")).thenReturn(prompt);

		AiCurriculumResult ai = aiResult("U1", "T1", 0.90);
		when(aiClient.classifyCurriculum(prompt)).thenReturn(ai);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		verify(persister, never()).persistAiSucceeded(anyLong(), anyString(), anyString(), any(), any());
		verify(persister, never()).persistAiFailed(anyLong(), anyString(), anyString(), any(), any());
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("scan이 없으면 실패로 저장(persistAiFailed)하고 unlock을 보장한다")
	void scanMissing_persistsFailed_and_unlocks() {
		// given
		Long scanId = 99L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true);
		when(scanRepository.findById(scanId)).thenReturn(Optional.empty());

		FailureDecision decision = mock(FailureDecision.class);
		when(failureDecider.decide(any(Exception.class))).thenReturn(decision);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		verify(persister).persistAiFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), eq(batchNow));
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
		verifyNoInteractions(validator, promptBuilder, aiClient);
	}

	/**
	 * 테스트에서만 protected processOne을 호출하기 위한 래퍼.
	 */
	static final class TestableAiScanWorker extends AiScanWorker {

		TestableAiScanWorker(
			Clock clock,
			TransactionTemplate workerTxTemplate,
			Executor aiExecutor,
			ScanRepository scanRepository,
			cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository scanWorkRepository,
			AiClient aiClient,
			AiWorkerProperties properties,
			ScanLockGuard lockGuard,
			ScanUnlocker unlocker,
			BacklogLogger backlogLogger,
			WorkerLogPolicy logPolicy,
			AiFailureDecider failureDecider,
			AiScanValidator validator,
			AiCurriculumPromptBuilder promptBuilder,
			AiScanPersistUseCase persister) {
			super(
				clock,
				workerTxTemplate,
				aiExecutor,
				scanRepository,
				scanWorkRepository,
				aiClient,
				properties,
				lockGuard,
				unlocker,
				backlogLogger,
				logPolicy,
				failureDecider,
				validator,
				promptBuilder,
				persister);
		}

		void processOnePublic(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
			super.processOne(scanId, lockOwner, lockToken, batchNow);
		}
	}
}
