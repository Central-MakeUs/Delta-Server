package cmc.delta.domain.problem.application.query.mapper;

import cmc.delta.domain.problem.api.scan.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryClassificationResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.query.support.ProblemScanDetailMapper;
import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanSummaryMapper {

	public ProblemScanSummaryResponse toSummaryResponse(
		ProblemScanSummaryRow row,
		String viewUrl,
		ProblemScanDetailMapper.SubjectInfo subject
	) {
		ProblemScanSummaryResponse.OriginalImage originalImage =
			new ProblemScanSummaryResponse.OriginalImage(row.getAssetId(), viewUrl);

		CurriculumItemResponse subjectItem =
			new CurriculumItemResponse(subject.subjectId(), subject.subjectName());

		CurriculumItemResponse unitItem =
			row.getUnitId() == null ? null : new CurriculumItemResponse(row.getUnitId(), row.getUnitName());

		CurriculumItemResponse typeItem =
			row.getTypeId() == null ? null : new CurriculumItemResponse(row.getTypeId(), row.getTypeName());

		boolean needsReview = row.getNeedsReview() != null && row.getNeedsReview().booleanValue();

		ProblemScanSummaryClassificationResponse classification =
			new ProblemScanSummaryClassificationResponse(subjectItem, unitItem, typeItem, needsReview);

		return new ProblemScanSummaryResponse(
			row.getScanId(),
			row.getStatus(),
			originalImage,
			classification
		);
	}
}
