package cmc.delta.domain.problem.application.port.out.ocr.dto;

public record OcrResult(
	String plainText,
	String latexStyled,
	String rawJson
) {}
