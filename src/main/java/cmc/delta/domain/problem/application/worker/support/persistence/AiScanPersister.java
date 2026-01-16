package cmc.delta.domain.problem.application.worker.support.persistence;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.transaction.support.TransactionTemplate;

public class AiScanPersister {

	private static final long DEFAULT_RATE_LIMIT_DELAY_SECONDS = 180L;
	private static final long MIN_RATE_LIMIT_DELAY_SECONDS = 60L;

	private static final BigDecimal NEEDS_REVIEW_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.60);

	private final TransactionTemplate workerTransactionTemplate;
	private final ProblemScanJpaRepository problemScanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository problemTypeRepository;

	public AiScanPersister(
		TransactionTemplate workerTransactionTemplate,
		ProblemScanJpaRepository problemScanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository problemTypeRepository
	) {
		this.workerTransactionTemplate = workerTransactionTemplate;
		this.problemScanRepository = problemScanRepository;
		this.unitRepository = unitRepository;
		this.problemTypeRepository = problemTypeRepository;
	}

	public void persistAiSucceeded(
		Long scanId,
		String lockOwner,
		String lockToken,
		AiCurriculumResult aiResult,
		LocalDateTime completedAt
	) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = problemScanRepository.findById(scanId)
				.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));

			Unit predictedUnit = findUnitOrNull(aiResult.predictedUnitId());
			ProblemType predictedType = findProblemTypeOrNull(aiResult.predictedTypeId());

			BigDecimal confidence = BigDecimal.valueOf(aiResult.confidence());
			boolean needsReview = shouldNeedsReview(predictedUnit, predictedType, confidence);

			scan.markAiSucceeded(
				predictedUnit,
				predictedType,
				confidence,
				needsReview,
				aiResult.unitCandidatesJson(),
				aiResult.typeCandidatesJson(),
				aiResult.aiDraftJson(),
				completedAt
			);
		});
	}

	public void persistAiFailed(
		Long scanId,
		String lockOwner,
		String lockToken,
		FailureDecision decision,
		LocalDateTime now
	) {
		workerTransactionTemplate.executeWithoutResult(status -> {
			if (!isLockedByMe(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = problemScanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			String reason = decision.reasonCode().code();

			if (!decision.retryable()) {
				scan.markFailed(reason);
				return;
			}

			if (decision.reasonCode() == FailureReason.AI_RATE_LIMIT) {
				scan.markAiRateLimited(reason);

				Long delaySeconds = decision.retryAfterSeconds();
				long delay = delaySeconds == null ? 180L : delaySeconds.longValue();
				scan.scheduleNextRetryForAi(now, delay);
				return;
			}

			scan.markAiFailed(reason);
			scan.scheduleNextRetryForAi(now);
		});
	}



	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = problemScanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}

	private Unit findUnitOrNull(String unitId) {
		String normalizedUnitId = normalizeIdOrNull(unitId);
		if (normalizedUnitId == null) return null;
		return unitRepository.findById(normalizedUnitId).orElse(null);
	}

	private ProblemType findProblemTypeOrNull(String problemTypeId) {
		String normalizedProblemTypeId = normalizeIdOrNull(problemTypeId);
		if (normalizedProblemTypeId == null) return null;
		return problemTypeRepository.findById(normalizedProblemTypeId).orElse(null);
	}

	private String normalizeIdOrNull(String id) {
		if (id == null) return null;
		String trimmed = id.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private boolean shouldNeedsReview(Unit predictedUnit, ProblemType predictedType, BigDecimal confidence) {
		if (predictedUnit == null || predictedType == null) return true;
		return confidence.compareTo(NEEDS_REVIEW_CONFIDENCE_THRESHOLD) < 0;
	}

	private long computeRateLimitDelaySeconds(Long retryAfterSeconds) {
		if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
			return DEFAULT_RATE_LIMIT_DELAY_SECONDS;
		}
		return Math.max(retryAfterSeconds, MIN_RATE_LIMIT_DELAY_SECONDS);
	}
}
