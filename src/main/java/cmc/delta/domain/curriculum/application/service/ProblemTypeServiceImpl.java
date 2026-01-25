package cmc.delta.domain.curriculum.application.service;

import cmc.delta.domain.curriculum.application.port.in.type.ProblemTypeUseCase;
import cmc.delta.domain.curriculum.application.port.in.type.command.CreateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.SetProblemTypeActiveCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.UpdateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProblemTypeServiceImpl implements ProblemTypeUseCase {

	private final ProblemTypeRepositoryPort problemTypeRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;

	@Override
	@Transactional(readOnly = true)
	public ProblemTypeListResponse getMyTypes(Long userId, boolean includeInactive) {
		List<ProblemType> types = includeInactive
			? problemTypeRepositoryPort.findAllForUser(userId)
			: problemTypeRepositoryPort.findAllActiveForUser(userId);

		List<ProblemTypeItemResponse> items = types.stream().map(this::toItem).toList();
		return new ProblemTypeListResponse(items);
	}

	@Override
	public ProblemTypeItemResponse createCustomType(Long userId, CreateCustomProblemTypeCommand command) {
		String name = trimToNull(command.name());
		if (name == null) {
			throw new ProblemValidationException("name은 필수입니다.");
		}
		if (name.length() > 100) {
			throw new ProblemValidationException("name은 100자 이하여야 합니다.");
		}

		if (problemTypeRepositoryPort.existsCustomByUserIdAndName(userId, name)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 추가된 유형입니다.");
		}

		int sortOrder = problemTypeRepositoryPort.findMaxSortOrderVisibleForUser(userId) + 1;
		String id = generateCustomTypeId();
		User userRef = userRepositoryPort.getReferenceById(userId);

		ProblemType newType = new ProblemType(id, name, sortOrder, true, userRef, true);
		ProblemType saved = problemTypeRepositoryPort.save(newType);
		return toItem(saved);
	}

	@Override
	public void setActive(Long userId, String typeId, SetProblemTypeActiveCommand command) {
		ProblemType type = problemTypeRepositoryPort.findOwnedCustomById(userId, typeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));

		type.changeActive(command.active());
		problemTypeRepositoryPort.save(type);
	}

	@Override
	public ProblemTypeItemResponse updateCustomType(Long userId, String typeId, UpdateCustomProblemTypeCommand command) {
		if (command == null) {
			throw new ProblemValidationException("요청 본문이 비어있습니다.");
		}

		String newName = trimToNull(command.name());
		Integer newSortOrder = command.sortOrder();

		boolean hasNameChange = command.name() != null;
		boolean hasSortOrderChange = newSortOrder != null;
		if (!hasNameChange && !hasSortOrderChange) {
			throw new ProblemValidationException("수정할 값이 없습니다.");
		}

		ProblemType type = problemTypeRepositoryPort.findOwnedCustomById(userId, typeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));

		if (hasNameChange) {
			if (newName == null) {
				throw new ProblemValidationException("name은 필수입니다.");
			}
			if (newName.length() > 100) {
				throw new ProblemValidationException("name은 100자 이하여야 합니다.");
			}
			if (!newName.equals(type.getName()) && problemTypeRepositoryPort.existsCustomByUserIdAndName(userId, newName)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 추가된 유형입니다.");
			}
			type.rename(newName);
		}

		if (hasSortOrderChange) {
			if (newSortOrder.intValue() < 1) {
				throw new ProblemValidationException("sortOrder는 1 이상이어야 합니다.");
			}
			type.changeSortOrder(newSortOrder.intValue());
		}

		ProblemType saved = problemTypeRepositoryPort.save(type);
		return toItem(saved);
	}

	private String trimToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}

	private ProblemTypeItemResponse toItem(ProblemType type) {
		return new ProblemTypeItemResponse(
			type.getId(),
			type.getName(),
			type.isCustom(),
			type.isActive(),
			type.getSortOrder()
		);
	}

	private String generateCustomTypeId() {
		String hex = UUID.randomUUID().toString().replace("-", "");
		return "T_C_" + hex;
	}
}
