package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateScanValidator {

	private final ScanRepository scanRepository;
	private final ProblemJpaRepository problemRepository;

	public ProblemScan getOwnedScan(Long userId, Long scanId) {
		return scanRepository.findByIdAndUserId(scanId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND));
	}

	public void validateScanIsAiDone(ProblemScan scan) {
		if (scan.getStatus() != ScanStatus.AI_DONE) {
			throw new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_READY);
		}
	}

	public void validateProblemNotAlreadyCreated(Long scanId) {
		boolean alreadyCreated = problemRepository.existsByScan_Id(scanId);
		if (alreadyCreated) {
			throw new ProblemStateException(ErrorCode.PROBLEM_ALREADY_CREATED);
		}
	}

	public ProblemStateException toProblemAlreadyCreatedException(DataIntegrityViolationException e) {
		return new ProblemStateException(ErrorCode.PROBLEM_ALREADY_CREATED);
	}
}
