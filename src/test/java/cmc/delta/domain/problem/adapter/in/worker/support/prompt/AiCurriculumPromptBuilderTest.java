package cmc.delta.domain.problem.adapter.in.worker.support.prompt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiCurriculumPromptBuilderTest {

	@Test
	@DisplayName("AI 프롬프트 빌드: 활성 단원/유형을 option 리스트로 변환해 반환")
	void build_mapsUnitsAndTypesToOptions() {
		// given
		UnitJpaRepository unitRepository = mock(UnitJpaRepository.class);
		ProblemTypeJpaRepository typeRepository = mock(ProblemTypeJpaRepository.class);
		AiCurriculumPromptBuilder sut = new AiCurriculumPromptBuilder(unitRepository, typeRepository);

		when(unitRepository.findAllRootUnitsActive())
			.thenReturn(List.of(new Unit("S1", "대단원", null, 1, true)));
		when(unitRepository.findAllChildUnitsActive())
			.thenReturn(List.of(new Unit("U1", "소단원", null, 1, true)));

		when(typeRepository.findAllActiveForUser(10L))
			.thenReturn(List.of(new ProblemType("T1", "유형", 1, true, null, false)));

		// when
		AiCurriculumPrompt prompt = sut.build(10L, "ocr");

		// then
		assertThat(prompt.ocrPlainText()).isEqualTo("ocr");
		assertThat(prompt.subjects()).containsExactly(new AiCurriculumPrompt.Option("S1", "대단원"));
		assertThat(prompt.units()).containsExactly(new AiCurriculumPrompt.Option("U1", "소단원"));
		assertThat(prompt.types()).containsExactly(new AiCurriculumPrompt.Option("T1", "유형"));
	}
}
