package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.port.ai.AiClient;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiScanWorker extends AbstractClaimingScanWorker {

	private final ProblemScanJpaRepository scanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;
	private final AiClient aiClient;
	private final TransactionTemplate tx;

	public AiScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("aiExecutor") Executor aiExecutor,
		ProblemScanJpaRepository scanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository typeRepository,
		AiClient aiClient
	) {
		super(clock, workerTxTemplate, aiExecutor);
		this.tx = workerTxTemplate;
		this.scanRepository = scanRepository;
		this.unitRepository = unitRepository;
		this.typeRepository = typeRepository;
		this.aiClient = aiClient;
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
	}

	@Override
	protected void onClaimed(LocalDateTime now, int count) {
		log.info("AI 워커 tick - 이번 배치 처리 대상={}건", count);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!isStillLockedByMe(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			ProblemScan scan = scanRepository.findById(scanId)
				.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));

			String ocrText = scan.getOcrPlainText();
			if (ocrText == null || ocrText.isBlank()) {
				throw new IllegalStateException("OCR_TEXT_EMPTY");
			}

			Long userId = scan.getUser().getId();

			List<AiCurriculumPrompt.Option> subjectOptions = unitRepository.findAllRootUnitsActive()
				.stream().map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName())).toList();

			List<AiCurriculumPrompt.Option> unitOptions = unitRepository.findAllByActiveTrueOrderBySortOrderAsc()
				.stream().map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName())).toList();

			List<AiCurriculumPrompt.Option> typeOptions = typeRepository.findAllActiveForUser(userId)
				.stream().map(t -> new AiCurriculumPrompt.Option(t.getId(), t.getName())).toList();

			AiCurriculumResult ai = aiClient.classifyCurriculum(
				new AiCurriculumPrompt(ocrText, subjectOptions, unitOptions, typeOptions)
			);

			Unit predictedUnit = ai.predictedUnitId() == null ? null : unitRepository.findById(ai.predictedUnitId()).orElse(null);
			ProblemType predictedType = ai.predictedTypeId() == null ? null : typeRepository.findById(ai.predictedTypeId()).orElse(null);

			BigDecimal confidence = BigDecimal.valueOf(ai.confidence());
			boolean needsReview = shouldNeedsReview(predictedUnit, predictedType, confidence);

			saveAiSuccess(
				scanId,
				predictedUnit,
				predictedType,
				confidence,
				needsReview,
				ai.unitCandidatesJson(),
				ai.typeCandidatesJson(),
				ai.aiDraftJson()
			);

			log.info("AI 분류 완료 scanId={} 상태=AI_DONE 단원={} 유형={} 신뢰도={} 검토필요={}",
				scanId,
				predictedUnit == null ? null : predictedUnit.getId(),
				predictedType == null ? null : predictedType.getId(),
				confidence,
				needsReview
			);
		} catch (Exception e) {
			String reason = classifyFailureReason(e);
			boolean retryable = isRetryable(e);
			saveAiFailure(scanId, reason, retryable);

			if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
				log.warn("AI 호출 4xx scanId={} reason={} status={}", scanId, reason, rre.getRawStatusCode());
			} else {
				log.error("AI 처리 실패 scanId={} reason={}", scanId, reason, e);
			}
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private boolean isStillLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}

	private void saveAiSuccess(
		Long scanId,
		Unit predictedUnit,
		ProblemType predictedType,
		BigDecimal confidence,
		boolean needsReview,
		String unitCandidatesJson,
		String typeCandidatesJson,
		String aiDraftJson
	) {
		tx.executeWithoutResult(status -> {
			ProblemScan scan = scanRepository.findById(scanId)
				.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));

			scan.markAiSucceeded(
				predictedUnit,
				predictedType,
				confidence,
				needsReview,
				unitCandidatesJson,
				typeCandidatesJson,
				aiDraftJson,
				LocalDateTime.now()
			);
		});
	}

	private void saveAiFailure(Long scanId, String reason, boolean retryable) {
		tx.executeWithoutResult(status -> {
			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			scan.markAiFailed(reason);

			if (retryable) {
				scan.scheduleNextRetryForAi(LocalDateTime.now());
			}
		});
	}

	private void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			tx.executeWithoutResult(status -> scanRepository.unlock(scanId, lockOwner, lockToken));
		} catch (Exception unlockEx) {
			log.error("AI unlock 실패 scanId={}", scanId, unlockEx);
		}
	}

	private boolean shouldNeedsReview(Unit unit, ProblemType type, BigDecimal confidence) {
		if (unit == null || type == null) return true;
		return confidence.compareTo(BigDecimal.valueOf(0.60)) < 0;
	}

	private boolean isRetryable(Exception e) {
		if (e instanceof ResourceAccessException) return true;
		if (e instanceof RestClientResponseException rre) {
			int s = rre.getRawStatusCode();
			if (s == 429) return true;
			if (s >= 500) return true;
			return false;
		}
		return true;
	}

	private String classifyFailureReason(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("OCR_TEXT_EMPTY".equals(msg)) return "OCR_TEXT_EMPTY";
			if ("SCAN_NOT_FOUND".equals(msg)) return "SCAN_NOT_FOUND";
			return "ILLEGAL_STATE";
		}
		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return "AI_RATE_LIMIT";
			if (status >= 400 && status < 500) return "AI_CLIENT_4XX";
			if (status >= 500) return "AI_CLIENT_5XX";
			return "AI_CLIENT_ERROR";
		}
		if (e instanceof ResourceAccessException) return "AI_NETWORK_ERROR";
		return "AI_FAILED";
	}
}
