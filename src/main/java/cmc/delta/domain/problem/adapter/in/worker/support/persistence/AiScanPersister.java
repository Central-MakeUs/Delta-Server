package cmc.delta.domain.problem.adapter.in.worker.support.persistence;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class AiScanPersister {

	private static final BigDecimal NEEDS_REVIEW_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.60);
	private static final long DEFAULT_RETRY_AFTER_SECONDS = 180L;

	private final TransactionTemplate workerTx;
	private final ScanWorkRepository scanWorkRepository;
	private final ScanRepository scanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository problemTypeRepository;

	public AiScanPersister(
		TransactionTemplate workerTx,
		ScanWorkRepository scanWorkRepository,
		ScanRepository scanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository problemTypeRepository) {
		this.workerTx = workerTx;
		this.scanWorkRepository = scanWorkRepository;
		this.scanRepository = scanRepository;
		this.unitRepository = unitRepository;
		this.problemTypeRepository = problemTypeRepository;
	}

	public void persistAiSucceeded(
		Long scanId,
		String lockOwner,
		String lockToken,
		AiCurriculumResult aiResult,
		LocalDateTime completedAt) {
		workerTx.executeWithoutResult(tx -> inLockedTx(scanId, lockOwner, lockToken, scan -> {
			AiPersistContext context = buildPersistContext(aiResult);
			scan.markAiSucceeded(
				context.predictedUnit(),
				context.predictedType(),
				context.confidence(),
				context.needsReview(),
				aiResult.unitCandidatesJson(),
				aiResult.typeCandidatesJson(),
				aiResult.aiDraftJson(),
				completedAt);
		}));
	}

	public void persistAiFailed(
		Long scanId,
		String lockOwner,
		String lockToken,
		FailureDecision decision,
		LocalDateTime now) {
		workerTx.executeWithoutResult(tx -> inLockedTx(scanId, lockOwner, lockToken, scan -> {
			String reason = decision.reasonCode().code();

			if (!decision.retryable()) {
				scan.markFailed(reason);
				return;
			}

			if (decision.reasonCode() == FailureReason.AI_RATE_LIMIT) {
				scan.markAiRateLimited(reason);
				scan.scheduleNextRetryForAi(now, resolveRetryAfterSeconds(decision));
				return;
			}

			scan.markAiFailed(reason);
			scan.scheduleNextRetryForAi(now);
		}));
	}

	private void inLockedTx(
		Long scanId,
		String lockOwner,
		String lockToken,
		Consumer<ProblemScan> action) {
		if (!isLockedByMe(scanId, lockOwner, lockToken)) {
			return;
		}

		ProblemScan scan = scanRepository.findById(scanId).orElse(null);
		if (scan == null) {
			return;
		}

		action.accept(scan);
	}

	private boolean isLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanWorkRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}

	private long resolveRetryAfterSeconds(FailureDecision decision) {
		Long retryAfter = decision.retryAfterSeconds();
		return retryAfter == null ? DEFAULT_RETRY_AFTER_SECONDS : retryAfter.longValue();
	}

	private AiPersistContext buildPersistContext(AiCurriculumResult aiResult) {
		Unit predictedUnit = findUnitOrNull(aiResult.predictedUnitId());
		ProblemType predictedType = findProblemTypeOrNull(aiResult.predictedTypeId());
		BigDecimal confidence = BigDecimal.valueOf(aiResult.confidence());
		boolean needsReview = shouldNeedsReview(predictedUnit, predictedType, confidence);
		return new AiPersistContext(predictedUnit, predictedType, confidence, needsReview);
	}

	private Unit findUnitOrNull(String unitId) {
		String normalized = normalizeIdOrNull(unitId);
		if (normalized == null) {
			return null;
		}
		return unitRepository.findById(normalized).orElse(null);
	}

	private ProblemType findProblemTypeOrNull(String problemTypeId) {
		String normalized = normalizeIdOrNull(problemTypeId);
		if (normalized == null) {
			return null;
		}
		return problemTypeRepository.findById(normalized).orElse(null);
	}

	private String normalizeIdOrNull(String id) {
		if (id == null) {
			return null;
		}
		String trimmed = id.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private boolean shouldNeedsReview(Unit predictedUnit, ProblemType predictedType, BigDecimal confidence) {
		if (predictedUnit == null || predictedType == null) {
			return true;
		}
		return confidence.compareTo(NEEDS_REVIEW_CONFIDENCE_THRESHOLD) < 0;
	}

	private record AiPersistContext(
		Unit predictedUnit,
		ProblemType predictedType,
		BigDecimal confidence,
		boolean needsReview) {
	}
}
