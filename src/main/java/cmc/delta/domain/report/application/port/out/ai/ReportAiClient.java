package cmc.delta.domain.report.application.port.out.ai;

import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportNarrative;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import java.util.List;

public interface ReportAiClient {

	ReportNarrative analyze(ReportAggregate aggregate, List<WrongAnswerSample> samples);
}
