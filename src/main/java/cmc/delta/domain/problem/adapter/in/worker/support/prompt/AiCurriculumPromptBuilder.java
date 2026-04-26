package cmc.delta.domain.problem.adapter.in.worker.support.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import jakarta.annotation.PostConstruct;

/**
 * 과목·단원은 앱 실행 시 메모리에 캐싱되며, 변경 시 재시작이 필요합니다.
 * 고정 타입(is_custom=false)도 동일하게 메모리 캐싱됩니다.
 * 커스텀 타입(is_custom=true)은 유저별 Redis 캐시를 통해 조회됩니다.
 */
@Component
public class AiCurriculumPromptBuilder {

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeRepositoryPort problemTypeRepositoryPort;

	private List<AiCurriculumPrompt.Option> subjectOptions;
	private List<AiCurriculumPrompt.Option> unitOptions;
	private List<AiCurriculumPrompt.Option> fixedTypeOptions;

	public AiCurriculumPromptBuilder(
		UnitJpaRepository unitRepository,
		ProblemTypeRepositoryPort problemTypeRepositoryPort) {
		this.unitRepository = unitRepository;
		this.problemTypeRepositoryPort = problemTypeRepositoryPort;
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
		this.fixedTypeOptions = problemTypeRepositoryPort.findAllActiveFixed()
			.stream()
			.map(this::toOption)
			.toList();
	}

	public AiCurriculumPrompt build(Long userId, String normalizedOcrText, OcrSignalSummary ocrSignals) {
		List<AiCurriculumPrompt.Option> customTypeOptions = problemTypeRepositoryPort
			.findActiveCustomByUserId(userId)
			.stream()
			.map(this::toOption)
			.toList();

		List<AiCurriculumPrompt.Option> typeOptions = concatOptions(fixedTypeOptions, customTypeOptions);

		return new AiCurriculumPrompt(normalizedOcrText, ocrSignals, subjectOptions, unitOptions, typeOptions);
	}

	private List<AiCurriculumPrompt.Option> concatOptions(
		List<AiCurriculumPrompt.Option> fixed,
		List<AiCurriculumPrompt.Option> custom) {
		if (custom.isEmpty()) {
			return fixed;
		}
		return java.util.stream.Stream.concat(fixed.stream(), custom.stream()).toList();
	}

	private AiCurriculumPrompt.Option toOption(Unit unit) {
		return new AiCurriculumPrompt.Option(unit.getId(), unit.getName());
	}

	private AiCurriculumPrompt.Option toOption(ProblemType type) {
		return new AiCurriculumPrompt.Option(type.getId(), type.getName());
	}
}
