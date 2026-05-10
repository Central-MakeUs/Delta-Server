package cmc.delta.domain.dashboard.application.dto;

import java.util.List;

public record DashboardMonthlyAccessResponse(int year, int month, List<DashboardDailyAccessItem> dailyAccess) {
}
