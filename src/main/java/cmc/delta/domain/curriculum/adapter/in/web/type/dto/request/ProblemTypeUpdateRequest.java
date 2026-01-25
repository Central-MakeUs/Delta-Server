package cmc.delta.domain.curriculum.adapter.in.web.type.dto.request;

import cmc.delta.domain.curriculum.application.port.in.type.command.UpdateCustomProblemTypeCommand;

public record ProblemTypeUpdateRequest(String name, Integer sortOrder) {
	public UpdateCustomProblemTypeCommand toCommand() {
		return new UpdateCustomProblemTypeCommand(name, sortOrder);
	}
}
