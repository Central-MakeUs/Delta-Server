package cmc.delta.domain.problem.application.service.command;

import static cmc.delta.domain.problem.application.support.ProblemTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.support.FakeScanImageUploadPort;
import cmc.delta.domain.problem.application.support.InMemoryAssetRepositoryPort;
import cmc.delta.domain.problem.application.support.InMemoryProblemScanRepositoryPort;
import cmc.delta.domain.problem.application.support.command.ProblemScanStoragePaths;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.application.validation.command.ProblemScanStatusValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.support.FakeUserRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanServiceImplTest {

	private FakeScanImageUploadPort scanImageUploadPort;
	private FakeUserRepositoryPort userRepositoryPort;
	private InMemoryProblemScanRepositoryPort scanRepositoryPort;
	private InMemoryAssetRepositoryPort assetRepositoryPort;

	private ProblemCreateScanValidator uploadValidator;
	private ProblemScanStatusValidator statusValidator;

	private ProblemScanServiceImpl sut;

	@BeforeEach
	void setUp() {
		scanImageUploadPort = new FakeScanImageUploadPort();
		scanRepositoryPort = new InMemoryProblemScanRepositoryPort();
		assetRepositoryPort = new InMemoryAssetRepositoryPort();
		userRepositoryPort = FakeUserRepositoryPort.create();

		uploadValidator = mock(ProblemCreateScanValidator.class);
		statusValidator = mock(ProblemScanStatusValidator.class);

		Clock fixedClock = Clock.fixed(Instant.parse("2026-01-21T00:00:00Z"), ZoneId.of("UTC"));

		sut = new ProblemScanServiceImpl(
			scanImageUploadPort,
			userRepositoryPort,
			scanRepositoryPort,
			assetRepositoryPort,
			uploadValidator,
			statusValidator,
			fixedClock);
	}

	@Test
	@DisplayName("createScan: 업로드→scan 저장→original asset 저장 후 ScanCreateResult를 반환한다")
	void createScan_success() {
		// given
		long userId = givenUser();
		CreateScanCommand cmd = givenCreateScanCommand();

		// when
		ScanCreateResult r = sut.createScan(userId, cmd);

		// then
		thenCreatedAsUploaded(r, "s3/key.png", 100, 200);
	}

	private long givenUser() {
		return userRepositoryPort.save(user()).getId();
	}

	private CreateScanCommand givenCreateScanCommand() {
		CreateScanCommand cmd = mock(CreateScanCommand.class);
		when(cmd.file()).thenReturn(file());
		return cmd;
	}

    private void thenCreatedAsUploaded(ScanCreateResult r, String key, int w, int h) {
        assertThat(r.status()).isEqualTo("UPLOADED");
        String datePath = "2026/01/21"; // fixed clock in test setup
        long userId = scanRepositoryPort.getAll().get(0).getUser().getId();
        String expectedDir = ProblemScanStoragePaths.ORIGINAL_DIR + "/" + datePath + "/" + userId;
        assertThat(scanImageUploadPort.lastUploadDirectory).isEqualTo(expectedDir);

		assertThat(scanRepositoryPort.count()).isEqualTo(1);
		assertThat(assetRepositoryPort.count()).isEqualTo(1);

		ProblemScan scan = scanRepositoryPort.get(r.scanId());
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.UPLOADED);

		Asset asset = assetRepositoryPort.get(r.assetId());
		assertThat(asset)
			.extracting(Asset::getStorageKey, Asset::getWidth, Asset::getHeight)
			.containsExactly(key, w, h);
	}
}
