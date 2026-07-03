package cmc.delta.domain.dashboard.application.dto;

import cmc.delta.domain.user.model.enums.UserRole;
import java.time.LocalDateTime;

public record DashboardProblemDetailRow(
	Long problemId,
	String problemName,
	String unitName,
	String problemType,
	long aiSolutionCount,
	long viewCount,
	LocalDateTime registeredAt,
	boolean wrongAnswerCompleted,
	UserRole userRole,
	String storageKey
) {
	public DashboardProblemDetailResponse toResponse(String viewUrl) {
		return new DashboardProblemDetailResponse(
			problemId,
			problemName,
			unitName,
			problemType,
			aiSolutionCount,
			viewCount,
			registeredAt,
			wrongAnswerCompleted,
			userRole,
			viewUrl);
	}
}
