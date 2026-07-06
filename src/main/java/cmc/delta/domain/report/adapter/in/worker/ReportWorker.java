package cmc.delta.domain.report.adapter.in.worker;

import cmc.delta.domain.report.application.service.ReportCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportWorker {

	private final ReportCommandService commandService;

	public void runBatch(int batchSize) {
		for (int index = 0; index < batchSize; index++) {
			try {
				commandService.processNextPendingReport();
			} catch (Exception exception) {
				log.warn("취약점 리포트 워커 처리 실패 batchIndex={} message={}", index, exception.getMessage());
			}
		}
	}
}
