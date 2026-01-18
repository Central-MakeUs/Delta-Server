package cmc.delta.domain.problem.api.problem.dto.response;

import java.time.LocalDateTime;

public record ProblemListItemResponse(
	Long problemId,
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,
	String previewText,
	LocalDateTime createdAt
) { }
