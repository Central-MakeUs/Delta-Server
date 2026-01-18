package cmc.delta.domain.problem.api.problem.dto.response;

import java.time.LocalDateTime;

public record ProblemListItemResponse(
	Long problemId,
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,
	PreviewImageResponse previewImage,
	LocalDateTime createdAt
) {
	public record PreviewImageResponse(
		Long assetId,
		String viewUrl
	) { }
}
