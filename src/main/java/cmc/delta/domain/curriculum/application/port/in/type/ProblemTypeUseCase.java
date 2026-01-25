package cmc.delta.domain.curriculum.application.port.in.type;

import cmc.delta.domain.curriculum.application.port.in.type.command.CreateCustomProblemTypeCommand;

import cmc.delta.domain.curriculum.application.port.in.type.command.UpdateCustomProblemTypeCommand;
import cmc.delta.domain.curriculum.application.port.in.type.command.SetProblemTypeActiveCommand;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;

public interface ProblemTypeUseCase {
	ProblemTypeListResponse getMyTypes(Long userId, boolean includeInactive);

	ProblemTypeItemResponse createCustomType(Long userId, CreateCustomProblemTypeCommand command);

	ProblemTypeItemResponse updateCustomType(Long userId, String typeId, UpdateCustomProblemTypeCommand command);

	void setActive(Long userId, String typeId, SetProblemTypeActiveCommand command);
}
