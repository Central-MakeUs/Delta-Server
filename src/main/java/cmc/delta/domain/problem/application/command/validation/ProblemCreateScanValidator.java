package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.problem.application.common.exception.ProblemAlreadyCreatedException;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotReadyException;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
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
			.orElseThrow(ProblemScanNotFoundException::new);
	}

	public void validateScanIsAiDone(ProblemScan scan) {
		if (scan.getStatus() != ScanStatus.AI_DONE) {
			throw new ProblemScanNotReadyException();
		}
	}

	public void validateProblemNotAlreadyCreated(Long scanId) {
		boolean alreadyCreated = problemRepository.existsByScan_Id(scanId);
		if (alreadyCreated) {
			throw new ProblemAlreadyCreatedException();
		}
	}

	public ProblemAlreadyCreatedException toProblemAlreadyCreatedException(DataIntegrityViolationException e) {
		return new ProblemAlreadyCreatedException();
	}
}
