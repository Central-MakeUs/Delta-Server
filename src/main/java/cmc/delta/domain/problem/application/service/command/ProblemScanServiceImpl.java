package cmc.delta.domain.problem.application.service.command;

import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.support.command.ProblemScanStoragePaths;
import cmc.delta.domain.problem.application.validation.command.ProblemScanStatusValidator;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
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
public class ProblemScanServiceImpl implements ScanCommandUseCase {

	private final StorageService storageService;
	private final UserRepositoryPort userRepositoryPort;
	private final ScanRepository scanRepository;
	private final AssetJpaRepository assetRepository;

	private final ProblemScanStatusValidator statusValidator;
	private final Clock clock;

	@Transactional
	@Override
	public ScanCreateResult createScan(Long userId, CreateScanCommand command) {
		MultipartFile file = command.file();
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "업로드 파일이 비어있습니다.");
		}

		StorageUploadData uploaded = storageService.uploadImage(file, ProblemScanStoragePaths.ORIGINAL_DIR);

		User userRef = userRepositoryPort.getReferenceById(userId);
		ProblemScan scan = scanRepository.save(ProblemScan.uploaded(userRef));

		Asset original = assetRepository.save(Asset.createOriginal(
			scan,
			uploaded.storageKey(),
			uploaded.width(),
			uploaded.height()
		));

		return new ScanCreateResult(
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
