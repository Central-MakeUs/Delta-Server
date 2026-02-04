package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanDetailMapper;
import cmc.delta.domain.problem.application.mapper.scan.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.ProblemScanDetailValidator;
import cmc.delta.domain.problem.application.validation.query.ProblemScanQueryValidator;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
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

		ScanSummaryContext context = buildSummaryContext(row, scanId);
		return summaryMapper.toSummaryResponse(
			row,
			context.viewUrl(),
			context.subject(),
			context.types());
	}

	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ScanDetailProjection p = scanQueryPort.findDetail(userId, scanId)
			.orElseThrow(this::scanNotFound);

		detailValidator.validateOriginalAsset(p);
		ScanDetailContext context = buildDetailContext(p, scanId);
		ProblemScanDetailResponse.AiClassification ai = detailMapper.toAiClassification(
			p,
			context.subject(),
			context.predictedTypes());
		return detailMapper.toDetailResponse(p, context.viewUrl(), ai);
	}

	private ScanSummaryContext buildSummaryContext(ScanListRow row, Long scanId) {
		String viewUrl = storagePort.issueReadUrl(row.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(row.getUnitId());
		List<CurriculumItemResponse> types = loadPredictedTypeItems(scanId);
		return new ScanSummaryContext(viewUrl, subject, types);
	}

	private ScanDetailContext buildDetailContext(ScanDetailProjection projection, Long scanId) {
		String viewUrl = storagePort.issueReadUrl(projection.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(projection.getPredictedUnitId());
		List<ProblemScanDetailResponse.PredictedTypeResponse> predictedTypes = loadPredictedTypeDetails(scanId);
		return new ScanDetailContext(viewUrl, subject, predictedTypes);
	}

	private List<CurriculumItemResponse> loadPredictedTypeItems(Long scanId) {
		return scanTypePredictionReader.findByScanId(scanId).stream()
			.map(v -> new CurriculumItemResponse(v.typeId(), v.typeName()))
			.toList();
	}

	private List<ProblemScanDetailResponse.PredictedTypeResponse> loadPredictedTypeDetails(Long scanId) {
		return scanTypePredictionReader.findByScanId(scanId).stream()
			.map(v -> new ProblemScanDetailResponse.PredictedTypeResponse(
				v.typeId(),
				v.typeName(),
				v.rankNo(),
				v.confidence()))
			.toList();
	}

	private ProblemException scanNotFound() {
		return new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}

	private record ScanSummaryContext(
		String viewUrl,
		SubjectInfo subject,
		List<CurriculumItemResponse> types) {
	}

	private record ScanDetailContext(
		String viewUrl,
		SubjectInfo subject,
		List<ProblemScanDetailResponse.PredictedTypeResponse> predictedTypes) {
	}
}
