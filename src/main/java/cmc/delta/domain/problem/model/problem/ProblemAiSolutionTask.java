package cmc.delta.domain.problem.model.problem;

import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.ProblemAiSolutionStatus;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_ai_solution_task", indexes = {
	@Index(name = "uk_problem_ai_solution_task_problem_id", columnList = "problem_id", unique = true),
	@Index(name = "idx_problem_ai_solution_task_status_retry", columnList = "status, next_retry_at"),
	@Index(name = "idx_problem_ai_solution_task_status_locked", columnList = "status, locked_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemAiSolutionTask extends BaseTimeEntity {

	private static final int ZERO = 0;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false, unique = true)
	private Problem problem;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ProblemAiSolutionStatus status;

	@Column(name = "prompt_version", nullable = false, length = 32)
	private String promptVersion;

	@Column(name = "input_hash", nullable = false, length = 64)
	private String inputHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "answer_format", nullable = false, length = 20)
	private AnswerFormat answerFormat;

	@Column(name = "answer_value", length = 255)
	private String answerValue;

	@Column(name = "answer_choice_no")
	private Integer answerChoiceNo;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Lob
	@Column(name = "problem_markdown_snapshot", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String problemMarkdownSnapshot;

	@Lob
	@Column(name = "solution_latex", columnDefinition = "MEDIUMTEXT")
	private String solutionLatex;

	@Lob
	@Column(name = "solution_text", columnDefinition = "MEDIUMTEXT")
	private String solutionText;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_retry_at")
	private LocalDateTime nextRetryAt;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "locked_at")
	private LocalDateTime lockedAt;

	@Column(name = "lock_owner", length = 64)
	private String lockOwner;

	@Column(name = "lock_token", length = 40)
	private String lockToken;

	public static ProblemAiSolutionTask createPending(
		Problem problem,
		String promptVersion,
		String inputHash,
		String problemMarkdownSnapshot,
		AnswerFormat answerFormat,
		String answerValue,
		Integer answerChoiceNo,
		LocalDateTime requestedAt) {
		ProblemAiSolutionTask task = new ProblemAiSolutionTask();
		task.problem = problem;
		task.promptVersion = promptVersion;
		task.inputHash = inputHash;
		task.problemMarkdownSnapshot = problemMarkdownSnapshot;
		task.answerFormat = answerFormat;
		task.answerValue = answerValue;
		task.answerChoiceNo = answerChoiceNo;
		task.status = ProblemAiSolutionStatus.PENDING;
		task.requestedAt = requestedAt;
		task.startedAt = null;
		task.completedAt = null;
		task.solutionLatex = null;
		task.solutionText = null;
		task.failureReason = null;
		task.attemptCount = ZERO;
		task.nextRetryAt = requestedAt;
		task.clearLock();
		return task;
	}

	public boolean canReuseFor(String nextInputHash) {
		if (!this.inputHash.equals(nextInputHash)) {
			return false;
		}
		return this.status == ProblemAiSolutionStatus.PENDING
			|| this.status == ProblemAiSolutionStatus.PROCESSING
			|| this.status == ProblemAiSolutionStatus.READY;
	}

	public void requestAgain(
		String promptVersion,
		String inputHash,
		String problemMarkdownSnapshot,
		AnswerFormat answerFormat,
		String answerValue,
		Integer answerChoiceNo,
		LocalDateTime requestedAt) {
		this.promptVersion = promptVersion;
		this.inputHash = inputHash;
		this.problemMarkdownSnapshot = problemMarkdownSnapshot;
		this.answerFormat = answerFormat;
		this.answerValue = answerValue;
		this.answerChoiceNo = answerChoiceNo;
		this.status = ProblemAiSolutionStatus.PENDING;
		this.requestedAt = requestedAt;
		this.startedAt = null;
		this.completedAt = null;
		this.solutionLatex = null;
		this.solutionText = null;
		this.failureReason = null;
		this.attemptCount = ZERO;
		this.nextRetryAt = requestedAt;
		clearLock();
	}

	public void markReady(String solutionLatex, String solutionText, LocalDateTime completedAt) {
		this.status = ProblemAiSolutionStatus.READY;
		this.solutionLatex = solutionLatex;
		this.solutionText = solutionText;
		this.completedAt = completedAt;
		this.failureReason = null;
		this.nextRetryAt = null;
		clearLock();
	}

	public void markProcessing(LocalDateTime startedAt) {
		this.status = ProblemAiSolutionStatus.PROCESSING;
		this.startedAt = startedAt;
		this.completedAt = null;
		this.failureReason = null;
	}

	public void markRetryableFailure(String reason, LocalDateTime now, long delaySeconds, int maxAttempts) {
		this.attemptCount += 1;
		this.failureReason = reason;
		if (this.attemptCount >= maxAttempts) {
			markTerminalFailure(reason, now);
			return;
		}
		this.status = ProblemAiSolutionStatus.PENDING;
		this.nextRetryAt = now.plusSeconds(Math.max(ZERO, delaySeconds));
		clearLock();
	}

	public void markTerminalFailure(String reason, LocalDateTime now) {
		this.status = ProblemAiSolutionStatus.FAILED;
		this.failureReason = reason;
		this.completedAt = now;
		this.nextRetryAt = null;
		clearLock();
	}

	private void clearLock() {
		this.lockedAt = null;
		this.lockOwner = null;
		this.lockToken = null;
	}
}
