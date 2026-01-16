package cmc.delta.domain.problem.application.scan.port.out.ocr.dto;

public record OcrResult(
	String plainText,
	String latexStyled,
	String rawJson
) {}
