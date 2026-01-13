package cmc.delta.domain.curriculum.model;

import cmc.delta.domain.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "problem_type",
	indexes = {
		@Index(name = "idx_type_active_sort", columnList = "is_active, sort_order"),
		@Index(name = "idx_type_custom_sort", columnList = "created_by_user_id, is_custom, sort_order")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_type_user_name", columnNames = {"created_by_user_id", "name"})
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemType {

	@Id
	@Column(length = 50, nullable = false)
	private String id;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id")
	private User createdByUser;

	@Column(name = "is_custom", nullable = false)
	private boolean custom;

	public ProblemType(String id, String name, int sortOrder, boolean active, User createdByUser, boolean custom) {
		this.id = id;
		this.name = name;
		this.sortOrder = sortOrder;
		this.active = active;
		this.createdByUser = createdByUser;
		this.custom = custom;
	}
}
