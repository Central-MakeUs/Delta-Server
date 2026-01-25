package cmc.delta.domain.problem.adapter.in.worker.support.validation;

import cmc.delta.domain.problem.adapter.in.worker.exception.OcrTextEmptyException;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import org.springframework.stereotype.Component;

@Component
public class AiScanValidator {

	private static final int OCR_TEXT_MAX_CHARS = 3000;

	public AiValidatedInput validateAndNormalize(Long scanId, ProblemScan scan) {
		Long userId = scan.getUser().getId();
		String normalizedOcrText = normalizeOcrText(scan.getOcrPlainText());

		if (normalizedOcrText.isBlank()) {
			throw new OcrTextEmptyException(scanId);
		}
		return new AiValidatedInput(userId, normalizedOcrText);
	}

	private String normalizeOcrText(String ocrText) {
		if (ocrText == null)
			return "";
		String normalized = ocrText.replaceAll("\\s+", " ").trim();
		if (normalized.length() > OCR_TEXT_MAX_CHARS) {
			normalized = normalized.substring(0, OCR_TEXT_MAX_CHARS);
		}
		return normalized;
	}

	public record AiValidatedInput(Long userId, String normalizedOcrText) {
	}
}
