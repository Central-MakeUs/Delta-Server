package cmc.delta.domain.curriculum.application.service;

import cmc.delta.domain.curriculum.application.port.in.type.ProblemTypeUseCase;
import cmc.delta.domain.curriculum.application.port.in.type.command.CreateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.SetProblemTypeActiveCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.UpdateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;
import cmc.delta.domain.curriculum.application.exception.ProblemTypeException;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.application.validation.ProblemTypeCommandValidator;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
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
	private final ProblemTypeCommandValidator validator;

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
		if (command == null) {
			throw ProblemTypeException.invalid("요청 본문이 비어있습니다.");
		}

		String name = validator.requireName(command.name());
		validator.ensureNoDuplicateCustomName(userId, name);

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
			.orElseThrow(ProblemTypeException::notFound);

		type.changeActive(command.active());
		problemTypeRepositoryPort.save(type);
	}

	@Override
	public ProblemTypeItemResponse updateCustomType(Long userId, String typeId, UpdateCustomProblemTypeCommand command) {
		if (command == null) {
			throw ProblemTypeException.invalid("요청 본문이 비어있습니다.");
		}

		Integer newSortOrder = command.sortOrder();
		boolean hasNameChange = command.name() != null;
		boolean hasSortOrderChange = newSortOrder != null;
		if (!hasNameChange && !hasSortOrderChange) {
			throw ProblemTypeException.invalid("수정할 값이 없습니다.");
		}
		validator.validateSortOrder(newSortOrder);

		ProblemType type = problemTypeRepositoryPort.findOwnedCustomById(userId, typeId)
			.orElseThrow(ProblemTypeException::notFound);

		if (hasNameChange) {
			String newName = validator.requireNameWhenPresent(command.name());
			if (!newName.equals(type.getName())) {
				validator.ensureNoDuplicateCustomName(userId, newName);
			}
			type.rename(newName);
		}

		if (hasSortOrderChange) {
			type.changeSortOrder(newSortOrder.intValue());
		}

		ProblemType saved = problemTypeRepositoryPort.save(type);
		return toItem(saved);
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
