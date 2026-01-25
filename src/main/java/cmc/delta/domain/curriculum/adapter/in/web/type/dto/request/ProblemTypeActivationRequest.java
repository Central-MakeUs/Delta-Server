package cmc.delta.domain.curriculum.adapter.in.web.type.dto.request;

import cmc.delta.domain.curriculum.application.port.in.type.command.SetProblemTypeActiveCommand;

public record ProblemTypeActivationRequest(boolean active) {
	public SetProblemTypeActiveCommand toCommand() {
		return new SetProblemTypeActiveCommand(active);
	}
}
