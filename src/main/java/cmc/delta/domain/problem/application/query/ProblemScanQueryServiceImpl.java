package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.scan.dto.response.CandidateResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.query.mapper.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.query.support.CandidateIdScore;
import cmc.delta.domain.problem.application.query.support.CandidateJsonParser;
import cmc.delta.domain.problem.application.query.support.ProblemScanDetailMapper;
import cmc.delta.domain.problem.application.query.support.ProblemScanDetailValidator;
import cmc.delta.domain.problem.application.query.support.UnitSubjectResolver;
import cmc.delta.domain.problem.application.query.validation.ProblemScanQueryValidator;
import cmc.delta.domain.problem.persistence.scan.query.projection.ScanDetailProjection;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.persistence.scan.query.dto.ScanListRow;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
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
public class ProblemScanQueryServiceImpl implements ProblemScanQueryService {

	private final ScanRepository scanRepository;
	private final StorageService storageService;

	// detail 전용
	private final ProblemScanDetailValidator detailValidator;
	private final UnitSubjectResolver subjectResolver;
	private final ProblemScanDetailMapper detailMapper;

	// summary 전용
	private final ProblemScanQueryValidator summaryValidator;
	private final CandidateJsonParser candidateJsonParser;
	private final ProblemScanSummaryMapper summaryMapper;

	@Override
	@Transactional(readOnly = true)
	public ProblemScanSummaryResponse getSummary(Long userId, Long scanId) {
		ScanListRow row = scanRepository.findSummaryRow(userId, scanId).orElse(null);
		if (row == null) {
			throw new ProblemScanNotFoundException();
		}

		summaryValidator.validateHasOriginalAsset(row);

		StoragePresignedGetData presigned = storageService.issueReadUrl(row.getStorageKey(), null);

		ProblemScanDetailMapper.SubjectInfo subject = subjectResolver.resolveByUnitId(row.getUnitId());

		return summaryMapper.toSummaryResponse(row, presigned.url(), subject);
	}


	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ScanDetailProjection p = detailValidator.getOwnedDetail(scanRepository, scanId, userId);
		detailValidator.validateOriginalAsset(p);

		StoragePresignedGetData presigned = storageService.issueReadUrl(p.getStorageKey(), null);
		ProblemScanDetailMapper.SubjectInfo subject = subjectResolver.resolveByUnitId(p.getPredictedUnitId());

		ProblemScanDetailResponse.AiClassification ai = detailMapper.toAiClassification(p, subject);
		return detailMapper.toDetailResponse(p, presigned.url(), ai);
	}

	private List<CandidateResponse> toTopCandidates(String json, int topN) {
		List<CandidateIdScore> parsed = candidateJsonParser.parse(json);
		if (parsed.isEmpty()) {
			return Collections.emptyList();
		}

		parsed.sort(new Comparator<CandidateIdScore>() {
			@Override
			public int compare(CandidateIdScore a, CandidateIdScore b) {
				if (a.score() == null && b.score() == null) {
					return 0;
				}
				if (a.score() == null) {
					return 1;
				}
				if (b.score() == null) {
					return -1;
				}
				return b.score().compareTo(a.score());
			}
		});

		int limit = Math.min(parsed.size(), topN);
		List<CandidateResponse> result = new ArrayList<CandidateResponse>(limit);

		for (int i = 0; i < limit; i++) {
			CandidateIdScore c = parsed.get(i);
			result.add(new CandidateResponse(c.id(), null, c.score()));
		}
		return result;
	}
}
