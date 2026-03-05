package cmc.delta.domain.problem.adapter.in.worker;

import cmc.delta.domain.problem.application.service.command.ProblemAiSolutionCommandServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemAiSolutionWorker {

	private final ProblemAiSolutionCommandServiceImpl commandService;

	public void runBatch(int batchSize) {
		for (int index = 0; index < batchSize; index++) {
			try {
				commandService.processNextPendingTask();
			} catch (Exception exception) {
				log.warn("AI 풀이 워커 처리 실패 batchIndex={} message={}", index, exception.getMessage());
			}
		}
	}
}
