package cmc.delta.domain.problem.application.port.in.worker;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import java.time.LocalDateTime;

public interface AiScanPersistUseCase {
	void persistAiSucceeded(Long scanId, String lockOwner, String lockToken, AiCurriculumResult aiResult,
		LocalDateTime batchNow);

	void persistAiFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow);
}
