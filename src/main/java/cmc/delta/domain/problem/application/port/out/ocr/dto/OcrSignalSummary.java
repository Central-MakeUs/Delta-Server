package cmc.delta.domain.problem.application.port.out.ocr.dto;

public record OcrSignalSummary(
	int mathLineCount,
	int textLineCount,
	int codeLineCount,
	int pseudocodeLineCount) {

	public boolean hasCodeLine() {
		return codeLineCount > 0 || pseudocodeLineCount > 0;
	}
}
