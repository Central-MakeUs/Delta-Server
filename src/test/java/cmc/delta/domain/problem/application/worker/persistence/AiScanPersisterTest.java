package cmc.delta.domain.problem.application.worker.persistence;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class AiScanPersisterTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ScanRepository scanRepo;
	private ScanWorkRepository scanWorkRepo;

	private UnitJpaRepository unitRepo;
	private ProblemTypeJpaRepository typeRepo;
	private TransactionTemplate tx;

	private AiScanPersister sut;

	@BeforeEach
	void setUp() {
		scanRepo = mock(ScanRepository.class);
		scanWorkRepo = mock(ScanWorkRepository.class);

		unitRepo = mock(UnitJpaRepository.class);
		typeRepo = mock(ProblemTypeJpaRepository.class);

		tx = WorkerTestTx.immediateTx();

		sut = new AiScanPersister(tx, scanWorkRepo, scanRepo, unitRepo, typeRepo);
	}

	@Test
	@DisplayName("persistAiSucceeded: 락이 없으면 아무 것도 저장하지 않는다")
	void succeed_lockLost_noop() {
		// given
		Long scanId = 1L;
		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(null);

		// when
		sut.persistAiSucceeded(scanId, OWNER, TOKEN, mock(AiCurriculumResult.class), LocalDateTime.now());

		// then
		verify(scanRepo, never()).findById(anyLong());
		verifyNoInteractions(unitRepo, typeRepo);
	}

	@Test
	@DisplayName("persistAiSucceeded: unit/type이 존재하고 confidence>=0.60이면 needs_review=false로 AI_DONE 저장")
	void succeed_highConf_marksAiDone_noReview() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		AiCurriculumResult ai = aiResult("U1", "T1", 0.90);

		Unit u = unit("U1");
		ProblemType t = type("T1");
		when(unitRepo.findById("U1")).thenReturn(Optional.of(u));
		when(typeRepo.findById("T1")).thenReturn(Optional.of(t));

		// when
		sut.persistAiSucceeded(scanId, OWNER, TOKEN, ai, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.AI_DONE);
		assertThat(scan.isNeedsReview()).isFalse();
		assertThat(scan.getFailReason()).isNull();
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("persistAiSucceeded: confidence<0.60이면 needs_review=true로 저장")
	void succeed_lowConf_marksNeedsReview() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		AiCurriculumResult ai = aiResult("U1", "T1", 0.59);

		when(unitRepo.findById("U1")).thenReturn(Optional.of(mock(Unit.class)));
		when(typeRepo.findById("T1")).thenReturn(Optional.of(mock(ProblemType.class)));

		// when
		sut.persistAiSucceeded(scanId, OWNER, TOKEN, ai, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.AI_DONE);
		assertThat(scan.isNeedsReview()).isTrue();
	}

	@Test
	@DisplayName("persistAiSucceeded: unit/type이 없으면 needs_review=true로 저장")
	void succeed_missingUnitOrType_marksNeedsReview() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		AiCurriculumResult ai = aiResult("U_UNKNOWN", "T_UNKNOWN", 0.90);

		when(unitRepo.findById("U_UNKNOWN")).thenReturn(Optional.empty());
		when(typeRepo.findById("T_UNKNOWN")).thenReturn(Optional.empty());

		// when
		sut.persistAiSucceeded(scanId, OWNER, TOKEN, ai, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.AI_DONE);
		assertThat(scan.isNeedsReview()).isTrue();
	}

	@Test
	@DisplayName("persistAiFailed: retryable=false면 즉시 FAILED로 전환한다")
	void failed_notRetryable_marksFailed() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(false);

		FailureReason reason = mock(FailureReason.class);
		when(reason.code()).thenReturn("AI_CLIENT_4XX");
		when(decision.reasonCode()).thenReturn(reason);

		// when
		sut.persistAiFailed(scanId, OWNER, TOKEN, decision, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("AI_CLIENT_4XX");
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("persistAiFailed: 429(AI_RATE_LIMIT) + Retry-After가 있으면 그 값으로 next_retry_at을 잡는다")
	void failed_rateLimit_usesRetryAfter() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(true);
		when(decision.reasonCode()).thenReturn(FailureReason.AI_RATE_LIMIT);
		when(decision.retryAfterSeconds()).thenReturn(10L);

		LocalDateTime now = LocalDateTime.now();

		// when
		sut.persistAiFailed(scanId, OWNER, TOKEN, decision, now);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getAiAttemptCount()).isEqualTo(1);
		assertThat(scan.getFailReason()).isEqualTo(FailureReason.AI_RATE_LIMIT.code());
		assertThat(scan.getNextRetryAt()).isNotNull();
	}

	@Test
	@DisplayName("persistAiFailed: 429(AI_RATE_LIMIT)인데 Retry-After가 없으면 기본 180초로 재시도를 잡는다")
	void failed_rateLimit_fallback180() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(true);
		when(decision.reasonCode()).thenReturn(FailureReason.AI_RATE_LIMIT);
		when(decision.retryAfterSeconds()).thenReturn(null);

		// when
		sut.persistAiFailed(scanId, OWNER, TOKEN, decision, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getAiAttemptCount()).isEqualTo(1);
		assertThat(scan.getFailReason()).isEqualTo(FailureReason.AI_RATE_LIMIT.code());
		assertThat(scan.getNextRetryAt()).isNotNull();
	}

	@Test
	@DisplayName("persistAiFailed: retryable=true + 일반 실패면 attempt를 올리고 scheduleNextRetryForAi(now)를 탄다")
	void failed_retryable_general_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = ocrDone(user(10L), "some text");

		when(scanWorkRepo.existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
		when(scanRepo.findById(scanId)).thenReturn(Optional.of(scan));

		FailureDecision decision = mock(FailureDecision.class);
		when(decision.retryable()).thenReturn(true);

		FailureReason reason = mock(FailureReason.class);
		when(reason.code()).thenReturn("AI_NETWORK");
		when(decision.reasonCode()).thenReturn(reason);

		// when
		sut.persistAiFailed(scanId, OWNER, TOKEN, decision, LocalDateTime.now());

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getAiAttemptCount()).isEqualTo(1);
		assertThat(scan.getFailReason()).isEqualTo("AI_NETWORK");
		assertThat(scan.getNextRetryAt()).isNotNull();
	}
}
