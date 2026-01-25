package cmc.delta.domain.problem.application.port.in.problem.result;

import java.time.LocalDateTime;
import java.util.List;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;

public record ProblemListItemResponse(
	Long problemId,
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	PreviewImageResponse previewImage,
	LocalDateTime createdAt
) {
	public record PreviewImageResponse(
		Long assetId,
		String viewUrl
	) { }
}
