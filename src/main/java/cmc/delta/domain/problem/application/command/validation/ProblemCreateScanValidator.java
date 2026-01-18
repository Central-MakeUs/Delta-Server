package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.problem.application.common.exception.ProblemAlreadyCreatedException;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotReadyException;
import cmc.delta.domain.problem.application.common.exception.ProblemScanRenderModeMissingException;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ProblemCreateScanValidator {

	public ProblemScan getOwnedScan(ProblemScanJpaRepository scanRepository, Long userId, Long scanId) {
		Optional<ProblemScan> found = scanRepository.findByIdAndUserId(scanId, userId);
		if (found.isEmpty()) {
			throw new ProblemScanNotFoundException();
		}
		return found.get();
	}

	public void validateScanIsAiDone(ProblemScan scan) {
		if (scan.getStatus() != ScanStatus.AI_DONE) {
			throw new ProblemScanNotReadyException();
		}
	}

	public void validateProblemNotAlreadyCreated(ProblemJpaRepository problemRepository, Long scanId) {
		boolean alreadyCreated = problemRepository.existsByScan_Id(scanId);
		if (alreadyCreated) {
			throw new ProblemAlreadyCreatedException();
		}
	}

	public ProblemAlreadyCreatedException toProblemAlreadyCreatedException(DataIntegrityViolationException e) {
		return new ProblemAlreadyCreatedException();
	}

	public ProblemScanRenderModeMissingException toScanRenderModeMissingException() {
		return new ProblemScanRenderModeMissingException();
	}
}
