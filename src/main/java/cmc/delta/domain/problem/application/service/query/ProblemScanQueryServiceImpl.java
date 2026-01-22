package cmc.delta.domain.problem.application.service.query;

import java.util.List;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.projection.ScanDetailProjection;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanDetailMapper;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.ProblemScanDetailValidator;
import cmc.delta.domain.problem.application.validation.query.ProblemScanQueryValidator;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemScanQueryServiceImpl implements ProblemScanQueryUseCase {

	private final ScanQueryPort scanQueryPort;
	private final StoragePort storagePort;
	private final ScanTypePredictionReader scanTypePredictionReader;

	// detail
	private final ProblemScanDetailValidator detailValidator;
	private final UnitSubjectResolver subjectResolver;
	private final ProblemScanDetailMapper detailMapper;

	// summary
	private final ProblemScanQueryValidator summaryValidator;
	private final ProblemScanSummaryMapper summaryMapper;

	@Override
	public ProblemScanSummaryResponse getSummary(Long userId, Long scanId) {
		ScanListRow row = scanQueryPort.findListRow(userId, scanId)
			.orElseThrow(this::scanNotFound);

		summaryValidator.validateHasOriginalAsset(row);

		String viewUrl = storagePort.issueReadUrl(row.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(row.getUnitId());

		List<CurriculumItemResponse> types =
			scanTypePredictionReader.findByScanId(scanId).stream()
				.map(v -> new CurriculumItemResponse(v.typeId(), v.typeName()))
				.toList();

		return summaryMapper.toSummaryResponse(row, viewUrl, subject, types);
	}


	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ScanDetailProjection p = scanQueryPort.findDetail(userId, scanId)
			.orElseThrow(this::scanNotFound);

		detailValidator.validateOriginalAsset(p);

		String viewUrl = storagePort.issueReadUrl(p.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(p.getPredictedUnitId());

		List<ProblemScanDetailResponse.PredictedTypeResponse> predictedTypes =
			scanTypePredictionReader.findByScanId(scanId).stream()
				.map(v -> new ProblemScanDetailResponse.PredictedTypeResponse(
					v.typeId(),
					v.typeName(),
					v.rankNo(),
					v.confidence()
				))
				.toList();

		ProblemScanDetailResponse.AiClassification ai =
			detailMapper.toAiClassification(p, subject, predictedTypes);

		return detailMapper.toDetailResponse(p, viewUrl, ai);
	}


	private ProblemException scanNotFound() {
		return new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}
}
