package cmc.delta.domain.problem.application.validation.command;

import java.util.ArrayList;
import java.util.List;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.application.port.out.UnitLoadPort;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateCurriculumValidator {

	private final ProblemTypeLoadPort typeLoadPort;
	private final UnitLoadPort unitLoadPort;

	public Unit getFinalUnit(String finalUnitId) {
		Unit unit = unitLoadPort.findById(finalUnitId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND));

		if (unit.getParent() == null) {
			throw new ProblemStateException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
		}

		return unit;
	}

	public ProblemType getFinalType(Long userId, String finalTypeId) {
		return typeLoadPort.findActiveVisibleById(userId, finalTypeId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));
	}


	public List<ProblemType> getFinalTypes(Long userId, List<String> typeIds) {
		if (typeIds == null || typeIds.isEmpty()) {
			throw new ProblemException(ErrorCode.INVALID_REQUEST);
		}

		List<String> distinct = typeIds.stream().distinct().toList();

		List<ProblemType> found = typeLoadPort.findActiveVisibleByIds(userId, distinct);
		if (found.size() != distinct.size()) {
			throw new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
		}

		// keep request order
		java.util.Map<String, ProblemType> byId = new java.util.HashMap<>();
		for (ProblemType t : found) {
			byId.put(t.getId(), t);
		}

		List<ProblemType> types = new ArrayList<>(distinct.size());
		for (String id : distinct) {
			ProblemType t = byId.get(id);
			if (t == null) {
				throw new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
			}
			types.add(t);
		}
		return types;
	}
}
