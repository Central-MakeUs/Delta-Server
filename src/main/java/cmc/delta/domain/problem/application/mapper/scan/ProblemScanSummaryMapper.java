package cmc.delta.domain.problem.application.mapper.scan;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryClassificationResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;

import org.springframework.stereotype.Component;

@Component
public class ProblemScanSummaryMapper {

	public ProblemScanSummaryResponse toSummaryResponse(
		ScanListRow row,
		String viewUrl,
		SubjectInfo subject
	) {
		return new ProblemScanSummaryResponse(
			row.getScanId(),
			row.getStatus(),
			toOriginalImage(row, viewUrl),
			toClassification(row, subject)
		);
	}

	private ProblemScanSummaryResponse.OriginalImage toOriginalImage(ScanListRow row, String viewUrl) {
		return new ProblemScanSummaryResponse.OriginalImage(row.getAssetId(), viewUrl);
	}

	private ProblemScanSummaryClassificationResponse toClassification(ScanListRow row, SubjectInfo subject) {
		CurriculumItemResponse subjectItem = toItem(subject.subjectId(), subject.subjectName());
		CurriculumItemResponse unitItem = toItem(row.getUnitId(), row.getUnitName());
		CurriculumItemResponse typeItem = toItem(row.getTypeId(), row.getTypeName());

		boolean needsReview = Boolean.TRUE.equals(row.getNeedsReview());

		return new ProblemScanSummaryClassificationResponse(subjectItem, unitItem, typeItem, needsReview);
	}

	private CurriculumItemResponse toItem(String id, String name) {
		if (id == null) return null;
		return new CurriculumItemResponse(id, name);
	}
}
