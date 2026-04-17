package cmc.delta.domain.problem.application.validation.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.application.port.out.UnitLoadPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemCreateCurriculumValidatorTest {

	private UnitLoadPort unitLoadPort;
	private ProblemTypeLoadPort typeLoadPort;
	private ProblemCreateCurriculumValidator sut;

	@BeforeEach
	void setUp() {
		unitLoadPort = mock(UnitLoadPort.class);
		typeLoadPort = mock(ProblemTypeLoadPort.class);
		sut = new ProblemCreateCurriculumValidator(typeLoadPort, unitLoadPort);
	}

	// ── getFinalUnit ──────────────────────────────────────────────────────────

	@Test
	@DisplayName("getFinalUnit: 부모 있는 unit이면 반환한다")
	void getFinalUnit_whenUnitHasParent_returnsUnit() {
		// given
		Unit parent = unitWithId("PARENT");
		Unit leaf = unitWithParent("LEAF", parent);
		when(unitLoadPort.findById("LEAF")).thenReturn(Optional.of(leaf));

		// when
		Unit result = sut.getFinalUnit("LEAF");

		// then
		assertThat(result).isSameAs(leaf);
	}

	@Test
	@DisplayName("getFinalUnit: unit이 존재하지 않으면 PROBLEM_FINAL_UNIT_NOT_FOUND")
	void getFinalUnit_whenNotFound_throwsNotFound() {
		// given
		when(unitLoadPort.findById("X")).thenReturn(Optional.empty());

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalUnit("X"),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
	}

	@Test
	@DisplayName("getFinalUnit: 부모 없는 unit(최상위)이면 PROBLEM_FINAL_UNIT_NOT_FOUND")
	void getFinalUnit_whenNoParent_throwsNotFound() {
		// given
		Unit root = unitWithId("ROOT");  // parent == null
		when(unitLoadPort.findById("ROOT")).thenReturn(Optional.of(root));

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalUnit("ROOT"),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
	}

	// ── getFinalType ──────────────────────────────────────────────────────────

	@Test
	@DisplayName("getFinalType: 존재하는 type이면 반환한다")
	void getFinalType_whenFound_returnsType() {
		// given
		Long userId = 1L;
		ProblemType type = typeWithId("T1");
		when(typeLoadPort.findActiveVisibleById(userId, "T1")).thenReturn(Optional.of(type));

		// when
		ProblemType result = sut.getFinalType(userId, "T1");

		// then
		assertThat(result).isSameAs(type);
	}

	@Test
	@DisplayName("getFinalType: 존재하지 않으면 PROBLEM_FINAL_TYPE_NOT_FOUND")
	void getFinalType_whenNotFound_throwsNotFound() {
		// given
		Long userId = 1L;
		when(typeLoadPort.findActiveVisibleById(userId, "NONE")).thenReturn(Optional.empty());

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalType(userId, "NONE"),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
	}

	// ── getFinalTypes ─────────────────────────────────────────────────────────

	@Test
	@DisplayName("getFinalTypes: 요청 순서대로 정렬해서 반환한다")
	void getFinalTypes_reordersToRequestOrder() {
		// given
		Long userId = 1L;
		ProblemType t1 = typeWithId("T1");
		ProblemType t2 = typeWithId("T2");
		// DB는 T2, T1 순으로 반환했지만 요청은 T1, T2 순
		when(typeLoadPort.findActiveVisibleByIds(userId, List.of("T1", "T2"))).thenReturn(List.of(t2, t1));

		// when
		List<ProblemType> result = sut.getFinalTypes(userId, List.of("T1", "T2"));

		// then
		assertThat(result).containsExactly(t1, t2);
	}

	@Test
	@DisplayName("getFinalTypes: 중복 typeId는 하나로 처리된다")
	void getFinalTypes_deduplicatesIds() {
		// given
		Long userId = 1L;
		ProblemType t1 = typeWithId("T1");
		when(typeLoadPort.findActiveVisibleByIds(userId, List.of("T1"))).thenReturn(List.of(t1));

		// when
		List<ProblemType> result = sut.getFinalTypes(userId, List.of("T1", "T1"));

		// then
		assertThat(result).hasSize(1).containsExactly(t1);
	}

	@Test
	@DisplayName("getFinalTypes: typeIds가 null이면 INVALID_REQUEST")
	void getFinalTypes_whenNull_throwsInvalidRequest() {
		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalTypes(1L, null),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("getFinalTypes: typeIds가 비어있으면 INVALID_REQUEST")
	void getFinalTypes_whenEmpty_throwsInvalidRequest() {
		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalTypes(1L, List.of()),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("getFinalTypes: 요청한 type 중 일부가 없으면 PROBLEM_FINAL_TYPE_NOT_FOUND")
	void getFinalTypes_whenSomeNotFound_throwsNotFound() {
		// given
		Long userId = 1L;
		ProblemType t1 = typeWithId("T1");
		// T2는 반환되지 않음
		when(typeLoadPort.findActiveVisibleByIds(userId, List.of("T1", "T2"))).thenReturn(List.of(t1));

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getFinalTypes(userId, List.of("T1", "T2")),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private Unit unitWithId(String id) {
		return new Unit(id, "name-" + id, null, 1, true);
	}

	private Unit unitWithParent(String id, Unit parent) {
		return new Unit(id, "name-" + id, parent, 1, true);
	}

	private ProblemType typeWithId(String id) {
		return new ProblemType(id, "name-" + id, 1, true, null, false);
	}
}
