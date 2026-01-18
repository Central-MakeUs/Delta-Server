package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanCreateResponse;
import cmc.delta.domain.problem.application.command.support.ProblemScanStatusValidator;
import cmc.delta.domain.problem.application.command.support.ProblemScanStoragePaths;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.StorageService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProblemScanServiceImpl implements ProblemScanService {

	private final StorageService storageService;
	private final UserJpaRepository userRepository;
	private final ScanRepository scanRepository;
	private final AssetJpaRepository assetRepository;

	private final ProblemScanStatusValidator statusValidator;
	private final Clock clock;

	@Transactional
	@Override
	public ProblemScanCreateResponse createScan(Long userId, MultipartFile file) {
		StorageUploadData uploaded = storageService.uploadImage(file, ProblemScanStoragePaths.ORIGINAL_DIR);

		User userRef = userRepository.getReferenceById(userId);
		ProblemScan scan = scanRepository.save(ProblemScan.uploaded(userRef));

		Asset original = assetRepository.save(Asset.createOriginal(
			scan,
			uploaded.storageKey(),
			uploaded.width(),
			uploaded.height()
		));

		return new ProblemScanCreateResponse(
			scan.getId(),
			original.getId(),
			scan.getStatus().name()
		);
	}

	@Transactional
	@Override
	public void retryFailed(Long userId, Long scanId) {
		ProblemScan scan = scanRepository.findOwnedByForUpdate(scanId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_SCAN_NOT_FOUND, "스캔을 찾을 수 없습니다."));

		statusValidator.requireFailed(scan);
		scan.retryFailed(LocalDateTime.now(clock));
	}
}
