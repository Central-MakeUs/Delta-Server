package cmc.delta.domain.problem.application.port.in.problem.result;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import java.time.LocalDateTime;
import java.util.List;

public record ProblemListItemResponse(
	Long problemId,
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	PreviewImageResponse previewImage,
	LocalDateTime createdAt) {
	public record PreviewImageResponse(
		Long assetId,
		String viewUrl) {
	}
}
