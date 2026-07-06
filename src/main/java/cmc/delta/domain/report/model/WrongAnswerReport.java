package cmc.delta.domain.report.model;

import cmc.delta.domain.report.model.enums.ReportStatus;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wrong_answer_report", indexes = {
	@Index(name = "idx_wrong_answer_report_user_id", columnList = "user_id, id"),
	@Index(name = "idx_wrong_answer_report_status_requested", columnList = "status, requested_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WrongAnswerReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ReportStatus status;

	@Column(name = "input_hash", nullable = false, length = 64)
	private String inputHash;

	@Column(name = "problem_count", nullable = false)
	private long problemCount;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Lob
	@Column(name = "aggregate_json", columnDefinition = "MEDIUMTEXT")
	private String aggregateJson;

	@Lob
	@Column(name = "narrative_json", columnDefinition = "MEDIUMTEXT")
	private String narrativeJson;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	public static WrongAnswerReport createPending(Long userId, String inputHash, long problemCount,
		LocalDateTime requestedAt) {
		WrongAnswerReport report = new WrongAnswerReport();
		report.userId = userId;
		report.inputHash = inputHash;
		report.problemCount = problemCount;
		report.status = ReportStatus.PENDING;
		report.requestedAt = requestedAt;
		return report;
	}

	public boolean canReuseFor(String nextInputHash) {
		if (!this.inputHash.equals(nextInputHash)) {
			return false;
		}
		return this.status == ReportStatus.PENDING
			|| this.status == ReportStatus.PROCESSING
			|| this.status == ReportStatus.READY;
	}

	public void markProcessing(LocalDateTime startedAt) {
		this.status = ReportStatus.PROCESSING;
		this.startedAt = startedAt;
		this.completedAt = null;
		this.failureReason = null;
	}

	public void markReady(String aggregateJson, String narrativeJson, LocalDateTime completedAt) {
		this.status = ReportStatus.READY;
		this.aggregateJson = aggregateJson;
		this.narrativeJson = narrativeJson;
		this.completedAt = completedAt;
		this.failureReason = null;
	}

	public void markTerminalFailure(String reason, LocalDateTime now) {
		this.status = ReportStatus.FAILED;
		this.failureReason = reason;
		this.completedAt = now;
	}
}
