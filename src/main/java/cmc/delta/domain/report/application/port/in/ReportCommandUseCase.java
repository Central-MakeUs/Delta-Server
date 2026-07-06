package cmc.delta.domain.report.application.port.in;

import cmc.delta.domain.report.application.dto.ReportRequestResult;

public interface ReportCommandUseCase {

	ReportRequestResult requestMyReport(Long userId);
}
