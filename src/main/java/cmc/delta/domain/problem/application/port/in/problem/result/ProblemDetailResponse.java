package cmc.delta.domain.problem.application.port.in.problem.result;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.time.LocalDateTime;
import java.util.List;

public record ProblemDetailResponse(
	Long problemId,

	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	OriginalImageResponse originalImage,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String memoText,

	boolean completed,
	LocalDateTime completedAt,

	LocalDateTime createdAt) {
	public record OriginalImageResponse(Long assetId, String viewUrl) {
	}
}
