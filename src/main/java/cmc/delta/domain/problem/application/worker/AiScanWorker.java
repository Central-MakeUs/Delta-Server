package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.support.failure.AiFailureDecider;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.application.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.application.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.application.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.application.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.application.worker.support.prompt.AiCurriculumPromptBuilder;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator.AiValidatedInput;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiScanWorker extends AbstractClaimingScanWorker {

	private static final String WORKER_KEY = "worker:ai:backlog";

	private final ProblemScanJpaRepository problemScanRepository;
	private final AiClient aiClient;
	private final AiWorkerProperties properties;

	private final ScanLockGuard lockGuard;
	private final ScanUnlocker unlocker;
	private final BacklogLogger backlogLogger;
	private final WorkerLogPolicy logPolicy;

	private final AiFailureDecider failureDecider;
	private final AiScanValidator validator;
	private final AiCurriculumPromptBuilder promptBuilder;
	private final AiScanPersister persister;

	public AiScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("aiExecutor") Executor aiExecutor,
		ProblemScanJpaRepository problemScanRepository,
		AiClient aiClient,
		AiWorkerProperties properties,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy,
		AiFailureDecider failureDecider,
		AiScanValidator validator,
		AiCurriculumPromptBuilder promptBuilder,
		AiScanPersister persister
	) {
		super(clock, workerTxTemplate, aiExecutor,"ai");
		this.problemScanRepository = problemScanRepository;
		this.aiClient = aiClient;
		this.properties = properties;

		this.lockGuard = lockGuard;
		this.unlocker = unlocker;
		this.backlogLogger = backlogLogger;
		this.logPolicy = logPolicy;

		this.failureDecider = failureDecider;
		this.validator = validator;
		this.promptBuilder = promptBuilder;
		this.persister = persister;
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken, LocalDateTime lockedAt, int limit) {
		return problemScanRepository.claimAiCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return problemScanRepository.findClaimedAiIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("AI 워커 tick - 처리 대상 없음");

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		backlogLogger.logIfDue(
			WORKER_KEY,
			now,
			properties.backlogLogMinutes(),
			() -> problemScanRepository.countAiBacklog(now, staleBefore),
			(backlog) -> log.info("AI 워커 - 처리 대상 없음 (aiBacklog={})", backlog)
		);
	}

	@Override
	protected void onClaimed(LocalDateTime now, int count) {
		log.info("AI 워커 tick - 이번 배치 처리 대상={}건", count);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

		try {
			ProblemScan scan = loadScanOrThrow(scanId);
			AiValidatedInput input = validator.validateAndNormalize(scanId, scan);

			AiCurriculumPrompt prompt = promptBuilder.build(input.userId(), input.normalizedOcrText());
			AiCurriculumResult aiResult = aiClient.classifyCurriculum(prompt);

			// 외부 호출 후 저장 직전 락 재확인
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			persister.persistAiSucceeded(scanId, lockOwner, lockToken, aiResult, LocalDateTime.now());
			log.info("AI 분류 완료 scanId={} 상태=AI_DONE", scanId);

		} catch (Exception exception) {
			FailureDecision decision = failureDecider.decide(exception);
			persister.persistAiFailed(scanId, lockOwner, lockToken, decision, LocalDateTime.now());

			if (exception instanceof RestClientResponseException restClientResponseException
				&& logPolicy.shouldSuppressStacktrace(restClientResponseException)) {
				log.warn("AI 호출 4xx scanId={} reason={} status={}",
					scanId, logPolicy.reasonCode(decision), restClientResponseException.getRawStatusCode()
				);
			} else {
				log.error("AI 처리 실패 scanId={} reason={}", scanId, logPolicy.reasonCode(decision), exception);
			}
		} finally {
			try {
				unlocker.unlockBestEffort(scanId, lockOwner, lockToken);
			} catch (Exception unlockException) {
				log.error("AI unlock 실패 scanId={}", scanId, unlockException);
			}
		}
	}

	private ProblemScan loadScanOrThrow(Long scanId) {
		Optional<ProblemScan> optionalScan = problemScanRepository.findById(scanId);
		if (optionalScan.isEmpty()) {
			throw new ProblemScanNotFoundException(scanId);
		}
		return optionalScan.get();
	}
}
