package cmc.delta.domain.problem.model.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class ProblemTypeTagId implements Serializable {

	@Column(name = "problem_id", nullable = false)
	private Long problemId;

	@Column(name = "type_id", length = 50, nullable = false)
	private String typeId;

	public ProblemTypeTagId(Long problemId, String typeId) {
		this.problemId = problemId;
		this.typeId = typeId;
	}

	public Long getProblemId() {
		return problemId;
	}

	public String getTypeId() {
		return typeId;
	}
}
