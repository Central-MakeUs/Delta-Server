package cmc.delta.domain.problem.application.port.ai;

import java.util.List;

public record AiCurriculumPrompt(
	String ocrPlainText,
	List<Option> subjects, // Unit(parent=null)
	List<Option> units,    // Unit(전체 or 활성)
	List<Option> types     // ProblemType
) {
	public record Option(String id, String name) {}
}
