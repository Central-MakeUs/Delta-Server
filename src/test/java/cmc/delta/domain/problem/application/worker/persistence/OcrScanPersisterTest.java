package cmc.delta.domain.problem.application.worker.persistence;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.application.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class OcrScanPersisterTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ScanRepository scanRepo;
	private TransactionTemplate tx;
	private OcrScanPersister sut;

	@BeforeEach
	void setUp() {
		scanRepo = mock(ScanRepository.class);
		tx = WorkerTestTx.immediateTx();

		sut = new OcrScanPersister(tx, scanRepo);
	}

	@Test
	@DisplayName("persistOcrSucceeded: 락이 없으면 아무 것도 저장하지 않는다")
	void succeed_lockLost_noop() {
		// given
		Long scanId = 1L;
		when(scanRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(null);

		// when
		sut.persistOcrSucceeded(scanId, OWNER, TOKEN, mock(OcrResult.class), LocalDateTime.now());

		// then
		verify(scanRepo, never()).findById(anyLong());
	}

	@Test
	@DisplayName("persistOcrSucceeded: scan이 없으면 아무 것도 저장하지 않는다")
	void succeed_scanMissing_noop() {
		// given
		Long scanId = 1L;
		when(scanRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.empty());

		// when
		sut.persistOcrSucceeded(scanId, OWNER, TOKEN, mock(OcrResult.class), LocalDateTime.now());

		// then
		// 예외 없이 종료되는지만 보면 됨 (상태변경 대상이 없음)
		verify(scanRepo).findById(scanId);
	}

	@Test
	@DisplayName("persistOcrSucceeded: 성공 저장 시 OCR_DONE으로 전환하고 결과/정리 필드를 세팅한다")
	void succeed_marksOcrDone() {
		// given
		Long scanId = 1L;
		ProblemScan scan = uploaded(user(10L));

		when(scanRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		OcrResult r = ocrResult("plain", "{\"ok\":true}");
		LocalDateTime completedAt = LocalDateTime.now();

		// when
		sut.persistOcrSucceeded(scanId, OWNER, TOKEN, r, completedAt);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getOcrPlainText()).isEqualTo("plain");
		assertThat(scan.getOcrRawJson()).isEqualTo("{\"ok\":true}");
		assertThat(scan.getFailReason()).isNull();
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("persistOcrFailed: retryable=false면 즉시 FAILED로 전환하고 next_retry_at을 비운다")
	void failed_notRetryable_marksFailed() {
		// given
		Long scanId = 1L;
		ProblemScan scan = uploaded(user(10L));

		when(scanRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(false);

		FailureReason reason = mock(FailureReason.class);
		when(reason.code()).thenReturn("OCR_CLIENT_4XX");
		when(decision.reasonCode()).thenReturn(reason);

		// when
		sut.persistOcrFailed(scanId, OWNER, TOKEN, decision, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("OCR_CLIENT_4XX");
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("persistOcrFailed: retryable=true면 attempt를 올리고 next_retry_at을 잡아 재시도를 예약한다")
	void failed_retryable_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = uploaded(user(10L));

		when(scanRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(true);

		FailureReason reason = mock(FailureReason.class);
		when(reason.code()).thenReturn("OCR_NETWORK");
		when(decision.reasonCode()).thenReturn(reason);

		LocalDateTime now = LocalDateTime.now();

		// when
		sut.persistOcrFailed(scanId, OWNER, TOKEN, decision, now);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.UPLOADED); // scheduleNextRetryForOcr가 UPLOADED로 되돌리는 구조 기준
		assertThat(scan.getOcrAttemptCount()).isEqualTo(1);
		assertThat(scan.getFailReason()).isEqualTo("OCR_NETWORK");
		assertThat(scan.getNextRetryAt()).isNotNull();
	}
}
