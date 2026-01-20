package cmc.delta.domain.problem.adapter.in.web.problem.dto.response;

import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.time.LocalDateTime;

public record ProblemDetailResponse(
	Long problemId,

	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,

	OriginalImageResponse originalImage,

	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String solutionText,

	boolean completed,
	LocalDateTime completedAt,

	LocalDateTime createdAt
) {
	public record OriginalImageResponse(Long assetId, String viewUrl) {
	}
}
