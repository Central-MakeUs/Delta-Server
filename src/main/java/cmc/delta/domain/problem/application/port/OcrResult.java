package cmc.delta.domain.problem.application.port;

public record OcrResult(
	String plainText,
	String rawJson
) {}
