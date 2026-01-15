package cmc.delta.domain.problem.model;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
	name = "problem_scan",
	indexes = {
		@Index(name = "idx_problem_scan_user_created", columnList = "user_id, created_at"),
		@Index(name = "idx_problem_scan_status_retry", columnList = "status, next_retry_at"),
		@Index(name = "idx_problem_scan_status_locked", columnList = "status, locked_at"),
		@Index(name = "idx_problem_scan_pred_unit", columnList = "predicted_unit_id"),
		@Index(name = "idx_problem_scan_pred_type", columnList = "predicted_type_id"),
		@Index(name = "idx_problem_scan_render_created", columnList = "render_mode, created_at"),
		@Index(name = "idx_problem_scan_figure_created", columnList = "has_figure, created_at")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemScan extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScanStatus status;

	@Column(name = "has_figure", nullable = false)
	private boolean hasFigure;

	@Enumerated(EnumType.STRING)
	@Column(name = "render_mode", nullable = false, length = 20)
	private RenderMode renderMode;

	@Lob
	@Column(name = "ocr_plain_text", columnDefinition = "MEDIUMTEXT")
	private String ocrPlainText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ocr_raw_json", columnDefinition = "json")
	private String ocrRawJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_draft_json", columnDefinition = "json")
	private String aiDraftJson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "predicted_unit_id")
	private Unit predictedUnit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "predicted_type_id")
	private ProblemType predictedType;

	@Column(precision = 4, scale = 3)
	private BigDecimal confidence;

	@Column(name = "needs_review", nullable = false)
	private boolean needsReview;

	@Lob
	@Column(name = "ai_problem_latex", columnDefinition = "MEDIUMTEXT")
	private String aiProblemLatex;

	@Lob
	@Column(name = "ai_solution_latex", columnDefinition = "MEDIUMTEXT")
	private String aiSolutionLatex;

	@Enumerated(EnumType.STRING)
	@Column(name = "ai_answer_format", length = 20)
	private AnswerFormat aiAnswerFormat;

	@Column(name = "ai_answer_value", length = 255)
	private String aiAnswerValue;

	@Column(name = "ai_answer_choice_no")
	private Integer aiAnswerChoiceNo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_choices_json", columnDefinition = "json")
	private String aiChoicesJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_unit_candidates_json", columnDefinition = "json")
	private String aiUnitCandidatesJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_type_candidates_json", columnDefinition = "json")
	private String aiTypeCandidatesJson;

	@Column(name = "ocr_attempt_count", nullable = false)
	private int ocrAttemptCount;

	@Column(name = "ai_attempt_count", nullable = false)
	private int aiAttemptCount;

	@Column(name = "next_retry_at")
	private LocalDateTime nextRetryAt;

	@Column(name = "locked_at")
	private LocalDateTime lockedAt;

	@Column(name = "lock_owner", length = 64)
	private String lockOwner;

	@Column(name = "ocr_completed_at")
	private LocalDateTime ocrCompletedAt;

	@Column(name = "ai_completed_at")
	private LocalDateTime aiCompletedAt;

	@Column(name = "fail_reason", length = 255)
	private String failReason;

	@OneToOne(mappedBy = "scan", fetch = FetchType.LAZY)
	private Problem problem;

	@Column(name = "lock_token", length = 40)
	private String lockToken;

	private static final int OCR_MAX_ATTEMPTS = 3;
	private static final long OCR_BACKOFF_SECONDS = 30L;

	private static final int AI_MAX_ATTEMPTS = 3;
	private static final long AI_BACKOFF_SECONDS = 60L;

	// 429는 실패라기보다 대기라서 더 길게 허용
	private static final int AI_MAX_RATE_LIMIT_ATTEMPTS = 20;

	public static ProblemScan uploaded(User user) {
		ProblemScan s = new ProblemScan();
		s.user = user;
		s.status = ScanStatus.UPLOADED;
		s.hasFigure = false;
		s.renderMode = RenderMode.LATEX;
		s.needsReview = false;
		s.ocrAttemptCount = 0;
		s.aiAttemptCount = 0;
		return s;
	}

	public static ProblemScan createUploaded(User user) {
		return uploaded(user);
	}

	/**
	 * OCR 단계는 OCR 필드만 변경한다.
	 */
	public void markOcrSucceeded(String plainText, String rawJson, LocalDateTime completedAt) {
		this.ocrPlainText = plainText;
		this.ocrRawJson = rawJson;
		this.ocrCompletedAt = completedAt;
		this.status = ScanStatus.OCR_DONE;

		clearFailureAndRetry();
		this.ocrAttemptCount = 0;
	}

	/**
	 * AI 단계: 단원/유형 분류 결과 저장 + AI_DONE 전이
	 */
	public void markAiSucceeded(
		Unit predictedUnit,
		ProblemType predictedType,
		BigDecimal confidence,
		boolean needsReview,
		String aiUnitCandidatesJson,
		String aiTypeCandidatesJson,
		String aiDraftJson,
		LocalDateTime completedAt
	) {
		this.predictedUnit = predictedUnit;
		this.predictedType = predictedType;
		this.confidence = confidence;
		this.needsReview = needsReview;

		this.aiUnitCandidatesJson = aiUnitCandidatesJson;
		this.aiTypeCandidatesJson = aiTypeCandidatesJson;
		this.aiDraftJson = aiDraftJson;

		this.aiCompletedAt = completedAt;
		this.status = ScanStatus.AI_DONE;

		clearFailureAndRetry();
		this.aiAttemptCount = 0;
	}

	public void markOcrFailed(String reason) {
		this.failReason = reason;
		this.ocrAttemptCount += 1;
	}

	public void markAiFailed(String reason) {
		this.failReason = reason;
		this.aiAttemptCount += 1;
	}

	public void scheduleNextRetryForOcr(LocalDateTime now) {
		if (this.ocrAttemptCount >= OCR_MAX_ATTEMPTS) {
			failTerminal(this.failReason);
			return;
		}

		long delaySeconds = OCR_BACKOFF_SECONDS * this.ocrAttemptCount;
		scheduleRetry(now, delaySeconds, ScanStatus.UPLOADED);
	}

	public void scheduleNextRetryForAi(LocalDateTime now) {
		if (this.aiAttemptCount >= AI_MAX_ATTEMPTS) {
			failTerminal(this.failReason);
			return;
		}

		long delaySeconds = AI_BACKOFF_SECONDS * this.aiAttemptCount;
		scheduleRetry(now, delaySeconds, ScanStatus.OCR_DONE);
	}

	@Deprecated
	public void markAiDone(LocalDateTime completedAt) {
		this.aiCompletedAt = completedAt;
		this.status = ScanStatus.AI_DONE;
	}

	public void retryFailed(LocalDateTime now) {
		this.failReason = null;
		this.nextRetryAt = now;
		this.lockedAt = null;
		this.lockOwner = null;
		this.lockToken = null;

		boolean hasOcr = this.ocrPlainText != null && !this.ocrPlainText.isBlank();
		if (hasOcr) {
			this.aiAttemptCount = 0;
			this.status = ScanStatus.OCR_DONE;
			return;
		}

		this.ocrAttemptCount = 0;
		this.status = ScanStatus.UPLOADED;
	}

	public void markAiRateLimited(String reason) {
		this.failReason = reason;
		this.aiAttemptCount += 1;
	}

	public void scheduleNextRetryForAi(LocalDateTime now, long delaySeconds) {
		if (this.aiAttemptCount >= AI_MAX_RATE_LIMIT_ATTEMPTS) {
			failTerminal(this.failReason);
			return;
		}
		scheduleRetry(now, delaySeconds, ScanStatus.OCR_DONE);
	}

	public void scheduleNextRetryForOcr(LocalDateTime now, long delaySeconds) {
		scheduleRetry(now, delaySeconds, ScanStatus.UPLOADED);
	}

	public void markFailed(String reason) {
		failTerminal(reason);
	}

	private void clearFailureAndRetry() {
		this.failReason = null;
		this.nextRetryAt = null;
	}

	private void scheduleRetry(LocalDateTime now, long delaySeconds, ScanStatus nextStatus) {
		this.nextRetryAt = now.plusSeconds(Math.max(0, delaySeconds));
		this.status = nextStatus;
	}

	private void failTerminal(String reason) {
		this.failReason = reason;
		this.status = ScanStatus.FAILED;
		this.nextRetryAt = null;
	}
}
