package cmc.delta.domain.problem.application.validation.command;

import java.util.ArrayList;
import java.util.List;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateCurriculumValidator {

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;

	public Unit getFinalUnit(String finalUnitId) {
		Unit unit = unitRepository.findById(finalUnitId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND));

		if (unit.getParent() == null) {
			throw new ProblemStateException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
		}

		return unit;
	}

	public ProblemType getFinalType(String finalTypeId) {
		return typeRepository.findById(finalTypeId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));
	}


	public List<ProblemType> getFinalTypes(List<String> typeIds) {
		if (typeIds == null || typeIds.isEmpty()) {
			throw new ProblemException(ErrorCode.INVALID_REQUEST);
		}

		List<String> distinct = typeIds.stream().distinct().toList();

		List<ProblemType> types = new ArrayList<>();
		for (String id : distinct) {
			types.add(getFinalType(id));
		}
		return types;
	}
}
