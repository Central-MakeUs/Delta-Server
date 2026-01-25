package cmc.delta.domain.problem.application.port.out.ai.dto;

import java.util.List;

public record AiUnitTypePrompt(
	String ocrPlainText,
	List<Option> units,
	List<Option> types) {
	public record Option(String id, String name) {
	}
}
