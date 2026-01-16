package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.failure.AiFailureDecider;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.ScanFailReasonCode;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiScanWorker extends AbstractClaimingScanWorker {

	private static final long DEFAULT_RATE_LIMIT_DELAY_SECONDS = 180L;
	private static final long MIN_RATE_LIMIT_DELAY_SECONDS = 60L;
	private static final int OCR_TEXT_MAX_CHARS = 3000;

	private final ProblemScanJpaRepository scanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;
	private final AiClient aiClient;
	private final TransactionTemplate template;
	private LocalDateTime lastBacklogLoggedAt;
	private final AiWorkerProperties props;

	private final ScanLockGuard lockGuard;
	private final AiFailureDecider failureDecider;

	public AiScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("aiExecutor") Executor aiExecutor,
		ProblemScanJpaRepository scanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository typeRepository,
		AiClient aiClient,
		AiWorkerProperties props
	) {
		super(clock, workerTxTemplate, aiExecutor);
		this.template = workerTxTemplate;
		this.scanRepository = scanRepository;
		this.unitRepository = unitRepository;
		this.typeRepository = typeRepository;
		this.aiClient = aiClient;
		this.props = props;

		this.lockGuard = new ScanLockGuard(scanRepository);
		this.failureDecider = new AiFailureDecider();
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken, LocalDateTime lockedAt, int limit) {
		return scanRepository.claimAiCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanRepository.findClaimedAiIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("AI 워커 tick - 처리 대상 없음");

		if (!shouldLogBacklog(now)) return;

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		long backlog = scanRepository.countAiBacklog(now, staleBefore);

		log.info("AI 워커 - 처리 대상 없음 (aiBacklog={})", backlog);
		lastBacklogLoggedAt = now;
	}

	private boolean shouldLogBacklog(LocalDateTime now) {
		if (lastBacklogLoggedAt == null) return true;

		Duration interval = Duration.ofMinutes(props.backlogLogMinutes());
		return !now.isBefore(lastBacklogLoggedAt.plus(interval));
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

			String ocrText = normalizeOcrText(scan.getOcrPlainText());
			if (ocrText.isBlank()) throw new IllegalStateException("OCR_TEXT_EMPTY");

			Long userId = scan.getUser().getId();

			AiCurriculumPrompt prompt = buildPrompt(userId, ocrText);
			AiCurriculumResult ai = aiClient.classifyCurriculum(prompt);

			// 외부 호출 후 저장 직전 락 재확인
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			Unit predictedUnit = findUnitOrNull(ai.predictedUnitId());
			ProblemType predictedType = findTypeOrNull(ai.predictedTypeId());

			BigDecimal confidence = BigDecimal.valueOf(ai.confidence());
			boolean needsReview = shouldNeedsReview(predictedUnit, predictedType, confidence);

			saveAiSuccess(
				scanId, lockOwner, lockToken,
				predictedUnit, predictedType, confidence, needsReview,
				ai.unitCandidatesJson(), ai.typeCandidatesJson(), ai.aiDraftJson()
			);

			log.info("AI 분류 완료 scanId={} unitId={} typeId={} confidence={} needsReview={}",
				scanId,
				predictedUnit == null ? null : predictedUnit.getId(),
				predictedType == null ? null : predictedType.getId(),
				confidence,
				needsReview
			);

		} catch (Exception e) {
			handleFailure(scanId, lockOwner, lockToken, e);
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private void handleFailure(Long scanId, String lockOwner, String lockToken, Exception e) {
		FailureDecision decision = failureDecider.decide(e);

		if (decision.reasonCode() == ScanFailReasonCode.OCR_TEXT_EMPTY) {
			saveAiFailure(scanId, lockOwner, lockToken, decision);
			log.warn("AI 처리 불가 scanId={} reason={}", scanId, decision.reasonCode().name());
			return;
		}

		saveAiFailure(scanId, lockOwner, lockToken, decision);

		if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
			log.warn("AI 호출 4xx scanId={} reason={} status={}", scanId, decision.reasonCode().name(), rre.getRawStatusCode());
		} else {
			log.error("AI 처리 실패 scanId={} reason={}", scanId, decision.reasonCode().name(), e);
		}
	}

	private ProblemScan loadScanOrThrow(Long scanId) {
		return scanRepository.findById(scanId)
			.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));
	}

	private String normalizeOcrText(String ocrText) {
		if (ocrText == null) return "";
		String t = ocrText.replaceAll("\\s+", " ").trim();
		if (t.length() > OCR_TEXT_MAX_CHARS) {
			t = t.substring(0, OCR_TEXT_MAX_CHARS);
		}
		return t;
	}

	private AiCurriculumPrompt buildPrompt(Long userId, String ocrText) {
		List<AiCurriculumPrompt.Option> subjectOptions = unitRepository.findAllRootUnitsActive()
			.stream()
			.map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> unitOptions = unitRepository.findAllChildUnitsActive()
			.stream()
			.map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> typeOptions = typeRepository.findAllActiveForUser(userId)
			.stream()
			.map(t -> new AiCurriculumPrompt.Option(t.getId(), t.getName()))
			.toList();

		return new AiCurriculumPrompt(ocrText, subjectOptions, unitOptions, typeOptions);
	}

	private Unit findUnitOrNull(String id) {
		String unitId = normalizeIdOrNull(id);
		return unitId == null ? null : unitRepository.findById(unitId).orElse(null);
	}

	private ProblemType findTypeOrNull(String id) {
		String typeId = normalizeIdOrNull(id);
		return typeId == null ? null : typeRepository.findById(typeId).orElse(null);
	}

	private String normalizeIdOrNull(String id) {
		if (id == null) return null;
		String t = id.trim();
		return t.isEmpty() ? null : t;
	}

	private void saveAiSuccess(
		Long scanId, String lockOwner, String lockToken,
		Unit predictedUnit, ProblemType predictedType, BigDecimal confidence, boolean needsReview,
		String unitCandidatesJson, String typeCandidatesJson, String aiDraftJson
	) {
		template.executeWithoutResult(status -> {
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = loadScanOrThrow(scanId);

			scan.markAiSucceeded(
				predictedUnit, predictedType, confidence, needsReview,
				unitCandidatesJson, typeCandidatesJson, aiDraftJson,
				LocalDateTime.now()
			);
		});
	}

	private void saveAiFailure(Long scanId, String lockOwner, String lockToken, FailureDecision decision) {
		template.executeWithoutResult(status -> {
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			String reason = decision.reasonCode().name();

			if (!decision.retryable()) {
				scan.markFailed(reason);
				return;
			}

			if (decision.reasonCode() == ScanFailReasonCode.AI_RATE_LIMIT) {
				scan.markAiRateLimited(reason);

				long delay = computeRateLimitDelaySeconds(decision.retryAfterSeconds());
				scan.scheduleNextRetryForAi(LocalDateTime.now(), delay);
				return;
			}

			scan.markAiFailed(reason);
			scan.scheduleNextRetryForAi(LocalDateTime.now());
		});
	}

	private long computeRateLimitDelaySeconds(Long retryAfterSec) {
		if (retryAfterSec == null || retryAfterSec <= 0) return DEFAULT_RATE_LIMIT_DELAY_SECONDS;
		return Math.max(retryAfterSec, MIN_RATE_LIMIT_DELAY_SECONDS);
	}

	private void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			template.executeWithoutResult(status -> scanRepository.unlock(scanId, lockOwner, lockToken));
		} catch (Exception unlockEx) {
			log.error("AI unlock 실패 scanId={}", scanId, unlockEx);
		}
	}

	private boolean shouldNeedsReview(Unit unit, ProblemType type, BigDecimal confidence) {
		if (unit == null || type == null) return true;
		return confidence.compareTo(BigDecimal.valueOf(0.60)) < 0;
	}
}
