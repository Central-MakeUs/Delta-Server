package cmc.delta.domain.problem.model.problem;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemTest {

	@Test
	@DisplayName("complete: completedAt이 비어있으면 now로 채우고, 이미 있으면 유지")
	void complete_setsCompletedAtOnce() {
		// given
		Problem p = problem(AnswerFormat.TEXT);
		LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
		LocalDateTime t2 = LocalDateTime.of(2026, 1, 1, 0, 1, 0);

		// when
		p.complete("sol1", t1);
		p.complete("sol2", t2);

		// then
		assertThat(p.getSolutionText()).isEqualTo("sol2");
		assertThat(p.getCompletedAt()).isEqualTo(t1);
	}

	@Test
	@DisplayName("updateAnswer: 객관식이면 choiceNo만 유지하고 answerValue는 null")
	void updateAnswer_whenChoice_thenSetsChoiceNoAndNullValue() {
		// given
		Problem p = problem(AnswerFormat.CHOICE);

		// when
		p.updateAnswer(3, "ignored");

		// then
		assertThat(p.getAnswerChoiceNo()).isEqualTo(3);
		assertThat(p.getAnswerValue()).isNull();
	}

	@Test
	@DisplayName("updateAnswer: 주관식이면 answerValue만 유지하고 choiceNo는 null")
	void updateAnswer_whenText_thenSetsValueAndNullChoiceNo() {
		// given
		Problem p = problem(AnswerFormat.TEXT);

		// when
		p.updateAnswer(1, "ans");

		// then
		assertThat(p.getAnswerValue()).isEqualTo("ans");
		assertThat(p.getAnswerChoiceNo()).isNull();
	}

	@Test
	@DisplayName("applyUpdate: 변경 플래그에 따라 answer/solution 업데이트를 적용")
	void applyUpdate_appliesBasedOnFlags() {
		// given
		Problem p = problem(AnswerFormat.TEXT);
		ProblemUpdateCommand cmd = new ProblemUpdateCommand(
			null,
			"newAns",
			"newSol",
			true,
			true
		);

		// when
		p.applyUpdate(cmd);

		// then
		assertThat(p.getAnswerValue()).isEqualTo("newAns");
		assertThat(p.getSolutionText()).isEqualTo("newSol");
	}

	@Test
	@DisplayName("replaceTypes: null/empty면 typeTags를 비우고, 값이 있으면 ProblemTypeTag를 생성해 attach")
	void replaceTypes_replacesTags() {
		// given
		Problem p = problem(AnswerFormat.TEXT);
		setId(p, 1L);

		ProblemType t1 = new ProblemType("T1", "유형1", 1, true, null, false);
		ProblemType t2 = new ProblemType("T2", "유형2", 2, true, null, false);

		// when
		p.replaceTypes(List.of(t1, t2));

		// then
		assertThat(p.getTypeTags()).hasSize(2);
		assertThat(p.getTypeTags().get(0).getProblem()).isSameAs(p);
		assertThat(p.getTypeTags().get(0).getType()).isSameAs(t1);
		assertThat(p.getTypeTags().get(0).getId().getProblemId()).isEqualTo(1L);
		assertThat(p.getTypeTags().get(0).getId().getTypeId()).isEqualTo("T1");

		// when
		p.replaceTypes(List.of());

		// then
		assertThat(p.getTypeTags()).isEmpty();
	}

	private Problem problem(AnswerFormat format) {
		User user = mock(User.class);
		ProblemScan scan = mock(ProblemScan.class);
		Unit unit = mock(Unit.class);
		cmc.delta.domain.curriculum.model.ProblemType type = mock(cmc.delta.domain.curriculum.model.ProblemType.class);
		when(type.getId()).thenReturn("T0");
		return Problem.create(user, scan, unit, type, RenderMode.LATEX, "md", format, "a", 1, "s");
	}

	private void setId(Problem p, long id) {
		try {
			Field f = Problem.class.getDeclaredField("id");
			f.setAccessible(true);
			f.set(p, id);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 id 세팅 실패", e);
		}
	}
}
