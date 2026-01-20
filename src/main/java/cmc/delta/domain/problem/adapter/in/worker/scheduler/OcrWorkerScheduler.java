package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.OcrScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.support.LockOwnerProvider;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrWorkerScheduler {

	private final OcrScanWorker ocrScanWorker;
	private final LockOwnerProvider lockOwnerProvider;
	private final OcrWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.ocr.fixed-delay-ms:2000}")
	public void tick() {
		ocrScanWorker.runBatch(
			lockOwnerProvider.get(),
			props.batchSize(),
			props.lockLeaseSeconds()
		);
	}
}
