package cmc.delta.domain.dashboard.application.dto;

import cmc.delta.domain.user.model.enums.UserRole;
import java.time.LocalDateTime;

public record DashboardProblemItem(
	Long problemId,
	String problemName,
	String unitName,
	String problemType,
	long aiSolutionCount,
	long viewCount,
	LocalDateTime registeredAt,
	boolean wrongAnswerCompleted,
	UserRole userRole
) {
}
