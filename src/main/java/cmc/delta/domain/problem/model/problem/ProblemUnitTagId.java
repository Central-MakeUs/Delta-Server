package cmc.delta.domain.problem.model.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class ProblemUnitTagId implements Serializable {

	@Column(name = "problem_id", nullable = false)
	private Long problemId;

	@Column(name = "unit_id", length = 50, nullable = false)
	private String unitId;

	public Long getProblemId() {
		return problemId;
	}

	public String getUnitId() {
		return unitId;
	}
}
