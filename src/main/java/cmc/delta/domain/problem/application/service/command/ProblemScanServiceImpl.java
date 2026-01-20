package cmc.delta.domain.problem.application.service.command;

import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.application.support.command.ProblemScanStoragePaths;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemScanStatusValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProblemScanServiceImpl implements ScanCommandUseCase {

	private final StoragePort storagePort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProblemScanRepositoryPort scanRepositoryPort;
	private final AssetRepositoryPort assetRepositoryPort;

	private final ProblemCreateScanValidator uploadValidator;
	private final ProblemScanStatusValidator statusValidator;
	private final Clock clock;

	@Transactional
	@Override
	public ScanCreateResult createScan(Long userId, CreateScanCommand command) {
		MultipartFile file = command.file();
		uploadValidator.validateFileNotEmpty(file);

		StoragePort.UploadResult uploaded =
			storagePort.uploadImage(file, ProblemScanStoragePaths.ORIGINAL_DIR);

		User userRef = userRepositoryPort.getReferenceById(userId);
		ProblemScan scan = scanRepositoryPort.save(ProblemScan.uploaded(userRef));

		Asset original = assetRepositoryPort.save(Asset.createOriginal(
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
		ProblemScan scan = scanRepositoryPort.findOwnedByForUpdate(scanId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_SCAN_NOT_FOUND, "스캔을 찾을 수 없습니다."));

		statusValidator.requireFailed(scan);
		scan.retryFailed(LocalDateTime.now(clock));
	}
}
