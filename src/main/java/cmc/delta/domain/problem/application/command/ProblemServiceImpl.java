package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.application.command.mapper.ProblemCreateMapper;
import cmc.delta.domain.problem.application.command.support.ProblemCreateAssembler;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateCurriculumValidator;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateRequestValidator;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

	private final ProblemJpaRepository problemRepository;
	private final UserJpaRepository userRepository;

	private final ProblemCreateRequestValidator requestValidator;
	private final ProblemCreateScanValidator scanValidator;
	private final ProblemCreateCurriculumValidator curriculumValidator;

	private final ProblemCreateAssembler assembler;
	private final ProblemCreateMapper mapper;

	@Override
	@Transactional
	public ProblemCreateResponse createWrongAnswerCard(Long currentUserId, ProblemCreateRequest request) {
		validateCreateRequest(request);

		ProblemScan scan = loadOwnedAiDoneScan(currentUserId, request.scanId());
		ensureProblemNotExistsForScan(request.scanId());

		Unit finalUnit = loadFinalUnit(request.finalUnitId());
		ProblemType finalType = loadFinalType(request.finalTypeId());
		User userRef = loadUserReference(currentUserId);

		Problem newProblem = assembleProblem(userRef, scan, finalUnit, finalType, request);
		Problem savedProblem = saveProblemWithDuplicateGuard(newProblem);

		return mapper.toResponse(savedProblem);
	}

	private void validateCreateRequest(ProblemCreateRequest request) {
		requestValidator.validate(request);
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
		ProblemCreateRequest request
	) {
		return assembler.assemble(userRef, scan, finalUnit, finalType, request);
	}

	private Problem saveProblemWithDuplicateGuard(Problem problem) {
		try {
			return problemRepository.save(problem);
		} catch (DataIntegrityViolationException e) {
			throw scanValidator.toProblemAlreadyCreatedException(e);
		}
	}
}
