package cmc.delta.domain.problem.application.port.ai;

import java.util.List;

public record AiCurriculumPrompt(
	String ocrPlainText,
	List<Option> subjects,
	List<Option> units,
	List<Option> types
) {
	public record Option(String id, String name) {}
}
