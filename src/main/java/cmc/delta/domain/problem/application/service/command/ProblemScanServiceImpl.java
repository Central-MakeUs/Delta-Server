package cmc.delta.domain.problem.application.service.command;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.application.port.out.storage.ScanImageUploadPort;
import cmc.delta.domain.problem.application.support.command.ProblemScanStoragePaths;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemScanStatusValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanServiceImpl implements ScanCommandUseCase {

	private final ScanImageUploadPort scanImageUploadPort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProblemScanRepositoryPort scanRepositoryPort;
	private final AssetRepositoryPort assetRepositoryPort;

	private final ProblemCreateScanValidator uploadValidator;
	private final ProblemScanStatusValidator statusValidator;
	private final Clock clock;

	@Transactional
	@Override
	public ScanCreateResult createScan(Long userId, CreateScanCommand command) {
		UploadFile file = command.file();
		uploadValidator.validateFileNotEmpty(file);

		String datePath = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		String directory = ProblemScanStoragePaths.ORIGINAL_DIR + "/" + datePath + "/" + userId;

		ScanImageUploadPort.UploadResult uploaded = scanImageUploadPort.uploadImage(file, directory);

		User userRef = userRepositoryPort.getReferenceById(userId);
		ProblemScan scan = scanRepositoryPort.save(ProblemScan.uploaded(userRef));

		Asset original = assetRepositoryPort.save(Asset.createOriginal(
			scan,
			uploaded.storageKey(),
			uploaded.width(),
			uploaded.height()));

		return new ScanCreateResult(
			scan.getId(),
			original.getId(),
			scan.getStatus().name());
	}

	@Transactional
	@Override
	public void retryFailed(Long userId, Long scanId) {
		ProblemScan scan = scanRepositoryPort.findOwnedByForUpdate(scanId, userId)
			.orElseThrow(ProblemException::scanNotFound);

		statusValidator.requireFailed(scan);
		scan.retryFailed(LocalDateTime.now(clock));
	}
}
