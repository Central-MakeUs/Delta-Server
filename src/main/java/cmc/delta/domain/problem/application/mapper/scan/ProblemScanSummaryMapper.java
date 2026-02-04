package cmc.delta.domain.problem.application.mapper.scan;

import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryClassificationResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.mapper.support.ProblemCurriculumItemSupport;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanSummaryMapper implements ProblemCurriculumItemSupport {

	public ProblemScanSummaryResponse toSummaryResponse(
		ScanListRow row,
		String viewUrl,
		SubjectInfo subject,
		List<CurriculumItemResponse> types) {
		return new ProblemScanSummaryResponse(
			row.getScanId(),
			row.getStatus(),
			toOriginalImage(row, viewUrl),
			toClassification(row, subject, types));
	}

	private ProblemScanSummaryResponse.OriginalImage toOriginalImage(ScanListRow row, String viewUrl) {
		return new ProblemScanSummaryResponse.OriginalImage(row.getAssetId(), viewUrl);
	}

	private ProblemScanSummaryClassificationResponse toClassification(
		ScanListRow row,
		SubjectInfo subject,
		List<CurriculumItemResponse> types) {
		CurriculumItemResponse subjectItem = toItem(subject.subjectId(), subject.subjectName());
		CurriculumItemResponse unitItem = toItem(row.getUnitId(), row.getUnitName());
		boolean needsReview = Boolean.TRUE.equals(row.getNeedsReview());
		return new ProblemScanSummaryClassificationResponse(subjectItem, unitItem, types, needsReview);
	}
}
