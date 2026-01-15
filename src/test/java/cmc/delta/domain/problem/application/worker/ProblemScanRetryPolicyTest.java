package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanRetryPolicyTest {

	@Test
	@DisplayName("ProblemScan은 OCR 실패가 3회 누적되면 FAILED로 전환한다.")
	void ocrRetry3_failsTerminal() {
		// given
		ProblemScan scan = uploaded(user(1L));

		// when
		scan.markOcrFailed("OCR_NETWORK_ERROR");
		scan.scheduleNextRetryForOcr(LocalDateTime.now());

		scan.markOcrFailed("OCR_NETWORK_ERROR");
		scan.scheduleNextRetryForOcr(LocalDateTime.now());

		scan.markOcrFailed("OCR_NETWORK_ERROR");
		scan.scheduleNextRetryForOcr(LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("ProblemScan은 AI 실패가 3회 누적되면 FAILED로 전환한다.")
	void aiRetry3_failsTerminal() {
		// given
		ProblemScan scan = ocrDone(user(1L), "text");

		// when
		scan.markAiFailed("AI_NETWORK_ERROR");
		scan.scheduleNextRetryForAi(LocalDateTime.now());

		scan.markAiFailed("AI_NETWORK_ERROR");
		scan.scheduleNextRetryForAi(LocalDateTime.now());

		scan.markAiFailed("AI_NETWORK_ERROR");
		scan.scheduleNextRetryForAi(LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("ProblemScan은 429가 20회 누적되면 FAILED로 전환한다.")
	void rateLimit20_failsTerminal() {
		// given
		ProblemScan scan = ocrDone(user(1L), "text");

		// 미리 19회 누적
		for (int i = 0; i < 19; i++) {
			scan.markAiRateLimited("AI_RATE_LIMIT");
		}

		// when: 20번째
		scan.markAiRateLimited("AI_RATE_LIMIT");
		scan.scheduleNextRetryForAi(LocalDateTime.now(), 60);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getNextRetryAt()).isNull();
	}
}
