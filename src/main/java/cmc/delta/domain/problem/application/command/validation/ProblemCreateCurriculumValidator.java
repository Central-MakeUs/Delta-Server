package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class ProblemCreateCurriculumValidator {

	public Unit getFinalUnit(UnitJpaRepository unitRepository, String finalUnitId) {
		return unitRepository.findById(finalUnitId)
			.orElseThrow(ProblemFinalUnitNotFoundException::new);
	}

	public ProblemType getFinalType(ProblemTypeJpaRepository typeRepository, String finalTypeId) {
		return typeRepository.findById(finalTypeId)
			.orElseThrow(ProblemFinalTypeNotFoundException::new);
	}
}
