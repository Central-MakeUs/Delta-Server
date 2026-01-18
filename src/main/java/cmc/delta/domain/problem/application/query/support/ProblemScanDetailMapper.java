package cmc.delta.domain.problem.application.query.support;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.persistence.scan.query.projection.ScanDetailProjection;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanDetailMapper {

	public record SubjectInfo(String subjectId, String subjectName) {
		public static SubjectInfo empty() { return new SubjectInfo(null, null); }
	}

	public ProblemScanDetailResponse toDetailResponse(
		ScanDetailProjection p,
		String viewUrl,
		ProblemScanDetailResponse.AiClassification ai
	) {
		return new ProblemScanDetailResponse(
			p.getScanId(),
			p.getStatus().name(),
			Boolean.TRUE.equals(p.getHasFigure()),
			p.getRenderMode().name(),
			new ProblemScanDetailResponse.OriginalImage(
				p.getAssetId(),
				viewUrl,
				p.getWidth(),
				p.getHeight()
			),
			p.getOcrPlainText(),
			p.getAiProblemLatex(),
			p.getAiSolutionLatex(),
			ai,
			p.getCreatedAt(),
			p.getOcrCompletedAt(),
			p.getAiCompletedAt(),
			p.getFailReason()
		);
	}

	public ProblemScanDetailResponse.AiClassification toAiClassification(
		ScanDetailProjection p,
		SubjectInfo subject
	) {
		return new ProblemScanDetailResponse.AiClassification(
			subject.subjectId(),
			subject.subjectName(),
			p.getPredictedUnitId(),
			p.getPredictedUnitName(),
			p.getPredictedTypeId(),
			p.getPredictedTypeName(),
			p.getConfidence(),
			p.getNeedsReview(),
			p.getAiUnitCandidatesJson(),
			p.getAiTypeCandidatesJson(),
			p.getAiDraftJson()
		);
	}
}
