package cmc.delta.domain.problem.application.service.command;

import java.time.LocalDateTime;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.exception.ProblemNotFoundException;
import cmc.delta.domain.problem.application.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.mapper.command.ProblemCreateMapper;
import cmc.delta.domain.problem.application.port.in.problem.ProblemCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.support.command.ProblemCreateAssembler;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateCurriculumValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateRequestValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemUpdateRequestValidator;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.adapter.out.persistence.UserJpaRepository;
import cmc.delta.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemCommandUseCase {

	private final ProblemJpaRepository problemRepository;
	private final UserJpaRepository userRepository;

	private final ProblemCreateRequestValidator requestValidator;
	private final ProblemCreateScanValidator scanValidator;
	private final ProblemCreateCurriculumValidator curriculumValidator;

	private final ProblemCreateAssembler assembler;
	private final ProblemCreateMapper mapper;
	private final ProblemUpdateRequestValidator updateRequestValidator;

	@Override
	@Transactional
	public ProblemCreateResponse createWrongAnswerCard(Long currentUserId, CreateWrongAnswerCardCommand command) {
		validateCreateCommand(command);

		ProblemScan scan = loadOwnedAiDoneScan(currentUserId, command.scanId());
		ensureProblemNotExistsForScan(command.scanId());

		Unit finalUnit = loadFinalUnit(command.finalUnitId());
		ProblemType finalType = loadFinalType(command.finalTypeId());
		User userRef = loadUserReference(currentUserId);

		Problem newProblem = assembleProblem(userRef, scan, finalUnit, finalType, command);
		Problem savedProblem = saveProblemWithDuplicateGuard(newProblem);

		return mapper.toResponse(savedProblem);
	}

	@Override
	@Transactional
	public void completeWrongAnswerCard(Long currentUserId, Long problemId, String solutionText) {
		Problem problem = problemRepository.findByIdAndUserId(problemId, currentUserId)
			.orElseThrow(ProblemScanNotFoundException::new);

		LocalDateTime now = LocalDateTime.now();
		problem.complete(solutionText, now);
	}

	@Override
	@Transactional
	public void updateWrongAnswerCard(Long userId, Long problemId, UpdateWrongAnswerCardCommand command) {
		Problem problem = problemRepository.findByIdAndUserId(problemId, userId)
			.orElseThrow(ProblemNotFoundException::new);

		ProblemUpdateCommand cmd = updateRequestValidator.validateAndNormalize(problem, command);
		problem.applyUpdate(cmd);
	}

	private void validateCreateCommand(CreateWrongAnswerCardCommand command) {
		requestValidator.validate(command);
	}

	private ProblemScan loadOwnedAiDoneScan(Long userId, Long scanId) {
		ProblemScan scan = scanValidator.getOwnedScan(userId, scanId);
		scanValidator.validateScanIsAiDone(scan);
		return scan;
	}

	private void ensureProblemNotExistsForScan(Long scanId) {
		scanValidator.validateProblemNotAlreadyCreated(scanId);
	}

	private Unit loadFinalUnit(String finalUnitId) {
		return curriculumValidator.getFinalUnit(finalUnitId);
	}

	private ProblemType loadFinalType(String finalTypeId) {
		return curriculumValidator.getFinalType(finalTypeId);
	}

	private User loadUserReference(Long userId) {
		return userRepository.getReferenceById(userId);
	}

	private Problem assembleProblem(
		User userRef,
		ProblemScan scan,
		Unit finalUnit,
		ProblemType finalType,
		CreateWrongAnswerCardCommand command
	) {
		return assembler.assemble(userRef, scan, finalUnit, finalType, command);
	}

	private Problem saveProblemWithDuplicateGuard(Problem problem) {
		try {
			return problemRepository.save(problem);
		} catch (DataIntegrityViolationException e) {
			throw scanValidator.toProblemAlreadyCreatedException(e);
		}
	}
}
