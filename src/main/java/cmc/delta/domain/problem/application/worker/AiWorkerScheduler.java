package cmc.delta.domain.problem.application.worker;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWorkerScheduler {

	private final AiScanWorker aiScanWorker;

	@Scheduled(fixedDelayString = "${worker.ai.fixed-delay-ms:2000}")
	public void tick() {
		log.debug("AI 스케줄러 tick");
		aiScanWorker.runOnce("ai-" + UUID.randomUUID());
	}
}
