package cmc.delta.domain.curriculum.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitTypeMapTest {

	@Test
	@DisplayName("UnitTypeMap 생성: unit/type으로 id(unitId,typeId)가 자동 생성됨")
	void constructor_setsEmbeddedIdFromAssociations() {
		// given
		Unit unit = new Unit("U1", "단원", null, 1, true);
		ProblemType type = new ProblemType("T1", "유형", 1, true, null, false);

		// when
		UnitTypeMap map = new UnitTypeMap(unit, type);

		// then
		assertThat(map.getUnit()).isSameAs(unit);
		assertThat(map.getType()).isSameAs(type);
		assertThat(map.getId().getUnitId()).isEqualTo("U1");
		assertThat(map.getId().getTypeId()).isEqualTo("T1");
	}
}
