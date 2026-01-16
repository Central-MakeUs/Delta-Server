package cmc.delta.domain.problem.application.query.mapper;

import cmc.delta.domain.problem.api.scan.dto.response.CandidateResponse;
import cmc.delta.domain.problem.api.scan.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryClassificationResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemScanSummaryMapper {

	@Mapping(target = "scanId", source = "row.scanId")
	@Mapping(target = "status", source = "row.status")
	@Mapping(target = "hasFigure", source = "row.hasFigure")
	@Mapping(target = "renderMode", source = "row.renderMode")
	@Mapping(target = "originalImage", expression = "java(toOriginalImage(row, viewUrl))")
	@Mapping(target = "classification", expression = "java(toClassification(row, unitCandidates, typeCandidates))")
	@Mapping(target = "createdAt", source = "row.createdAt")
	@Mapping(target = "aiCompletedAt", source = "row.aiCompletedAt")
	ProblemScanSummaryResponse toResponse(
		ProblemScanSummaryRow row,
		String viewUrl,
		List<CandidateResponse> unitCandidates,
		List<CandidateResponse> typeCandidates
	);

	default ProblemScanSummaryResponse.OriginalImageResponse toOriginalImage(ProblemScanSummaryRow row, String viewUrl) {
		return new ProblemScanSummaryResponse.OriginalImageResponse(
			row.getAssetId(),
			viewUrl,
			row.getWidth(),
			row.getHeight()
		);
	}

	default ProblemScanSummaryClassificationResponse toClassification(
		ProblemScanSummaryRow row,
		List<CandidateResponse> unitCandidates,
		List<CandidateResponse> typeCandidates
	) {
		CurriculumItemResponse subject = toItem(row.getSubjectId(), row.getSubjectName());
		CurriculumItemResponse unit = toItem(row.getUnitId(), row.getUnitName());
		CurriculumItemResponse type = toItem(row.getTypeId(), row.getTypeName());

		boolean needsReview = row.getNeedsReview() != null && row.getNeedsReview().booleanValue();

		return new ProblemScanSummaryClassificationResponse(
			subject,
			unit,
			type,
			row.getConfidence(),
			needsReview,
			unitCandidates,
			typeCandidates
		);
	}

	default CurriculumItemResponse toItem(String id, String name) {
		if (id == null) {
			return null;
		}
		return new CurriculumItemResponse(id, name);
	}
}
