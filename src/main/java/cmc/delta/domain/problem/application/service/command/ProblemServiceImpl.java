package cmc.delta.domain.problem.application.service.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.command.ProblemCreateMapper;
import cmc.delta.domain.problem.application.port.in.problem.ProblemCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemCreateResponse;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.support.command.ProblemCreateAssembler;
import cmc.delta.domain.problem.application.support.cache.ProblemScrollCacheEpochStore;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateCurriculumValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateRequestValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemUpdateRequestValidator;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemCommandUseCase {

	private final ProblemRepositoryPort problemRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;

	private final ProblemCreateRequestValidator requestValidator;
	private final ProblemCreateScanValidator scanValidator;
	private final ProblemCreateCurriculumValidator curriculumValidator;

	private final ProblemCreateAssembler assembler;
	private final ProblemCreateMapper mapper;
	private final ProblemUpdateRequestValidator updateRequestValidator;
	private final ProblemScrollCacheEpochStore scrollCacheEpochStore;

	private final Clock clock;

	@Override
	@Transactional
	public ProblemCreateResponse createWrongAnswerCard(Long currentUserId, CreateWrongAnswerCardCommand command) {
		requestValidator.validate(command);

		ProblemScan scan = loadValidatedScan(currentUserId, command.scanId());
		Unit finalUnit = curriculumValidator.getFinalUnit(command.finalUnitId());
		List<ProblemType> finalTypes = loadFinalTypesOrThrow(currentUserId, command.finalTypeIds());
		User userRef = userRepositoryPort.getReferenceById(currentUserId);

		Problem newProblem = assembleProblem(userRef, scan, finalUnit, finalTypes, command);
		Problem savedProblem = problemRepositoryPort.save(newProblem);
		bumpScrollCache(currentUserId);
		return mapper.toResponse(savedProblem);
	}

	@Override
	@Transactional
	public void completeWrongAnswerCard(Long currentUserId, Long problemId, String solutionText) {
		Problem problem = loadProblemOrThrow(problemId, currentUserId);
		completeProblem(problem, solutionText);
		bumpScrollCache(currentUserId);
	}

	@Override
	@Transactional
	public void updateWrongAnswerCard(Long userId, Long problemId, UpdateWrongAnswerCardCommand command) {
		Problem problem = loadProblemOrThrow(problemId, userId);
		ProblemUpdateCommand updateCommand = updateRequestValidator.validateAndNormalize(problem, command);
		applyUpdate(problem, updateCommand);
		bumpScrollCache(userId);
	}

	private ProblemScan loadValidatedScan(Long userId, Long scanId) {
		ProblemScan scan = scanValidator.getOwnedScan(userId, scanId);
		scanValidator.validateScanIsAiDone(scan);
		scanValidator.validateProblemNotAlreadyCreated(scanId);
		return scan;
	}

	private List<ProblemType> loadFinalTypesOrThrow(Long userId, List<String> finalTypeIds) {
		List<ProblemType> finalTypes = curriculumValidator.getFinalTypes(userId, finalTypeIds);
		if (finalTypes == null || finalTypes.isEmpty()) {
			throw new ProblemException(ErrorCode.INVALID_REQUEST);
		}
		return finalTypes;
	}

	private Problem assembleProblem(
		User userRef,
		ProblemScan scan,
		Unit finalUnit,
		List<ProblemType> finalTypes,
		CreateWrongAnswerCardCommand command) {
		ProblemType primaryType = finalTypes.get(0);
		Problem newProblem = assembler.assemble(userRef, scan, finalUnit, primaryType, command);
		newProblem.replaceTypes(finalTypes);
		return newProblem;
	}

	private Problem loadProblemOrThrow(Long problemId, Long userId) {
		return problemRepositoryPort.findByIdAndUserId(problemId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));
	}

	private void applyUpdate(Problem problem, ProblemUpdateCommand updateCommand) {
		problem.applyUpdate(updateCommand);
	}

	private void completeProblem(Problem problem, String solutionText) {
		problem.complete(solutionText, LocalDateTime.now(clock));
	}

	private void bumpScrollCache(Long userId) {
		scrollCacheEpochStore.bumpAfterCommit(userId);
	}
}
