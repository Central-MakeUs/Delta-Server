package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.OcrScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.LockOwnerProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcrWorkerScheduler {

	private final OcrScanWorker ocrScanWorker;
	private final LockOwnerProvider lockOwnerProvider;
	private final OcrWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.ocr.fixed-delay-ms:2000}")
	public void tick() {
		runBatch();
	}

	private void runBatch() {
		ocrScanWorker.runBatch(
			lockOwnerProvider.get(),
			props.batchSize(),
			props.lockLeaseSeconds());
	}
}
