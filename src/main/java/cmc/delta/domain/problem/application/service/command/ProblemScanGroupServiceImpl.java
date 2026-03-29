package cmc.delta.domain.problem.application.service.command;

import cmc.delta.domain.problem.application.port.in.scan.ScanGroupCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanGroupCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanGroupCreateResult;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanGroupRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.application.port.out.storage.ScanImageUploadPort;
import cmc.delta.domain.problem.application.support.command.ProblemScanStoragePaths;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.scan.ProblemScanGroup;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanGroupServiceImpl implements ScanGroupCommandUseCase {

	private final ScanImageUploadPort scanImageUploadPort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProblemScanGroupRepositoryPort scanGroupRepositoryPort;
	private final ProblemScanRepositoryPort scanRepositoryPort;
	private final AssetRepositoryPort assetRepositoryPort;
	private final ProblemCreateScanValidator uploadValidator;
	private final Clock clock;

	private static final String DATE_FORMAT = "yyyy/MM/dd";

	@Transactional
	@Override
	public ScanGroupCreateResult createScanGroup(Long userId, CreateScanGroupCommand command) {
		command.files().forEach(uploadValidator::validateFileNotEmpty);

		User userRef = userRepositoryPort.getReferenceById(userId);
		ProblemScanGroup group = scanGroupRepositoryPort.save(ProblemScanGroup.create(userRef));

		String datePath = LocalDate.now(clock).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
		String directory = ProblemScanStoragePaths.ORIGINAL_DIR + "/" + datePath + "/" + userId;

		List<ScanCreateResult> scans = new ArrayList<>();
		// batch size 설정 필요 (saveAll로 변경도 고려)
		for (UploadFile file : command.files()) {
			ScanImageUploadPort.UploadResult uploaded = scanImageUploadPort.uploadImage(file, directory);
			ProblemScan scan = scanRepositoryPort.save(ProblemScan.uploadedInGroup(userRef, group));
			Asset original = assetRepositoryPort.save(Asset.createOriginal(scan, uploaded.storageKey(), uploaded.width(), uploaded.height()));
			scans.add(new ScanCreateResult(scan.getId(), original.getId(), scan.getStatus().name()));
		}

		return new ScanGroupCreateResult(group.getId(), scans);
	}
}
