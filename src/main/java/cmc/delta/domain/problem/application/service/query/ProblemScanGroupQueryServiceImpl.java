package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.mapper.scan.ProblemScanSummaryMapper;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.ScanGroupQueryUseCase;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanGroupSummaryResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.prediction.ScanTypePredictionReader;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.support.query.UnitSubjectResolver;
import cmc.delta.domain.problem.application.validation.query.ProblemScanQueryValidator;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemScanGroupQueryServiceImpl implements ScanGroupQueryUseCase {

	private final ScanQueryPort scanQueryPort;
	private final StoragePort storagePort;
	private final ScanTypePredictionReader scanTypePredictionReader;
	private final UnitSubjectResolver subjectResolver;
	private final ProblemScanQueryValidator summaryValidator;
	private final ProblemScanSummaryMapper summaryMapper;

	@Override
	public ProblemScanGroupSummaryResponse getGroupSummary(Long userId, Long groupId) {
		List<ScanListRow> rows = scanQueryPort.findListRowsByGroupId(userId, groupId);

		List<ProblemScanSummaryResponse> scans = rows.stream()
			.map(this::buildSummaryResponse)
			.toList();

		return new ProblemScanGroupSummaryResponse(groupId, scans);
	}

	private ProblemScanSummaryResponse buildSummaryResponse(ScanListRow row) {
		summaryValidator.validateHasOriginalAsset(row);
		String viewUrl = storagePort.issueReadUrl(row.getStorageKey());
		SubjectInfo subject = subjectResolver.resolveByUnitId(row.getUnitId());
		List<CurriculumItemResponse> types = scanTypePredictionReader.findByScanId(row.getScanId())
			.stream()
			.map(v -> new CurriculumItemResponse(v.typeId(), v.typeName()))
			.toList();
		return summaryMapper.toSummaryResponse(row, viewUrl, subject, types);
	}
}
