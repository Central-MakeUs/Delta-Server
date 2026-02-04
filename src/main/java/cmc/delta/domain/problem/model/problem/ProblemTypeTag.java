package cmc.delta.domain.problem.model.problem;

import cmc.delta.domain.curriculum.model.ProblemType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_type_tag", indexes = {
	@Index(name = "idx_problem_type_tag_type_problem", columnList = "type_id, problem_id"),
	@Index(name = "idx_problem_type_tag_problem", columnList = "problem_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uk_problem_type_tag", columnNames = {"problem_id", "type_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemTypeTag {

	@EmbeddedId
	private ProblemTypeTagId id;

	@MapsId("problemId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@MapsId("typeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ProblemType type;

	public ProblemTypeTag(ProblemType type) {
		this.type = type;
	}

	void attachTo(Problem problem) {
		initialize(problem);
	}

	private void initialize(Problem problem) {
		this.problem = problem;
		this.id = new ProblemTypeTagId(problem.getId(), type.getId());
	}
}
