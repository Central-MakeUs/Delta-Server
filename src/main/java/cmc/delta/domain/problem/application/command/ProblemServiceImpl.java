package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.application.command.mapper.ProblemCreateMapper;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateCurriculumValidator;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateRequestValidator;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

	private static final String FALLBACK_PROBLEM_MARKDOWN = "(문제 텍스트 없음)";

	private final ProblemScanJpaRepository scanRepository;
	private final ProblemJpaRepository problemRepository;
	private final UserJpaRepository userRepository;

	private final UnitJpaRepository unitRepository;
	private final ProblemTypeJpaRepository typeRepository;

	private final ProblemCreateRequestValidator requestValidator;
	private final ProblemCreateScanValidator scanValidator;
	private final ProblemCreateCurriculumValidator curriculumValidator;

	private final ProblemCreateMapper mapper;

	@Override
	@Transactional
	public ProblemCreateResponse createWrongAnswerCard(Long currentUserId, ProblemCreateRequest request) {
		requestValidator.validate(request);

		Long scanId = request.scanId();

		ProblemScan ownedScan = scanValidator.getOwnedScan(scanRepository, currentUserId, scanId);
		scanValidator.validateScanIsAiDone(ownedScan);
		scanValidator.validateProblemNotAlreadyCreated(problemRepository, scanId);

		Unit finalUnit = curriculumValidator.getFinalUnit(unitRepository, request.finalUnitId());
		ProblemType finalType = curriculumValidator.getFinalType(typeRepository, request.finalTypeId());

		User userRef = userRepository.getReferenceById(currentUserId);

		RenderMode renderMode = resolveRenderMode(ownedScan);
		String problemMarkdown = resolveProblemMarkdown(ownedScan);

		Problem newProblem = Problem.create(
			userRef,
			ownedScan,
			finalUnit,
			finalType,
			renderMode,
			problemMarkdown,
			request.answerFormat(),
			request.answerValue(),
			request.answerChoiceNo(),
			request.solutionText(),
			request.memo()
		);

		Problem savedProblem;
		try {
			savedProblem = problemRepository.save(newProblem);
		} catch (DataIntegrityViolationException e) {
			// scan_id UNIQUE 경쟁 조건 방어(동시 요청 등)
			throw scanValidator.toProblemAlreadyCreatedException(e);
		}

		return mapper.toResponse(savedProblem);
	}

	private RenderMode resolveRenderMode(ProblemScan scan) {
		RenderMode renderMode = scan.getRenderMode();
		if (renderMode == null) {
			// 정책상 scan에 renderMode는 항상 있어야 정상이라 예외로 막는 게 안전
			throw scanValidator.toScanRenderModeMissingException();
		}
		return renderMode;
	}

	private String resolveProblemMarkdown(ProblemScan scan) {
		String ocrText = scan.getOcrPlainText();
		if (ocrText == null) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}
		String trimmed = ocrText.trim();
		if (trimmed.isEmpty()) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}
		return ocrText;
	}
}
