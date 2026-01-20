package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.CandidateResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.projection.ScanDetailProjection;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanDetailMapper;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.support.query.CandidateIdScore;
import cmc.delta.domain.problem.application.support.query.CandidateJsonParser;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.ProblemScanDetailValidator;
import cmc.delta.domain.problem.application.validation.query.ProblemScanQueryValidator;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.application.StorageService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemScanQueryServiceImpl implements ProblemScanQueryUseCase {

	private static final Comparator<CandidateIdScore> SCORE_DESC_NULLS_LAST =
		Comparator.comparing(CandidateIdScore::score, Comparator.nullsLast(Comparator.reverseOrder()));

	private final ScanQueryPort scanQueryPort;
	private final StorageService storageService;

	// detail
	private final ProblemScanDetailValidator detailValidator;
	private final UnitSubjectResolver subjectResolver;
	private final ProblemScanDetailMapper detailMapper;

	// summary
	private final ProblemScanQueryValidator summaryValidator;
	private final CandidateJsonParser candidateJsonParser;
	private final ProblemScanSummaryMapper summaryMapper;

	@Override
	public ProblemScanSummaryResponse getSummary(Long userId, Long scanId) {
		ScanListRow row = scanQueryPort.findListRow(userId, scanId)
			.orElseThrow(this::scanNotFound);

		summaryValidator.validateHasOriginalAsset(row);

		String viewUrl = presignedUrl(row.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(row.getUnitId());

		return summaryMapper.toSummaryResponse(row, viewUrl, subject);
	}

	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ScanDetailProjection p = scanQueryPort.findDetail(userId, scanId)
			.orElseThrow(this::scanNotFound);

		detailValidator.validateOriginalAsset(p);

		String viewUrl = presignedUrl(p.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(p.getPredictedUnitId());

		ProblemScanDetailResponse.AiClassification ai = detailMapper.toAiClassification(p, subject);
		return detailMapper.toDetailResponse(p, viewUrl, ai);
	}

	private String presignedUrl(String storageKey) {
		StoragePresignedGetData presigned = storageService.issueReadUrl(storageKey, null);
		return presigned.url();
	}

	private ProblemException scanNotFound() {
		return new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}

	private List<CandidateResponse> toTopCandidates(String json, int topN) {
		if (topN <= 0) {
			return Collections.emptyList();
		}
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}

		List<CandidateIdScore> parsed = candidateJsonParser.parse(json);
		if (parsed.isEmpty()) {
			return Collections.emptyList();
		}

		parsed.sort(SCORE_DESC_NULLS_LAST);

		int limit = Math.min(parsed.size(), topN);
		List<CandidateResponse> result = new ArrayList<>(limit);

		for (int i = 0; i < limit; i++) {
			CandidateIdScore c = parsed.get(i);
			result.add(new CandidateResponse(c.id(), null, c.score()));
		}
		return result;
	}
}
