package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.common.exception.FinalUnitMustBeChildUnitException;
import cmc.delta.domain.problem.application.common.exception.ProblemFinalTypeNotFoundException;
import cmc.delta.domain.problem.application.common.exception.ProblemFinalUnitNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ProblemCreateCurriculumValidator {

	public Unit getFinalUnit(UnitJpaRepository unitRepository, String finalUnitId) {
		Unit unit = unitRepository.findById(finalUnitId)
			.orElseThrow(ProblemFinalUnitNotFoundException::new);

		if (unit.getParent() == null) {
			throw new FinalUnitMustBeChildUnitException();
		}

		return unit;
	}

	public ProblemType getFinalType(ProblemTypeJpaRepository typeRepository, String finalTypeId) {
		return typeRepository.findById(finalTypeId)
			.orElseThrow(ProblemFinalTypeNotFoundException::new);
	}
}
