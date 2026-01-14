package cmc.delta.domain.problem.application.port.ocr;

public record OcrResult(
	String plainText,
	String latexStyled,
	String rawJson
) {}
