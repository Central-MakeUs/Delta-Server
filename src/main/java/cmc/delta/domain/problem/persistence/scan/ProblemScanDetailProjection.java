package cmc.delta.domain.problem.persistence.scan;

import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.LocalDateTime;

public interface ProblemScanDetailProjection {

	Long getScanId();
	ScanStatus getStatus();
	Boolean getHasFigure();
	RenderMode getRenderMode();

	Long getAssetId();
	String getStorageKey();
	Integer getWidth();
	Integer getHeight();

	String getOcrPlainText();
	String getAiProblemLatex();
	String getAiSolutionLatex();

	String getPredictedUnitId();
	String getPredictedUnitName();
	String getPredictedTypeId();
	String getPredictedTypeName();
	Double getConfidence();
	Boolean getNeedsReview();

	String getAiUnitCandidatesJson();
	String getAiTypeCandidatesJson();
	String getAiDraftJson();

	LocalDateTime getCreatedAt();
	LocalDateTime getOcrCompletedAt();
	LocalDateTime getAiCompletedAt();
	String getFailReason();
}
