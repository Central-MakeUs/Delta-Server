package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.LocalDateTime;

public interface ProblemScanDetailProjection {

	Long getScanId();
	ScanStatus getStatus();
	Boolean getHasFigure();
	RenderMode getRenderMode();

	// ORIGINAL asset (없을 수도 있으니 nullable)
	Long getAssetId();
	String getStorageKey();
	Integer getWidth();
	Integer getHeight();

	String getOcrPlainText();
	String getAiProblemLatex();
	String getAiSolutionLatex();

	LocalDateTime getCreatedAt();
	LocalDateTime getOcrCompletedAt();
	LocalDateTime getAiCompletedAt();
	String getFailReason();
}
