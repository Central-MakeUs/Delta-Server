package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.application.port.out.UnitLoadPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateCurriculumValidator {

	private final ProblemTypeLoadPort typeLoadPort;
	private final UnitLoadPort unitLoadPort;

	public Unit getFinalUnit(String finalUnitId) {
		Unit unit = loadUnitOrThrow(finalUnitId);
		validateHasParent(unit);
		return unit;
	}

	public ProblemType getFinalType(Long userId, String finalTypeId) {
		return loadTypeOrThrow(userId, finalTypeId);
	}

	public List<ProblemType> getFinalTypes(Long userId, List<String> typeIds) {
		List<String> distinct = requireTypeIds(typeIds);
		List<ProblemType> found = loadTypesOrThrow(userId, distinct);
		return reorderByRequest(distinct, found);
	}

	private Unit loadUnitOrThrow(String finalUnitId) {
		return unitLoadPort.findById(finalUnitId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND));
	}

	private void validateHasParent(Unit unit) {
		if (unit.getParent() == null) {
			throw new ProblemStateException(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
		}
	}

	private ProblemType loadTypeOrThrow(Long userId, String finalTypeId) {
		return typeLoadPort.findActiveVisibleById(userId, finalTypeId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));
	}

	private List<String> requireTypeIds(List<String> typeIds) {
		if (typeIds == null || typeIds.isEmpty()) {
			throw new ProblemException(ErrorCode.INVALID_REQUEST);
		}
		return typeIds.stream().distinct().toList();
	}

	private List<ProblemType> loadTypesOrThrow(Long userId, List<String> typeIds) {
		List<ProblemType> found = typeLoadPort.findActiveVisibleByIds(userId, typeIds);
		if (found.size() != typeIds.size()) {
			throw new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
		}
		return found;
	}

	private List<ProblemType> reorderByRequest(List<String> requestedIds, List<ProblemType> found) {
		java.util.Map<String, ProblemType> byId = new java.util.HashMap<>();
		for (ProblemType type : found) {
			byId.put(type.getId(), type);
		}

		List<ProblemType> types = new ArrayList<>(requestedIds.size());
		for (String id : requestedIds) {
			ProblemType type = byId.get(id);
			if (type == null) {
				throw new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
			}
			types.add(type);
		}
		return types;
	}
}
