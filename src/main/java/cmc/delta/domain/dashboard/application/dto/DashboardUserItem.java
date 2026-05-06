package cmc.delta.domain.dashboard.application.dto;

import cmc.delta.domain.user.model.enums.UserRole;
import java.time.LocalDate;

public record DashboardUserItem(
	Long userId,
	String nickname,
	UserRole userRole,
	long accessCount,
	LocalDate lastAccessDate,
	long problemCount
) {
}
