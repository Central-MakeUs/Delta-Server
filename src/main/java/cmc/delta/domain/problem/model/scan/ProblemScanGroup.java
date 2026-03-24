package cmc.delta.domain.problem.model.scan;

import cmc.delta.domain.user.model.User;
import cmc.delta.global.persistence.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_scan_group", indexes = {
	@Index(name = "idx_problem_scan_group_user_created", columnList = "user_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemScanGroup extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public static ProblemScanGroup create(User user) {
		ProblemScanGroup group = new ProblemScanGroup();
		group.user = user;
		return group;
	}
}
