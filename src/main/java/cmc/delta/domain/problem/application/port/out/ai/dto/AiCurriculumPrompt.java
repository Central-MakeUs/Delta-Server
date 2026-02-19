package cmc.delta.domain.problem.application.port.out.ai.dto;

import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import java.util.List;

public record AiCurriculumPrompt(
	String ocrPlainText,
	OcrSignalSummary ocrSignals,
	List<Option> subjects,
	List<Option> units,
	List<Option> types) {
	public record Option(String id, String name) {
	}
}
