package cmc.delta.domain.dashboard.application.dto;

import java.time.LocalDate;

public record DashboardDailyAccessItem(LocalDate date, long count) {
}
