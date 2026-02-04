package cmc.delta.domain.problem.application.mapper.scan;

import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse.PredictedTypeResponse;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanDetailMapper {

	public ProblemScanDetailResponse toDetailResponse(
		ScanDetailProjection p,
		String viewUrl,
		ProblemScanDetailResponse.AiClassification ai) {
		return new ProblemScanDetailResponse(
			p.getScanId(),
			enumName(p.getStatus()),
			Boolean.TRUE.equals(p.getHasFigure()),
			enumName(p.getRenderMode()),
			toOriginalImage(p, viewUrl),
			p.getOcrPlainText(),
			p.getAiProblemLatex(),
			p.getAiSolutionLatex(),
			ai,
			p.getCreatedAt(),
			p.getOcrCompletedAt(),
			p.getAiCompletedAt(),
			p.getFailReason());
	}

	public ProblemScanDetailResponse.AiClassification toAiClassification(
		ScanDetailProjection p,
		SubjectInfo subject,
		List<PredictedTypeResponse> predictedTypes) {
		return new ProblemScanDetailResponse.AiClassification(
			subject.subjectId(),
			subject.subjectName(),
			p.getPredictedUnitId(),
			p.getPredictedUnitName(),
			p.getPredictedTypeId(),
			p.getPredictedTypeName(),
			p.getConfidence(),
			p.getNeedsReview(),
			resolvePredictedTypes(predictedTypes),
			p.getAiUnitCandidatesJson(),
			p.getAiTypeCandidatesJson(),
			p.getAiDraftJson());
	}

	private ProblemScanDetailResponse.OriginalImage toOriginalImage(ScanDetailProjection p, String viewUrl) {
		return new ProblemScanDetailResponse.OriginalImage(
			p.getAssetId(),
			viewUrl,
			p.getWidth(),
			p.getHeight());
	}

	private String enumName(Enum<?> value) {
		return value == null ? null : value.name();
	}

	private List<PredictedTypeResponse> resolvePredictedTypes(List<PredictedTypeResponse> predictedTypes) {
		return predictedTypes == null ? List.of() : predictedTypes;
	}
}
