package cmc.delta.domain.problem.model.problem;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_choice", uniqueConstraints = {
	@UniqueConstraint(name = "uk_problem_choice", columnNames = {"problem_id", "choice_no"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemChoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@Column(name = "choice_no", nullable = false)
	private int choiceNo;

	@Column(length = 10, nullable = false)
	private String label;

	@Lob
	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String text;

	public ProblemChoice(Problem problem, int choiceNo, String label, String text) {
		initialize(problem, choiceNo, label, text);
	}

	private void initialize(Problem problem, int choiceNo, String label, String text) {
		this.problem = problem;
		this.choiceNo = choiceNo;
		this.label = label;
		this.text = text;
	}
}
