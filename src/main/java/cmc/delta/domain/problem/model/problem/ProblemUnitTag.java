package cmc.delta.domain.problem.model.problem;

import cmc.delta.domain.curriculum.model.Unit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "problem_unit_tag",
	indexes = {
		@Index(name = "idx_problem_unit_tag_unit_problem", columnList = "unit_id, problem_id"),
		@Index(name = "idx_problem_unit_tag_primary", columnList = "problem_id, is_primary")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemUnitTag {

	@EmbeddedId
	private ProblemUnitTagId id;

	@MapsId("problemId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@MapsId("unitId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false)
	private Unit unit;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

}
