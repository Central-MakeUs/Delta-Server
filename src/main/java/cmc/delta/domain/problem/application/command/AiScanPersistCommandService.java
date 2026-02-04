package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.AiScanPersister;
import cmc.delta.domain.problem.application.command.support.TypeCandidatesParser;
import cmc.delta.domain.problem.application.command.support.TypeCandidatesParser.TypeCandidate;
import cmc.delta.domain.problem.application.port.in.worker.AiScanPersistUseCase;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionWriter.TypePrediction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiScanPersistCommandService implements AiScanPersistUseCase {

	private static final int FIRST_RANK = 1;

	private final AiScanPersister persister;
	private final ScanTypePredictionWriter predictionWriter;
	private final TypeCandidatesParser typeCandidatesParser;

	@Override
	public void persistAiSucceeded(Long scanId, String lockOwner, String lockToken, AiCurriculumResult aiResult,
		LocalDateTime batchNow) {
		persister.persistAiSucceeded(scanId, lockOwner, lockToken, aiResult, batchNow);
		List<TypeCandidate> candidates = resolveTypeCandidates(aiResult);
		List<TypePrediction> rows = buildPredictions(candidates);
		predictionWriter.replacePredictions(scanId, rows);
	}

	@Override
	public void persistAiFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow) {
		persister.persistAiFailed(scanId, lockOwner, lockToken, decision, batchNow);
	}

	private List<TypeCandidate> resolveTypeCandidates(AiCurriculumResult aiResult) {
		List<TypeCandidate> candidates = typeCandidatesParser.parseTypeCandidates(aiResult.typeCandidatesJson());
		if (!candidates.isEmpty()) {
			return candidates;
		}
		return fallbackCandidates(aiResult);
	}

	private List<TypeCandidate> fallbackCandidates(AiCurriculumResult aiResult) {
		String predictedTypeId = aiResult.predictedTypeId();
		if (predictedTypeId == null || predictedTypeId.isBlank()) {
			return List.of();
		}
		return List.of(new TypeCandidate(predictedTypeId, BigDecimal.valueOf(aiResult.confidence())));
	}

	private List<TypePrediction> buildPredictions(List<TypeCandidate> candidates) {
		List<TypePrediction> rows = new ArrayList<>();
		for (int i = 0; i < candidates.size(); i++) {
			TypeCandidate candidate = candidates.get(i);
			rows.add(new TypePrediction(candidate.typeId(), i + FIRST_RANK, candidate.score()));
		}
		return rows;
	}
}
