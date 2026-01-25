package cmc.delta.domain.problem.adapter.in.worker;

import cmc.delta.domain.problem.adapter.in.worker.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.AbstractExternalCallScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerIdentity;
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
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.in.worker.AiScanPersistUseCase;
import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class AiScanWorker extends AbstractExternalCallScanWorker {

	private static final WorkerIdentity IDENTITY = new WorkerIdentity("ai", "AI", "worker:ai:backlog");

	private final ScanRepository scanRepository;
	private final ScanWorkRepository scanWorkRepository;

	private final AiClient aiClient;
	private final AiWorkerProperties properties;

	private final AiFailureDecider failureDecider;
	private final AiScanValidator validator;
	private final AiCurriculumPromptBuilder promptBuilder;

	private final AiScanPersistUseCase persistUseCase;

	public AiScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("aiExecutor")
		Executor aiExecutor,
		ScanRepository scanRepository,
		ScanWorkRepository scanWorkRepository,
		AiClient aiClient,
		AiWorkerProperties properties,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy,
		AiFailureDecider failureDecider,
		AiScanValidator validator,
		AiCurriculumPromptBuilder promptBuilder,
		AiScanPersistUseCase persistUseCase) {
		super(clock, workerTxTemplate, aiExecutor, IDENTITY, lockGuard, unlocker, backlogLogger, logPolicy);
		this.scanRepository = scanRepository;
		this.scanWorkRepository = scanWorkRepository;
		this.aiClient = aiClient;
		this.properties = properties;
		this.failureDecider = failureDecider;
		this.validator = validator;
		this.promptBuilder = promptBuilder;
		this.persistUseCase = persistUseCase;
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken,
		LocalDateTime lockedAt, int limit) {
		return scanWorkRepository.claimAiCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanWorkRepository.findClaimedAiIds(lockOwner, lockToken, limit);
	}

	@Override
	protected long backlogLogMinutes() {
		return properties.backlogLogMinutes();
	}

	@Override
	protected long countBacklog(LocalDateTime now, LocalDateTime staleBefore) {
		return scanWorkRepository.countAiBacklog(now, staleBefore);
	}

	@Override
	protected void handleSuccess(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		ProblemScan scan = loadScanOrThrow(scanId);
		AiValidatedInput input = validator.validateAndNormalize(scanId, scan);

		AiCurriculumPrompt prompt = promptBuilder.build(input.userId(), input.normalizedOcrText());
		AiCurriculumResult aiResult = aiClient.classifyCurriculum(prompt);

		if (!isOwned(scanId, lockOwner, lockToken))
			return;

		persistUseCase.persistAiSucceeded(scanId, lockOwner, lockToken, aiResult, batchNow);

		log.info("AI 분류 완료 scanId={} 상태=AI_DONE", scanId);
	}

	@Override
	protected FailureDecision decideFailure(Exception exception) {
		return failureDecider.decide(exception);
	}

	@Override
	protected void persistFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow) {
		persistUseCase.persistAiFailed(scanId, lockOwner, lockToken, decision, batchNow);
	}

	private ProblemScan loadScanOrThrow(Long scanId) {
		Optional<ProblemScan> optional = scanRepository.findById(scanId);
		if (optional.isEmpty())
			throw new ProblemScanNotFoundException(scanId);
		return optional.get();
	}
}
