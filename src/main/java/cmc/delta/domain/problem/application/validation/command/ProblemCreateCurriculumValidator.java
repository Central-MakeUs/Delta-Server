package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.adapter.out.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.exception.FinalUnitMustBeChildUnitException;
import cmc.delta.domain.problem.application.exception.ProblemFinalTypeNotFoundException;
import cmc.delta.domain.problem.application.exception.ProblemFinalUnitNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateCurriculumValidator {

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;

	public Unit getFinalUnit(String finalUnitId) {
		Unit unit = unitRepository.findById(finalUnitId)
			.orElseThrow(ProblemFinalUnitNotFoundException::new);

		if (unit.getParent() == null) {
			throw new FinalUnitMustBeChildUnitException();
		}

		return unit;
	}

	public ProblemType getFinalType(String finalTypeId) {
		return typeRepository.findById(finalTypeId)
			.orElseThrow(ProblemFinalTypeNotFoundException::new);
	}
}
