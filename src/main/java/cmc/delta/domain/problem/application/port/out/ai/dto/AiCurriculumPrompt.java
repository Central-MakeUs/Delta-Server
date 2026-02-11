package cmc.delta.domain.problem.application.port.out.ai.dto;

import java.util.List;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;

public record AiCurriculumPrompt(
	String ocrPlainText,
	OcrSignalSummary ocrSignals,
	List<Option> subjects,
	List<Option> units,
	List<Option> types) {
	public record Option(String id, String name) {
	}
}
