package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.failure.AiFailureDecider;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.application.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.application.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.application.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.application.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.application.worker.support.prompt.AiCurriculumPromptBuilder;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;

import org.springframework.transaction.support.TransactionTemplate;

public record AiWorkerDoublesV2(
	ScanRepository scanRepo,
	AiClient aiClient,
	TransactionTemplate tx,
	AiWorkerProperties props,
	ScanLockGuard lockGuard,
	ScanUnlocker unlocker,
	BacklogLogger backlogLogger,
	WorkerLogPolicy logPolicy,
	AiFailureDecider failureDecider,
	AiScanValidator validator,
	AiCurriculumPromptBuilder promptBuilder,
	AiScanPersister persister
) {
	public static AiWorkerDoublesV2 create() {
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		AiWorkerProperties props = new AiWorkerProperties(2000L, 10, 30L, 1, 1);

		return new AiWorkerDoublesV2(
			mock(ScanRepository.class),
			mock(AiClient.class),
			tx,
			props,
			mock(ScanLockGuard.class),
			mock(ScanUnlocker.class),
			mock(BacklogLogger.class),
			mock(WorkerLogPolicy.class),
			mock(AiFailureDecider.class),
			mock(AiScanValidator.class),
			mock(AiCurriculumPromptBuilder.class),
			mock(AiScanPersister.class)
		);
	}
}
