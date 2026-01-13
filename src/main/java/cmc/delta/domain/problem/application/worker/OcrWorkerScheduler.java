package cmc.delta.domain.problem.application.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcrWorkerScheduler {

	private final OcrScanWorker ocrWorker;

	@Scheduled(fixedDelayString = "PT2S")
	public void tick() {
		ocrWorker.runOnce("ocr-worker-1");
	}
}
