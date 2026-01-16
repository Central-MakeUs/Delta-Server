package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.support.failure.AiFailureDecider;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.application.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator.AiValidatedInput;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiScanWorker extends AbstractClaimingScanWorker {

	private final ProblemScanJpaRepository problemScanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository problemTypeRepository;
	private final AiClient aiClient;
	private final AiWorkerProperties properties;

	private LocalDateTime lastBacklogLoggedAt;

	private final ScanLockGuard lockGuard;
	private final AiFailureDecider failureDecider;
	private final AiScanPersister aiScanPersister;
	private final AiScanValidator aiScanValidator;

	private final org.springframework.transaction.support.TransactionTemplate workerTransactionTemplate;

	public AiScanWorker(
		Clock clock,
		org.springframework.transaction.support.TransactionTemplate workerTxTemplate,
		@Qualifier("aiExecutor") Executor aiExecutor,
		ProblemScanJpaRepository problemScanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository problemTypeRepository,
		AiClient aiClient,
		AiWorkerProperties properties
	) {
		super(clock, workerTxTemplate, aiExecutor);
		this.workerTransactionTemplate = workerTxTemplate;
		this.problemScanRepository = problemScanRepository;
		this.unitRepository = unitRepository;
		this.problemTypeRepository = problemTypeRepository;
		this.aiClient = aiClient;
		this.properties = properties;

		this.lockGuard = new ScanLockGuard(problemScanRepository);
		this.failureDecider = new AiFailureDecider();
		this.aiScanPersister = new AiScanPersister(
			workerTxTemplate,
			problemScanRepository,
			unitRepository,
			problemTypeRepository
		);
		this.aiScanValidator = new AiScanValidator();
	}

	@Override
	protected int claim(
		LocalDateTime now,
		LocalDateTime staleBefore,
		String lockOwner,
		String lockToken,
		LocalDateTime lockedAt,
		int limit
	) {
		return problemScanRepository.claimAiCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return problemScanRepository.findClaimedAiIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("AI 워커 tick - 처리 대상 없음");

		if (!shouldLogBacklog(now)) return;

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		long backlog = problemScanRepository.countAiBacklog(now, staleBefore);

		log.info("AI 워커 - 처리 대상 없음 (aiBacklog={})", backlog);
		lastBacklogLoggedAt = now;
	}

	private boolean shouldLogBacklog(LocalDateTime now) {
		if (lastBacklogLoggedAt == null) return true;

		Duration interval = Duration.ofMinutes(properties.backlogLogMinutes());
		return !now.isBefore(lastBacklogLoggedAt.plus(interval));
	}

	@Override
	protected void onClaimed(LocalDateTime now, int count) {
		log.info("AI 워커 tick - 이번 배치 처리 대상={}건", count);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			ProblemScan scan = loadScanOrThrow(scanId);
			AiValidatedInput validatedInput = aiScanValidator.validateAndNormalize(scanId, scan);

			AiCurriculumPrompt prompt = buildPrompt(validatedInput.userId(), validatedInput.normalizedOcrText());
			AiCurriculumResult aiResult = aiClient.classifyCurriculum(prompt);

			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) {
				return;
			}

			LocalDateTime completedAt = LocalDateTime.now();
			aiScanPersister.persistAiSucceeded(scanId, lockOwner, lockToken, aiResult, completedAt);

			log.info("AI 분류 완료 scanId={}", scanId);

		} catch (Exception exception) {
			FailureDecision decision = failureDecider.decide(exception);

			LocalDateTime now = LocalDateTime.now();
			aiScanPersister.persistAiFailed(scanId, lockOwner, lockToken, decision, now);

			if (exception instanceof RestClientResponseException restClientResponseException
				&& restClientResponseException.getRawStatusCode() >= 400
				&& restClientResponseException.getRawStatusCode() < 500
			) {
				log.warn("AI 호출 4xx scanId={} reason={} status={}",
					scanId,
					decision.reasonCode().code(),
					restClientResponseException.getRawStatusCode()
				);
			} else {
				log.error("AI 처리 실패 scanId={} reason={}", scanId, decision.reasonCode().code(), exception);
			}
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			workerTransactionTemplate.executeWithoutResult(status ->
				problemScanRepository.unlock(scanId, lockOwner, lockToken)
			);
		} catch (Exception unlockException) {
			log.error("AI unlock 실패 scanId={}", scanId, unlockException);
		}
	}

	private ProblemScan loadScanOrThrow(Long scanId) {
		Optional<ProblemScan> optionalScan = problemScanRepository.findById(scanId);
		if (optionalScan.isEmpty()) {
			throw new cmc.delta.domain.problem.application.worker.exception.ProblemScanNotFoundException(scanId);
		}
		return optionalScan.get();
	}

	private AiCurriculumPrompt buildPrompt(Long userId, String normalizedOcrText) {
		List<AiCurriculumPrompt.Option> subjectOptions = unitRepository.findAllRootUnitsActive()
			.stream()
			.map(unit -> new AiCurriculumPrompt.Option(unit.getId(), unit.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> unitOptions = unitRepository.findAllChildUnitsActive()
			.stream()
			.map(unit -> new AiCurriculumPrompt.Option(unit.getId(), unit.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> typeOptions = problemTypeRepository.findAllActiveForUser(userId)
			.stream()
			.map(problemType -> new AiCurriculumPrompt.Option(problemType.getId(), problemType.getName()))
			.toList();

		return new AiCurriculumPrompt(normalizedOcrText, subjectOptions, unitOptions, typeOptions);
	}
}
