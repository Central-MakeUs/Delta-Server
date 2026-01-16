package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.query.support.ProblemScanDetailMapper;
import cmc.delta.domain.problem.application.query.support.ProblemScanDetailValidator;
import cmc.delta.domain.problem.application.query.support.UnitSubjectResolver;
import cmc.delta.domain.problem.persistence.scan.ProblemScanDetailProjection;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanQueryServiceImpl implements ProblemScanQueryService {

	private final ProblemScanJpaRepository scanRepository;
	private final StorageService storageService;

	private final ProblemScanDetailValidator validator;
	private final UnitSubjectResolver subjectResolver;
	private final ProblemScanDetailMapper mapper;

	@Transactional(readOnly = true)
	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ProblemScanDetailProjection p = validator.getOwnedDetail(scanRepository, scanId, userId);
		validator.validateOriginalAsset(p);

		StoragePresignedGetData presigned = storageService.issueReadUrl(p.getStorageKey(), null);
		ProblemScanDetailMapper.SubjectInfo subject = subjectResolver.resolveByUnitId(p.getPredictedUnitId());

		ProblemScanDetailResponse.AiClassification ai = mapper.toAiClassification(p, subject);
		return mapper.toDetailResponse(p, presigned.url(), ai);
	}
}
