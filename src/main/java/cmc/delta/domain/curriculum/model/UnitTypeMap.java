package cmc.delta.domain.curriculum.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unit_type_map", indexes = {
	@Index(name = "idx_unit_type_map_type_unit", columnList = "type_id, unit_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnitTypeMap {

	@EmbeddedId
	private UnitTypeMapId id;

	@MapsId("unitId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false)
	private Unit unit;

	@MapsId("typeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ProblemType type;

	public UnitTypeMap(Unit unit, ProblemType type) {
		this.unit = unit;
		this.type = type;
		this.id = new UnitTypeMapId(unit.getId(), type.getId());
	}
}
