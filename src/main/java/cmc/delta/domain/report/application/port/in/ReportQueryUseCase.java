package cmc.delta.domain.report.application.port.in;

import cmc.delta.domain.report.application.dto.ReportDetailResult;

public interface ReportQueryUseCase {

	ReportDetailResult getMyReport(Long userId, Long reportId);

	ReportDetailResult getMyLatestReport(Long userId);
}
