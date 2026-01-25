package cmc.delta.domain.problem.application.support.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.application.port.out.UnitLoadPort;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitSubjectResolverTest {

	@Test
	@DisplayName("과목 resolve: unitId가 blank면 empty")
	void resolveByUnitId_whenBlank_thenEmpty() {
		// given
		UnitSubjectResolver sut = new UnitSubjectResolver(mock(UnitLoadPort.class));

		// when
		SubjectInfo out = sut.resolveByUnitId("  ");

		// then
		assertThat(out).isEqualTo(SubjectInfo.empty());
	}

	@Test
	@DisplayName("과목 resolve: unit이 없으면 empty")
	void resolveByUnitId_whenMissing_thenEmpty() {
		// given
		UnitLoadPort port = mock(UnitLoadPort.class);
		UnitSubjectResolver sut = new UnitSubjectResolver(port);
		when(port.findById("U1")).thenReturn(Optional.empty());

		// when
		SubjectInfo out = sut.resolveByUnitId("U1");

		// then
		assertThat(out).isEqualTo(SubjectInfo.empty());
	}

	@Test
	@DisplayName("과목 resolve: parent를 따라가 root unit을 과목으로 반환")
	void resolveByUnitId_whenChildUnit_thenReturnsRootSubject() {
		// given
		UnitLoadPort port = mock(UnitLoadPort.class);
		UnitSubjectResolver sut = new UnitSubjectResolver(port);

		Unit root = new Unit("S1", "대단원", null, 1, true);
		Unit child = new Unit("U1", "소단원", root, 1, true);
		when(port.findById("U1")).thenReturn(Optional.of(child));

		// when
		SubjectInfo out = sut.resolveByUnitId("U1");

		// then
		assertThat(out).isEqualTo(new SubjectInfo("S1", "대단원"));
	}
}
