package cmc.delta.domain.problem.application.port;

import com.fasterxml.jackson.databind.JsonNode;

public record OcrResult(
	String text,
	String latexStyled,
	Double confidence,
	String requestId,
	JsonNode rawJson
) {}
