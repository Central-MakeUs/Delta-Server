package cmc.delta.domain.problem.adapter.in.worker.support.validation;

import cmc.delta.domain.problem.adapter.in.worker.exception.OcrTextEmptyException;
import cmc.delta.domain.problem.adapter.in.worker.support.ocr.LineDataSignalExtractor;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import org.springframework.stereotype.Component;

@Component
public class AiScanValidator {

	private static final int OCR_TEXT_MAX_CHARS = 3000;
	private static final String EMPTY = "";
	private static final String WHITESPACE_PATTERN = "\\s+";
	private static final String SINGLE_SPACE = " ";

	private final LineDataSignalExtractor signalExtractor;

	public AiScanValidator(LineDataSignalExtractor signalExtractor) {
		this.signalExtractor = signalExtractor;
	}

	public AiValidatedInput validateAndNormalize(Long scanId, ProblemScan scan) {
		Long userId = scan.getUser().getId();
		String normalizedOcrText = normalizeOcrText(scan.getOcrPlainText());
		OcrSignalSummary ocrSignals = signalExtractor.extract(scan.getOcrRawJson());

		if (normalizedOcrText.isBlank()) {
			throw new OcrTextEmptyException(scanId);
		}
		return new AiValidatedInput(userId, normalizedOcrText, ocrSignals);
	}

	private String normalizeOcrText(String ocrText) {
		if (ocrText == null) {
			return EMPTY;
		}
		String normalized = compressWhitespace(ocrText).trim();
		return trimToMaxLength(normalized);
	}

	private String compressWhitespace(String ocrText) {
		return ocrText.replaceAll(WHITESPACE_PATTERN, SINGLE_SPACE);
	}

	private String trimToMaxLength(String normalized) {
		if (normalized.length() <= OCR_TEXT_MAX_CHARS) {
			return normalized;
		}
		return normalized.substring(0, OCR_TEXT_MAX_CHARS);
	}

	public record AiValidatedInput(Long userId, String normalizedOcrText, OcrSignalSummary ocrSignals) {
	}
}
