package cmc.delta.domain.report.application.service;

import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportDetailResult;
import cmc.delta.domain.report.application.dto.ReportNarrative;
import cmc.delta.domain.report.application.exception.ReportException;
import cmc.delta.domain.report.application.port.in.ReportQueryUseCase;
import cmc.delta.domain.report.application.port.out.ReportRepositoryPort;
import cmc.delta.domain.report.application.support.ReportPayloadCodec;
import cmc.delta.domain.report.model.WrongAnswerReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService implements ReportQueryUseCase {

	private final ReportRepositoryPort reportRepositoryPort;
	private final ReportPayloadCodec payloadCodec;

	@Override
	public ReportDetailResult getMyReport(Long userId, Long reportId) {
		WrongAnswerReport report = reportRepositoryPort.findByIdAndUserId(reportId, userId)
			.orElseThrow(ReportException::notFound);
		return toDetail(report);
	}

	@Override
	public ReportDetailResult getMyLatestReport(Long userId) {
		WrongAnswerReport report = reportRepositoryPort.findLatestByUserId(userId)
			.orElseThrow(ReportException::notFound);
		return toDetail(report);
	}

	private ReportDetailResult toDetail(WrongAnswerReport report) {
		ReportAggregate aggregate = payloadCodec.read(report.getAggregateJson(), ReportAggregate.class);
		ReportNarrative narrative = payloadCodec.read(report.getNarrativeJson(), ReportNarrative.class);
		return new ReportDetailResult(
			report.getId(),
			report.getStatus().name(),
			report.getProblemCount(),
			report.getCompletedAt(),
			aggregate,
			narrative);
	}
}
