package cmc.delta.domain.problem.model.scan;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.user.model.User;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanTest {

	@Test
	@DisplayName("uploaded 생성: 기본 status/renderMode/attemptCount가 초기화됨")
	void uploaded_defaults() {
		// given
		User user = User.createProvisioned("user@example.com", "delta");

		// when
		ProblemScan scan = ProblemScan.uploaded(user);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.UPLOADED);
		assertThat(scan.getOcrAttemptCount()).isEqualTo(0);
		assertThat(scan.getAiAttemptCount()).isEqualTo(0);
		assertThat(scan.isNeedsReview()).isFalse();
	}

	@Test
	@DisplayName("OCR 성공 처리: status=OCR_DONE, 실패/재시도 정보가 초기화되고 attemptCount가 0으로 리셋")
	void markOcrSucceeded_resetsFailureAndAttempts() {
		// given
		ProblemScan scan = ProblemScan.uploaded(User.createProvisioned("user@example.com", "delta"));
		scan.markOcrFailed("fail");
		scan.scheduleNextRetryForOcr(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
		assertThat(scan.getFailReason()).isNotNull();
		assertThat(scan.getNextRetryAt()).isNotNull();
		assertThat(scan.getOcrAttemptCount()).isEqualTo(1);

		// when
		scan.markOcrSucceeded("ocr", "{raw}", LocalDateTime.of(2026, 1, 1, 0, 0, 10));

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getOcrPlainText()).isEqualTo("ocr");
		assertThat(scan.getFailReason()).isNull();
		assertThat(scan.getNextRetryAt()).isNull();
		assertThat(scan.getOcrAttemptCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("OCR 재시도 스케줄: 최대 시도 횟수 미만이면 nextRetryAt 설정 + status=UPLOADED")
	void scheduleNextRetryForOcr_whenUnderMax_thenSchedules() {
		// given
		ProblemScan scan = ProblemScan.uploaded(User.createProvisioned("user@example.com", "delta"));
		scan.markOcrFailed("f");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

		// when
		scan.scheduleNextRetryForOcr(now, 10);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.UPLOADED);
		assertThat(scan.getNextRetryAt()).isEqualTo(now.plusSeconds(10));
	}

	@Test
	@DisplayName("OCR 재시도 스케줄: 최대 시도 횟수 이상이면 FAILED로 전이")
	void scheduleNextRetryForOcr_whenMax_thenTerminalFail() {
		// given
		ProblemScan scan = ProblemScan.uploaded(User.createProvisioned("user@example.com", "delta"));
		scan.markOcrFailed("f");
		scan.markOcrFailed("f");
		scan.markOcrFailed("f");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

		// when
		scan.scheduleNextRetryForOcr(now, 10);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getNextRetryAt()).isNull();
		assertThat(scan.getFailReason()).isEqualTo("f");
	}

	@Test
	@DisplayName("retryFailed: OCR 텍스트가 있으면 OCR_DONE으로 복구하고 aiAttemptCount를 0으로")
	void retryFailed_whenHasOcr_thenBackToOcrDone() {
		// given
		ProblemScan scan = ProblemScan.uploaded(User.createProvisioned("user@example.com", "delta"));
		scan.markOcrSucceeded("ocr", "{raw}", LocalDateTime.of(2026, 1, 1, 0, 0, 10));
		scan.markAiFailed("ai failed");
		setLockFields(scan);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 1, 0);

		// when
		scan.retryFailed(now);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getFailReason()).isNull();
		assertThat(scan.getNextRetryAt()).isEqualTo(now);
		assertThat(scan.getLockOwner()).isNull();
		assertThat(scan.getLockToken()).isNull();
		assertThat(scan.getLockedAt()).isNull();
		assertThat(scan.getAiAttemptCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("AI rate limit 스케줄: 최대 허용 횟수 이상이면 FAILED로 전이")
	void scheduleNextRetryForAiRateLimit_whenMax_thenTerminalFail() {
		// given
		ProblemScan scan = ProblemScan.uploaded(User.createProvisioned("user@example.com", "delta"));
		scan.markOcrSucceeded("ocr", "{raw}", LocalDateTime.of(2026, 1, 1, 0, 0, 10));

		for (int i = 0; i < 20; i++) {
			scan.markAiRateLimited("429");
		}
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

		// when
		scan.scheduleNextRetryForAi(now, 10);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getNextRetryAt()).isNull();
		assertThat(scan.getFailReason()).isEqualTo("429");
	}

	private void setLockFields(ProblemScan scan) {
		setField(scan, "lockOwner", "w1");
		setField(scan, "lockToken", "t1");
		setField(scan, "lockedAt", LocalDateTime.of(2026, 1, 1, 0, 0, 0));
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field f = target.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 필드 세팅 실패 field=" + fieldName, e);
		}
	}
}
