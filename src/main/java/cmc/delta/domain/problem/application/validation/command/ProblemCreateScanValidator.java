package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateScanValidator {

	private final ProblemScanRepositoryPort scanRepositoryPort;
	private final ProblemRepositoryPort problemRepositoryPort;

	public ProblemScan getOwnedScan(Long userId, Long scanId) {
		return scanRepositoryPort.findOwnedById(scanId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND));
	}

	public void validateScanIsAiDone(ProblemScan scan) {
		if (scan.getStatus() != ScanStatus.AI_DONE) {
			throw new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_READY);
		}
	}

	public void validateProblemNotAlreadyCreated(Long scanId) {
		boolean alreadyCreated = problemRepositoryPort.existsByScanId(scanId);
		if (alreadyCreated) {
			throw new ProblemStateException(ErrorCode.PROBLEM_ALREADY_CREATED);
		}
	}

	public void validateFileNotEmpty(UploadFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "업로드 파일이 비어있습니다.");
		}
	}
}
