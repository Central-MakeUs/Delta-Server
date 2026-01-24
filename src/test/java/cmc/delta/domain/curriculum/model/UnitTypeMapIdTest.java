package cmc.delta.domain.curriculum.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitTypeMapIdTest {

	@Test
	@DisplayName("UnitTypeMapId: unitId/typeId 기준으로 equals/hashCode가 동작")
	void equalsAndHashCode() {
		// given
		UnitTypeMapId id1 = new UnitTypeMapId("U1", "T1");
		UnitTypeMapId id2 = new UnitTypeMapId("U1", "T1");

		// then
		assertThat(id1).isEqualTo(id2);
		assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
		assertThat(id1.getUnitId()).isEqualTo("U1");
		assertThat(id1.getTypeId()).isEqualTo("T1");
	}
}
