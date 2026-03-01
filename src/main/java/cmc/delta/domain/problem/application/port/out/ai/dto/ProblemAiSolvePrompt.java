package cmc.delta.domain.problem.application.port.out.ai.dto;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemAiSolvePrompt(
	byte[] problemImageBytes,
	String problemImageMimeType,
	AnswerFormat answerFormat,
	String answerValue,
	Integer answerChoiceNo) {
}
