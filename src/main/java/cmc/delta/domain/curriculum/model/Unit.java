package cmc.delta.domain.curriculum.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "unit",
	indexes = {
		@Index(name = "idx_unit_parent", columnList = "parent_id"),
		@Index(name = "idx_unit_active_sort", columnList = "is_active, sort_order")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Unit {

	@Id
	@Column(length = 50, nullable = false)
	private String id;

	@Column(length = 100, nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Unit parent;

	@OneToMany(mappedBy = "parent")
	private List<Unit> children = new ArrayList<>();

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	public Unit(String id, String name, Unit parent, int sortOrder, boolean active) {
		this.id = id;
		this.name = name;
		this.parent = parent;
		this.sortOrder = sortOrder;
		this.active = active;
	}
}
