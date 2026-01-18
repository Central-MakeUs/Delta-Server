package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.application.command.mapper.ProblemCreateMapper;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateRequestValidator;
import cmc.delta.domain.problem.application.command.validation.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final ProblemJpaRepository problemRepository;
	private final UserJpaRepository userRepository;

	private final ProblemCreateRequestValidator requestValidator;
	private final ProblemCreateScanValidator scanValidator;
	private final ProblemCreateMapper mapper;

	@Override
	@Transactional
	public ProblemCreateResponse createWrongAnswerCard(Long currentUserId, ProblemCreateRequest request) {
		requestValidator.validate(request);

		Long scanId = request.scanId();

		ProblemScan ownedScan = scanValidator.getOwnedScan(scanRepository, currentUserId, scanId);
		scanValidator.validateScanIsAiDone(ownedScan);
		scanValidator.validateProblemNotCreated(problemRepository, scanId);

		Asset originalAsset = scanValidator.getOriginalAsset(assetRepository, scanId);

		User userRef = userRepository.getReferenceById(currentUserId);

		Problem createdProblem = mapper.toEntity(userRef, ownedScan, originalAsset, request);
		Problem savedProblem = problemRepository.save(createdProblem);

		return mapper.toResponse(savedProblem);
	}
}
