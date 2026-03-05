package cmc.delta.domain.curriculum.application.service;

import cmc.delta.domain.curriculum.application.exception.ProblemTypeException;
import cmc.delta.domain.curriculum.application.port.in.type.ProblemTypeUseCase;
import cmc.delta.domain.curriculum.application.port.in.type.command.CreateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.SetProblemTypeActiveCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.UpdateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.application.validation.ProblemTypeCommandValidator;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.application.support.cache.ProblemStatsCacheEpochStore;
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

	private static final int ONE = 1;
	private static final String CUSTOM_TYPE_PREFIX = "T_C_";
	private static final String UUID_HYPHEN = "-";
	private static final String EMPTY = "";

	private final ProblemTypeRepositoryPort problemTypeRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProblemTypeCommandValidator validator;
	private final ProblemStatsCacheEpochStore statsCacheEpochStore;

	@Override
	@Transactional(readOnly = true)
	public ProblemTypeListResponse getMyTypes(Long userId, boolean includeInactive) {
		List<ProblemType> types = loadTypes(userId, includeInactive);
		return toListResponse(types);
	}

	@Override
	public ProblemTypeItemResponse createCustomType(Long userId, CreateCustomProblemTypeCommand command) {
		CreateCustomProblemTypeCommand validated = requireCreateCommand(command);
		String name = validator.requireName(validated.name());
		ensureCustomNameAvailable(userId, name);
		ProblemType newType = buildCustomType(userId, name);
		ProblemType saved = problemTypeRepositoryPort.save(newType);
		bumpStatsCache(userId);
		return toItem(saved);
	}

	@Override
	public void setActive(Long userId, String typeId, SetProblemTypeActiveCommand command) {
		SetProblemTypeActiveCommand validated = requireSetActiveCommand(command);
		ProblemType type = loadOwnedCustomType(userId, typeId);
		changeActive(type, validated.active());
		bumpStatsCache(userId);
	}

	@Override
	public ProblemTypeItemResponse updateCustomType(Long userId, String typeId,
		UpdateCustomProblemTypeCommand command) {
		UpdateCustomProblemTypeCommand validated = requireUpdateCommand(command);
		UpdateChangeSet changeSet = extractChangeSet(validated);
		ProblemType type = loadOwnedCustomType(userId, typeId);
		applyNameChange(userId, type, changeSet);
		applySortOrderChange(type, changeSet);
		ProblemType saved = problemTypeRepositoryPort.save(type);
		bumpStatsCache(userId);
		return toItem(saved);
	}

	private void bumpStatsCache(Long userId) {
		statsCacheEpochStore.bumpAfterCommit(userId);
	}

	private List<ProblemType> loadTypes(Long userId, boolean includeInactive) {
		if (includeInactive) {
			return problemTypeRepositoryPort.findAllForUser(userId);
		}
		return problemTypeRepositoryPort.findAllActiveForUser(userId);
	}

	private ProblemTypeListResponse toListResponse(List<ProblemType> types) {
		List<ProblemTypeItemResponse> items = types.stream().map(this::toItem).toList();
		return new ProblemTypeListResponse(items);
	}

	private CreateCustomProblemTypeCommand requireCreateCommand(CreateCustomProblemTypeCommand command) {
		if (command == null) {
			throw ProblemTypeException.invalid("요청 본문이 비어있습니다.");
		}
		return command;
	}

	private SetProblemTypeActiveCommand requireSetActiveCommand(SetProblemTypeActiveCommand command) {
		if (command == null) {
			throw ProblemTypeException.invalid("요청 본문이 비어있습니다.");
		}
		return command;
	}

	private UpdateCustomProblemTypeCommand requireUpdateCommand(UpdateCustomProblemTypeCommand command) {
		if (command == null) {
			throw ProblemTypeException.invalid("요청 본문이 비어있습니다.");
		}
		return command;
	}

	private void ensureCustomNameAvailable(Long userId, String name) {
		problemTypeRepositoryPort.findOwnedCustomByUserIdAndName(userId, name).ifPresent(existing -> {
			if (existing.isActive()) {
				throw ProblemTypeException.duplicateName();
			}
			throw ProblemTypeException.duplicateNameWithExistingId(existing.getId());
		});
	}

	private ProblemType buildCustomType(Long userId, String name) {
		int sortOrder = problemTypeRepositoryPort.findMaxSortOrderVisibleForUser(userId) + ONE;
		String id = generateCustomTypeId();
		User userRef = userRepositoryPort.getReferenceById(userId);
		return new ProblemType(id, name, sortOrder, true, userRef, true);
	}

	private ProblemType loadOwnedCustomType(Long userId, String typeId) {
		return problemTypeRepositoryPort.findOwnedCustomById(userId, typeId)
			.orElseThrow(ProblemTypeException::notFound);
	}

	private void changeActive(ProblemType type, boolean active) {
		type.changeActive(active);
		problemTypeRepositoryPort.save(type);
	}

	private UpdateChangeSet extractChangeSet(UpdateCustomProblemTypeCommand command) {
		Integer newSortOrder = command.sortOrder();
		boolean hasNameChange = command.name() != null;
		boolean hasSortOrderChange = newSortOrder != null;
		if (!hasNameChange && !hasSortOrderChange) {
			throw ProblemTypeException.invalid("수정할 값이 없습니다.");
		}
		validator.validateSortOrder(newSortOrder);
		return new UpdateChangeSet(hasNameChange, hasSortOrderChange, newSortOrder, command.name());
	}

	private void applyNameChange(Long userId, ProblemType type, UpdateChangeSet changeSet) {
		if (!changeSet.hasNameChange()) {
			return;
		}
		String newName = validator.requireNameWhenPresent(changeSet.rawName());
		if (!newName.equals(type.getName())) {
			validator.ensureNoDuplicateCustomName(userId, newName);
		}
		type.rename(newName);
	}

	private void applySortOrderChange(ProblemType type, UpdateChangeSet changeSet) {
		if (!changeSet.hasSortOrderChange()) {
			return;
		}
		type.changeSortOrder(changeSet.sortOrder().intValue());
	}

	private ProblemTypeItemResponse toItem(ProblemType type) {
		return new ProblemTypeItemResponse(
			type.getId(),
			type.getName(),
			type.isCustom(),
			type.isActive(),
			type.getSortOrder());
	}

	private String generateCustomTypeId() {
		String hex = UUID.randomUUID().toString().replace(UUID_HYPHEN, EMPTY);
		return CUSTOM_TYPE_PREFIX + hex;
	}

	private record UpdateChangeSet(
		boolean hasNameChange,
		boolean hasSortOrderChange,
		Integer sortOrder,
		String rawName) {
	}
}
