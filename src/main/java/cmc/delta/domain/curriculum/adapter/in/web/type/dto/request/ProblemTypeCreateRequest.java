package cmc.delta.domain.curriculum.adapter.in.web.type.dto.request;

import cmc.delta.domain.curriculum.application.port.in.type.command.CreateCustomProblemTypeCommand;

public record ProblemTypeCreateRequest(String name) {
	public CreateCustomProblemTypeCommand toCommand() {
		return new CreateCustomProblemTypeCommand(name);
	}
}
