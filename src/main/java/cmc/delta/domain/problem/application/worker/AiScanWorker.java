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

	private static final long DEFAULT_RATE_LIMIT_DELAY_SECONDS = 180L;
	private static final long MIN_RATE_LIMIT_DELAY_SECONDS = 60L;
	private static final int OCR_TEXT_MAX_CHARS = 3000;

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
		if (!isStillLockedByMe(scanId, lockOwner, lockToken)) return;

		try {
			ProblemScan scan = loadScanOrThrow(scanId);

			String ocrText = normalizeOcrText(scan.getOcrPlainText());
			if (ocrText.isBlank()) throw new IllegalStateException("OCR_TEXT_EMPTY");

			Long userId = scan.getUser().getId(); // 프록시는 id 접근 가능(초기화 없이도 됨)

			AiCurriculumPrompt prompt = buildPrompt(userId, ocrText);
			AiCurriculumResult ai = aiClient.classifyCurriculum(prompt);

			// 외부 호출 후 저장 직전에 lock 재확인(lease 만료/steal 방지)
			if (!isStillLockedByMe(scanId, lockOwner, lockToken)) return;

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
		String reason = classifyFailureReason(e);

		// OCR_TEXT_EMPTY는 AI를 재시도해봐야 의미가 거의 없어서 즉시 터미널 처리 권장
		if ("OCR_TEXT_EMPTY".equals(reason)) {
			saveAiFailure(scanId, lockOwner, lockToken, reason, false, null);
			log.warn("AI 처리 불가 scanId={} reason={}", scanId, reason);
			return;
		}

		boolean retryable = isRetryable(e);
		Long retryAfterSec = extractRetryAfterSecondsIf429(e);
		saveAiFailure(scanId, lockOwner, lockToken, reason, retryable, retryAfterSec);

		if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
			log.warn("AI 호출 4xx scanId={} reason={} status={}", scanId, reason, rre.getRawStatusCode());
		} else {
			log.error("AI 처리 실패 scanId={} reason={}", scanId, reason, e);
		}
	}

	private boolean isStillLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
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

	private Long parseLongOrNull(String s) {
		if (s == null || s.isBlank()) return null;
		try {
			return Long.valueOf(s.trim());
		} catch (NumberFormatException ignore) {
			return null;
		}
	}

	private void saveAiSuccess(
		Long scanId, String lockOwner, String lockToken,
		Unit predictedUnit, ProblemType predictedType, BigDecimal confidence, boolean needsReview,
		String unitCandidatesJson, String typeCandidatesJson, String aiDraftJson
	) {
		tx.executeWithoutResult(status -> {
			if (scanRepository.existsLockedBy(scanId, lockOwner, lockToken) == null) return;

			ProblemScan scan = loadScanOrThrow(scanId);

			scan.markAiSucceeded(
				predictedUnit, predictedType, confidence, needsReview,
				unitCandidatesJson, typeCandidatesJson, aiDraftJson,
				LocalDateTime.now()
			);
		});
	}

	private void saveAiFailure(
		Long scanId, String lockOwner, String lockToken,
		String reason, boolean retryable, Long retryAfterSec
	) {
		tx.executeWithoutResult(status -> {
			if (scanRepository.existsLockedBy(scanId, lockOwner, lockToken) == null) return;

			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			// non-retryable은 즉시 FAILED(무한 루프 방지)
			if (!retryable) {
				scan.markFailed(reason);
				return;
			}

			// 429는 별도 backoff + rate-limit max attempts
			if ("AI_RATE_LIMIT".equals(reason)) {
				scan.markAiRateLimited(reason);

				long delay = computeRateLimitDelaySeconds(retryAfterSec);
				scan.scheduleNextRetryForAi(LocalDateTime.now(), delay);
				return;
			}

			// 그 외 retryable은 기존 정책(3회 후 FAILED)
			scan.markAiFailed(reason);
			scan.scheduleNextRetryForAi(LocalDateTime.now());
		});
	}

	private long computeRateLimitDelaySeconds(Long retryAfterSec) {
		if (retryAfterSec == null || retryAfterSec <= 0) return DEFAULT_RATE_LIMIT_DELAY_SECONDS;
		return Math.max(retryAfterSec, MIN_RATE_LIMIT_DELAY_SECONDS);
	}

	private Long extractRetryAfterSecondsIf429(Exception e) {
		if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() == 429) {
			var headers = rre.getResponseHeaders();
			if (headers == null) return null;
			String v = headers.getFirst("Retry-After");
			if (v == null || v.isBlank()) return null;
			try {
				return Long.parseLong(v.trim());
			} catch (NumberFormatException ignore) {
				return null;
			}
		}
		return null;
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
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			// 이 둘은 재시도해도 의미 거의 없음
			if ("OCR_TEXT_EMPTY".equals(msg)) return false;
			if ("SCAN_NOT_FOUND".equals(msg)) return false;
			return false;
		}

		if (e instanceof ResourceAccessException) return true;

		if (e instanceof RestClientResponseException rre) {
			int s = rre.getRawStatusCode();
			if (s == 429) return true;
			if (s == 408) return true;
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
