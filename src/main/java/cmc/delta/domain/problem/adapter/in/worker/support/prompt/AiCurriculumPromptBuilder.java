package cmc.delta.domain.problem.adapter.in.worker.support.prompt;

import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiCurriculumPromptBuilder {

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository problemTypeRepository;

	public AiCurriculumPromptBuilder(
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository problemTypeRepository
	) {
		this.unitRepository = unitRepository;
		this.problemTypeRepository = problemTypeRepository;
	}

	public AiCurriculumPrompt build(Long userId, String normalizedOcrText) {
		List<AiCurriculumPrompt.Option> subjectOptions = unitRepository.findAllRootUnitsActive()
			.stream()
			.map(this::toOption)
			.toList();

		List<AiCurriculumPrompt.Option> unitOptions = unitRepository.findAllChildUnitsActive()
			.stream()
			.map(this::toOption)
			.toList();

		List<AiCurriculumPrompt.Option> typeOptions = problemTypeRepository.findAllActiveForUser(userId)
			.stream()
			.map(this::toOption)
			.toList();

		return new AiCurriculumPrompt(normalizedOcrText, subjectOptions, unitOptions, typeOptions);
	}

	private AiCurriculumPrompt.Option toOption(Unit unit) {
		return new AiCurriculumPrompt.Option(unit.getId(), unit.getName());
	}

	private AiCurriculumPrompt.Option toOption(ProblemType type) {
		return new AiCurriculumPrompt.Option(type.getId(), type.getName());
	}
}
