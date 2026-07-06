package cmc.delta.domain.report.application.port.out;

import cmc.delta.domain.report.model.WrongAnswerReport;
import java.util.Optional;

public interface ReportRepositoryPort {

	WrongAnswerReport save(WrongAnswerReport report);

	Optional<WrongAnswerReport> findByIdAndUserId(Long id, Long userId);

	Optional<WrongAnswerReport> findLatestByUserId(Long userId);

	Optional<WrongAnswerReport> findNextPendingForUpdate();
}
