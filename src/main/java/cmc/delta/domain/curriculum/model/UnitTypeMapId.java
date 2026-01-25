package cmc.delta.domain.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class UnitTypeMapId implements Serializable {

	@Column(name = "unit_id", length = 50, nullable = false)
	private String unitId;

	@Column(name = "type_id", length = 50, nullable = false)
	private String typeId;

	public UnitTypeMapId(String unitId, String typeId) {
		this.unitId = unitId;
		this.typeId = typeId;
	}

	public String getUnitId() {
		return unitId;
	}

	public String getTypeId() {
		return typeId;
	}
}
