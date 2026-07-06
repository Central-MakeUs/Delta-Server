package cmc.delta.domain.report.application.service;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportNarrative;
import cmc.delta.domain.report.application.dto.ReportRequestResult;
import cmc.delta.domain.report.application.dto.ReportSourceSignature;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import cmc.delta.domain.report.application.exception.ReportException;
import cmc.delta.domain.report.application.port.in.ReportCommandUseCase;
import cmc.delta.domain.report.application.port.out.ReportRepositoryPort;
import cmc.delta.domain.report.application.port.out.ReportSourcePort;
import cmc.delta.domain.report.application.port.out.ai.ReportAiClient;
import cmc.delta.domain.report.application.support.ReportAggregator;
import cmc.delta.domain.report.application.support.ReportPayloadCodec;
import cmc.delta.domain.report.model.WrongAnswerReport;
import cmc.delta.global.error.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCommandService implements ReportCommandUseCase {

	private static final int MIN_PROBLEMS = 5;
	private static final int SAMPLE_LIMIT = 8;
	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String NULL_SENTINEL = "<null>";
	private static final String FALLBACK_FAILURE_REASON = "REPORT_GENERATION_FAILED";
	private static final int MAX_FAILURE_REASON_LENGTH = 255; // failure_reason 컬럼 길이. 초과 시 커밋 실패→롤백→재claim 무한루프.
	private static final ProblemStatsCondition ALL_PROBLEMS =
		new ProblemStatsCondition(null, null, null, ProblemStatsSort.DEFAULT);

	private final ReportRepositoryPort reportRepositoryPort;
	private final ReportSourcePort reportSourcePort;
	private final ProblemStatsQueryPort problemStatsQueryPort;
	private final ReportAiClient reportAiClient;
	private final ReportPayloadCodec payloadCodec;
	private final Clock clock;

	@Override
	@Transactional
	public ReportRequestResult requestMyReport(Long userId) {
		ReportSourceSignature signature = reportSourcePort.loadSignature(userId);
		if (signature.totalCount() < MIN_PROBLEMS) {
			throw ReportException.notEnoughProblems();
		}

		String inputHash = calculateInputHash(signature);
		Optional<WrongAnswerReport> latest = reportRepositoryPort.findLatestByUserId(userId);
		if (latest.isPresent() && latest.get().canReuseFor(inputHash)) {
			return toRequestResult(latest.get(), true);
		}

		WrongAnswerReport report = reportRepositoryPort.save(WrongAnswerReport.createPending(
			userId, inputHash, signature.totalCount(), LocalDateTime.now(clock)));
		return toRequestResult(report, false);
	}

	@Transactional
	public void processNextPendingReport() {
		reportRepositoryPort.findNextPendingForUpdate().ifPresent(this::generate);
	}

	private void generate(WrongAnswerReport report) {
		LocalDateTime startedAt = LocalDateTime.now(clock);
		report.markProcessing(startedAt);
		Long userId = report.getUserId();
		try {
			List<ProblemUnitStatsRow> unitStats = problemStatsQueryPort.findUnitStats(userId, ALL_PROBLEMS);
			List<ProblemTypeStatsRow> typeStats = problemStatsQueryPort.findTypeStats(userId, ALL_PROBLEMS);
			ReportAggregate aggregate = ReportAggregator.aggregate(unitStats, typeStats);

			List<WrongAnswerSample> samples = reportSourcePort.loadSamples(userId, SAMPLE_LIMIT);
			ReportNarrative narrative = reportAiClient.analyze(aggregate, samples);

			report.markReady(payloadCodec.write(aggregate), payloadCodec.write(narrative), LocalDateTime.now(clock));
			log.debug("취약점 리포트 생성 성공 reportId={} userId={}", report.getId(), userId);
		} catch (Exception exception) {
			report.markTerminalFailure(extractFailureReason(exception), LocalDateTime.now(clock));
			log.warn("취약점 리포트 생성 실패 reportId={} userId={} exceptionClass={} message={}",
				report.getId(), userId, exception.getClass().getSimpleName(), exception.getMessage());
		}
	}

	private String calculateInputHash(ReportSourceSignature signature) {
		String payload = String.join("|",
			String.valueOf(signature.totalCount()),
			String.valueOf(signature.solvedCount()),
			signature.lastUpdatedAt() == null ? NULL_SENTINEL : signature.lastUpdatedAt().toString());
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
		}
	}

	private String extractFailureReason(Exception exception) {
		if (exception instanceof BusinessException be && be.getErrorCode() != null) {
			return be.getErrorCode().code();
		}
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return FALLBACK_FAILURE_REASON;
		}
		return message.length() > MAX_FAILURE_REASON_LENGTH
			? message.substring(0, MAX_FAILURE_REASON_LENGTH)
			: message;
	}

	private ReportRequestResult toRequestResult(WrongAnswerReport report, boolean reused) {
		return new ReportRequestResult(report.getId(), report.getStatus().name(), reused, report.getRequestedAt());
	}
}
