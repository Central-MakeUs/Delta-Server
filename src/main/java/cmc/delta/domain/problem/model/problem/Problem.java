// src/main/java/cmc/delta/domain/problem/model/Problem.java
package cmc.delta.domain.problem.model.problem;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem", indexes = {
	@Index(name = "uk_problem_scan_id", columnList = "scan_id", unique = true),
	@Index(name = "idx_problem_user_created", columnList = "user_id, created_at"),
	@Index(name = "idx_problem_unit", columnList = "final_unit_id"),
	@Index(name = "idx_problem_type", columnList = "final_type_id"),
	@Index(name = "idx_problem_render_created", columnList = "render_mode, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseTimeEntity {

	private static final String ANSWER_FORMAT_REQUIRED_MESSAGE = "답변 형식이 설정되지 않았습니다.";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "scan_id", nullable = false, unique = true)
	private ProblemScan scan;

	@Column(name = "original_storage_key", length = 255)
	private String originalStorageKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "final_unit_id", nullable = false)
	private Unit finalUnit;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "final_type_id", nullable = false)
	private ProblemType finalType;

	@Enumerated(EnumType.STRING)
	@Column(name = "render_mode", nullable = false, length = 20)
	private RenderMode renderMode;

	@Lob
	@Column(name = "problem_markdown", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String problemMarkdown;

	@Enumerated(EnumType.STRING)
	@Column(name = "answer_format", nullable = false, length = 20)
	private AnswerFormat answerFormat;

	@Column(name = "answer_value", length = 255)
	private String answerValue;

	@Column(name = "answer_choice_no")
	private Integer answerChoiceNo;

	@Lob
	@Column(name = "solution_text", columnDefinition = "MEDIUMTEXT")
	private String solutionText;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProblemChoice> choices = new ArrayList<>();

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProblemUnitTag> unitTags = new ArrayList<>();

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ProblemTypeTag> typeTags = new ArrayList<>();

	public static Problem create(
		User user,
		ProblemScan scan,
		String originalStorageKey,
		Unit finalUnit,
		ProblemType finalType,
		RenderMode renderMode,
		String problemMarkdown,
		AnswerFormat answerFormat,
		String answerValue,
		Integer answerChoiceNo,
		String solutionText) {
		Problem p = new Problem();
		p.user = user;
		p.scan = scan;
		p.originalStorageKey = originalStorageKey;
		p.finalUnit = finalUnit;
		p.finalType = finalType;
		p.renderMode = renderMode;
		p.problemMarkdown = problemMarkdown;
		p.answerFormat = answerFormat;
		p.answerValue = answerValue;
		p.answerChoiceNo = answerChoiceNo;
		p.solutionText = solutionText;
		return p;
	}

	public void complete(String solutionText, java.time.LocalDateTime now) {
		updateSolutionText(solutionText);
		markCompletedAtIfEmpty(now);
	}

	public void updateAnswer(Integer answerChoiceNo, String answerValue) {
		AnswerFormat format = requireAnswerFormat();
		if (format == AnswerFormat.CHOICE) {
			applyChoiceAnswer(answerChoiceNo);
			return;
		}
		applyValueAnswer(answerValue);
	}

	public void updateSolutionText(String solutionText) {
		this.solutionText = solutionText;
	}

	public void replaceTypes(java.util.List<ProblemType> types) {
		this.typeTags.clear();

		if (types == null || types.isEmpty()) {
			return;
		}

		for (ProblemType type : types) {
			ProblemTypeTag tag = new ProblemTypeTag(type);
			tag.attachTo(this);
			this.typeTags.add(tag);
		}
	}

	public void applyUpdate(ProblemUpdateCommand cmd) {
		if (cmd.hasAnswerChange()) {
			updateAnswer(cmd.answerChoiceNo(), cmd.answerValue());
		}
		if (cmd.hasSolutionChange()) {
			updateSolutionText(cmd.solutionText());
		}
	}

	private void applyChoiceAnswer(Integer answerChoiceNo) {
		this.answerChoiceNo = answerChoiceNo;
		this.answerValue = null;
	}

	private void applyValueAnswer(String answerValue) {
		this.answerValue = answerValue;
		this.answerChoiceNo = null;
	}

	private AnswerFormat requireAnswerFormat() {
		if (this.answerFormat == null) {
			throw new IllegalStateException(ANSWER_FORMAT_REQUIRED_MESSAGE);
		}
		return this.answerFormat;
	}

	private void markCompletedAtIfEmpty(LocalDateTime now) {
		if (this.completedAt == null) {
			this.completedAt = now;
		}
	}
}
