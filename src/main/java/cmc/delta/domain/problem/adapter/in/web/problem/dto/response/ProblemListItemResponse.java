package cmc.delta.domain.problem.adapter.in.web.problem.dto.response;

import java.time.LocalDateTime;
import java.util.List;

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
