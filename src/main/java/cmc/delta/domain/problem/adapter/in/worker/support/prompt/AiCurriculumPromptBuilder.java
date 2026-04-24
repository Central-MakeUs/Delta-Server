package cmc.delta.domain.problem.adapter.in.worker.support.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import jakarta.annotation.PostConstruct;

/**
 * 현재 코드는 단원과 과목에 대한 코드입니다.
 * 과목 단원은 앱 실행시 자동 메모리에 캐싱되며, 과목과 단원을 추가할 경우 앱을 재시작해야 반영됩니다.
 */
@Component
public class AiCurriculumPromptBuilder {

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository problemTypeRepository;

	private List<AiCurriculumPrompt.Option> subjectOptions;
	private List<AiCurriculumPrompt.Option> unitOptions;

	public AiCurriculumPromptBuilder(
		UnitJpaRepository unitRepository,
		ProblemTypeJpaRepository problemTypeRepository) {
		this.unitRepository = unitRepository;
		this.problemTypeRepository = problemTypeRepository;
	}

	@PostConstruct
	void init() {
		this.subjectOptions = unitRepository.findAllRootUnitsActive()
			.stream()
			.map(this::toOption)
			.toList();
		this.unitOptions = unitRepository.findAllChildUnitsActive()
			.stream()
			.map(this::toOption)
			.toList();
	}

	public AiCurriculumPrompt build(Long userId, String normalizedOcrText, OcrSignalSummary ocrSignals) {
		List<AiCurriculumPrompt.Option> typeOptions = problemTypeRepository.findAllActiveForUser(userId)
			.stream()
			.map(this::toOption)
			.toList();

		return new AiCurriculumPrompt(normalizedOcrText, ocrSignals, subjectOptions, unitOptions, typeOptions);
	}

	private AiCurriculumPrompt.Option toOption(Unit unit) {
		return new AiCurriculumPrompt.Option(unit.getId(), unit.getName());
	}

	private AiCurriculumPrompt.Option toOption(ProblemType type) {
		return new AiCurriculumPrompt.Option(type.getId(), type.getName());
	}
}
