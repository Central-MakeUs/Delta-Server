package cmc.delta.domain.problem.application.service.impl;

import cmc.delta.domain.problem.api.dto.response.ProblemScanCreateResponse;
import cmc.delta.domain.problem.application.service.ProblemScanService;
import cmc.delta.domain.problem.model.Asset;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.StorageService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProblemScanServiceImpl implements ProblemScanService {

	private static final String ORIGINAL_DIR = "problem-scan/original";

	private final StorageService storageService;
	private final UserJpaRepository userRepository;
	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;

	@Transactional
	@Override
	public ProblemScanCreateResponse createScan(Long userId, MultipartFile file) {
		StorageUploadData uploaded = storageService.uploadImage(file, ORIGINAL_DIR);

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

		if (scan.getStatus() != ScanStatus.FAILED) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "FAILED 상태에서만 재시도할 수 있습니다.");
		}

		scan.retryFailed(LocalDateTime.now());
	}
}
