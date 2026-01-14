package cmc.delta.domain.problem.application.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrWorkerScheduler {

	private final OcrScanWorker ocrWorker;

	@Scheduled(fixedDelayString = "PT5S")
	public void tick() {
		log.info("[OCR_SCHED] tick");
		ocrWorker.runOnce("ocr-worker-1");
	}
}
