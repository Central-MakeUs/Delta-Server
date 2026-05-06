package cmc.delta.domain.dashboard.adapter.in.web.dto.request;

import java.time.YearMonth;

public record DashboardMonthlyAccessRequest(Integer year, Integer month) {

	public DashboardMonthlyAccessRequest {
		YearMonth now = YearMonth.now();
		if (year == null)
			year = now.getYear();
		if (month == null)
			month = now.getMonthValue();
	}

	public YearMonth toYearMonth() {
		return YearMonth.of(year, month);
	}
}
