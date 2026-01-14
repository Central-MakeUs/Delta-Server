package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrWorkerScheduler {

	private final Clock clock;
	private final ProblemScanJpaRepository scanRepository;
	private final OcrScanWorker ocrScanWorker;

	@Scheduled(fixedDelayString = "${worker.ocr.fixed-delay-ms:2000}")
	public void tick() {
		String traceId = UUID.randomUUID().toString().replace("-", "");
		MDC.put("traceId", traceId);

		try {
			LocalDateTime now = LocalDateTime.now(clock);
			long candidates = scanRepository.countOcrCandidates(now);

			if (candidates == 0) {
				log.debug("OCR 스케줄러 tick - 처리 대상 없음");
				return;
			}

			log.info("OCR 스케줄러 tick - 처리 대상={}건 (UPLOADED, 미락, 재시도시간 도래)", candidates);
			ocrScanWorker.runOnce("ocr-worker");
		} finally {
			MDC.remove("traceId");
		}
	}
}
