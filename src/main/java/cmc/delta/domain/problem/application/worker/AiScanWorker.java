package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.port.ai.AiClient;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.AbstractScanWorker;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiScanWorker extends AbstractScanWorker {

	private final ProblemScanJpaRepository scanRepository;
	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;
	private final AiClient aiClient;

	public AiScanWorker(
		Clock clock,
		ProblemScanJpaRepository scanRepository,
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository typeRepository,
		AiClient aiClient
	) {
		super(clock);
		this.scanRepository = scanRepository;
		this.unitRepository = unitRepository;
		this.typeRepository = typeRepository;
		this.aiClient = aiClient;
	}

	@Transactional
	public void runOnce(String lockOwner) {
		super.runOnceInternal(lockOwner);
	}

	@Override
	protected Long pickCandidateId(LocalDateTime now) {
		return scanRepository.findNextAiCandidateId(now).orElse(null);
	}

	@Override
	protected boolean tryLock(Long scanId, String lockOwner, LocalDateTime now) {
		return scanRepository.tryLockAiCandidate(scanId, lockOwner, now) == 1;
	}

	@Override
	protected void processLocked(Long scanId, LocalDateTime now) {
		ProblemScan scan = scanRepository.findById(scanId)
			.orElseThrow(() -> new IllegalStateException("problem_scan not found. id=" + scanId));

		String ocrText = scan.getOcrPlainText();
		if (ocrText == null || ocrText.isBlank()) {
			throw new IllegalStateException("OCR_TEXT_EMPTY");
		}

		Long userId = scan.getUser().getId();
		List<AiCurriculumPrompt.Option> subjectOptions = unitRepository.findAllRootUnitsActive()
			.stream()
			.map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> unitOptions = unitRepository.findAllByActiveTrueOrderBySortOrderAsc()
			.stream()
			.map(u -> new AiCurriculumPrompt.Option(u.getId(), u.getName()))
			.toList();

		List<AiCurriculumPrompt.Option> typeOptions = typeRepository.findAllActiveForUser(userId)
			.stream()
			.map(t -> new AiCurriculumPrompt.Option(t.getId(), t.getName()))
			.toList();

		AiCurriculumResult ai = aiClient.classifyCurriculum(
			new AiCurriculumPrompt(ocrText, subjectOptions, unitOptions, typeOptions)
		);

		Unit predictedUnit = ai.predictedUnitId() == null ? null
			: unitRepository.findById(ai.predictedUnitId()).orElse(null);

		ProblemType predictedType = ai.predictedTypeId() == null ? null
			: typeRepository.findById(ai.predictedTypeId()).orElse(null);

		BigDecimal confidence = BigDecimal.valueOf(ai.confidence());
		boolean needsReview = shouldNeedsReview(predictedUnit, predictedType, confidence);

		scan.markAiSucceeded(
			predictedUnit,
			predictedType,
			confidence,
			needsReview,
			ai.unitCandidatesJson(),
			ai.typeCandidatesJson(),
			ai.aiDraftJson(),
			now
		);

		log.info("AI 분류 완료 scanId={} 상태={} 단원={} 유형={} 신뢰도={} 검토필요={}",
			scanId,
			scan.getStatus().name(),
			predictedUnit == null ? null : predictedUnit.getId(),
			predictedType == null ? null : predictedType.getId(),
			confidence,
			needsReview
		);
	}

	private boolean shouldNeedsReview(Unit unit, ProblemType type, BigDecimal confidence) {
		if (unit == null || type == null) return true;
		return confidence.compareTo(BigDecimal.valueOf(0.60)) < 0;
	}

	@Override
	protected void handleFailure(Long scanId, LocalDateTime now, Exception e) {
		ProblemScan scan = scanRepository.findById(scanId).orElse(null);
		if (scan == null) {
			log.error("AI 처리 실패 scanId={} (스캔 없음)", scanId);
			return;
		}

		String reason = classifyFailureReason(e);

		if (e instanceof RestClientResponseException rre
			&& rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
			log.warn("AI 호출 4xx scanId={} 사유={} (재시도 정책 적용)", scanId, reason);
		} else {
			log.error("AI 처리 실패 scanId={} 사유={}", scanId, reason, e);
		}

		scan.markAiFailed(reason);
		scan.scheduleNextRetryForAi(now);

		log.warn("AI 재시도 예약 scanId={} aiAttemptCount={} nextRetryAt={} 사유={}",
			scanId, scan.getAiAttemptCount(), scan.getNextRetryAt(), reason);
	}

	private String classifyFailureReason(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("OCR_TEXT_EMPTY".equals(msg)) return "OCR_TEXT_EMPTY";
			if ("GEMINI_EMPTY_TEXT".equals(msg)) return "AI_EMPTY_TEXT";
			return "ILLEGAL_STATE";
		}
		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status >= 400 && status < 500) return "AI_CLIENT_4XX";
			if (status >= 500) return "AI_CLIENT_5XX";
			return "AI_CLIENT_ERROR";
		}
		if (e instanceof ResourceAccessException) return "AI_NETWORK_ERROR";
		return "AI_FAILED";
	}

	@Override
	protected void unlock(Long scanId, String lockOwner) {
		scanRepository.unlock(scanId, lockOwner);
	}
}
