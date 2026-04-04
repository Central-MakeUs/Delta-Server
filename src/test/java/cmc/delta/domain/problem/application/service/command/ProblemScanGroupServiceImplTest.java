package cmc.delta.domain.problem.application.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanGroupCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanGroupCreateResult;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanGroupRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.application.port.out.storage.ScanImageUploadPort;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.scan.ProblemScanGroup;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanGroupServiceImplTest {

	private ScanImageUploadPort scanImageUploadPort;
	private UserRepositoryPort userRepositoryPort;
	private ProblemScanGroupRepositoryPort scanGroupRepositoryPort;
	private ProblemScanRepositoryPort scanRepositoryPort;
	private AssetRepositoryPort assetRepositoryPort;
	private ProblemCreateScanValidator uploadValidator;
	private Clock fixedClock;

	private ProblemScanGroupServiceImpl sut;

	@BeforeEach
	void setUp() {
		scanImageUploadPort = mock(ScanImageUploadPort.class);
		userRepositoryPort = mock(UserRepositoryPort.class);
		scanGroupRepositoryPort = mock(ProblemScanGroupRepositoryPort.class);
		scanRepositoryPort = mock(ProblemScanRepositoryPort.class);
		assetRepositoryPort = mock(AssetRepositoryPort.class);
		uploadValidator = mock(ProblemCreateScanValidator.class);
		fixedClock = Clock.fixed(Instant.parse("2026-03-25T00:00:00Z"), ZoneId.of("UTC"));

		sut = new ProblemScanGroupServiceImpl(
			scanImageUploadPort,
			userRepositoryPort,
			scanGroupRepositoryPort,
			scanRepositoryPort,
			assetRepositoryPort,
			uploadValidator,
			fixedClock);
	}

	@Test
	@DisplayName("createScanGroup: 파일 수만큼 scan/asset이 생성되고 결과에 포함된다")
	void createScanGroup_success_returnsAllScans() {
		// given
		long userId = 10L;
		UploadFile file1 = new UploadFile("img1".getBytes(), "image/png", "a.png");
		UploadFile file2 = new UploadFile("img2".getBytes(), "image/jpeg", "b.jpg");
		CreateScanGroupCommand command = new CreateScanGroupCommand(List.of(file1, file2));

		User userRef = mock(User.class);
		ProblemScanGroup group = mock(ProblemScanGroup.class);
		when(group.getId()).thenReturn(99L);

		when(userRepositoryPort.getReferenceById(userId)).thenReturn(userRef);
		when(scanGroupRepositoryPort.save(any(ProblemScanGroup.class))).thenReturn(group);

		ProblemScan scan1 = stubbedScan(1L);
		ProblemScan scan2 = stubbedScan(2L);
		when(scanRepositoryPort.save(any(ProblemScan.class)))
			.thenReturn(scan1)
			.thenReturn(scan2);

		Asset asset1 = stubbedAsset(11L);
		Asset asset2 = stubbedAsset(12L);
		when(assetRepositoryPort.save(any(Asset.class)))
			.thenReturn(asset1)
			.thenReturn(asset2);

		when(scanImageUploadPort.uploadImage(eq(file1), anyString()))
			.thenReturn(new ScanImageUploadPort.UploadResult("key/a.png", 100, 200));
		when(scanImageUploadPort.uploadImage(eq(file2), anyString()))
			.thenReturn(new ScanImageUploadPort.UploadResult("key/b.jpg", 300, 400));

		// when
		ScanGroupCreateResult result = sut.createScanGroup(userId, command);

		// then
		assertThat(result.scanGroupId()).isEqualTo(99L);
		assertThat(result.scans()).hasSize(2);
		assertThat(result.scans().get(0).scanId()).isEqualTo(1L);
		assertThat(result.scans().get(0).assetId()).isEqualTo(11L);
		assertThat(result.scans().get(1).scanId()).isEqualTo(2L);
		assertThat(result.scans().get(1).assetId()).isEqualTo(12L);
	}

	@Test
	@DisplayName("createScanGroup: 각 파일에 대해 validateFileNotEmpty가 호출된다")
	void createScanGroup_validatesEachFileBeforeProcessing() {
		// given
		long userId = 10L;
		UploadFile file1 = new UploadFile("img1".getBytes(), "image/png", "a.png");
		UploadFile file2 = new UploadFile("img2".getBytes(), "image/png", "b.png");
		CreateScanGroupCommand command = new CreateScanGroupCommand(List.of(file1, file2));

		givenCreateGroupFlowOk(userId, command);

		// when
		sut.createScanGroup(userId, command);

		// then
		verify(uploadValidator).validateFileNotEmpty(file1);
		verify(uploadValidator).validateFileNotEmpty(file2);
	}

	@Test
	@DisplayName("createScanGroup: validateFileNotEmpty 예외 발생 시 그대로 전파된다")
	void createScanGroup_whenValidationFails_throwsException() {
		// given
		UploadFile emptyFile = new UploadFile(new byte[0], "image/png", "empty.png");
		CreateScanGroupCommand command = new CreateScanGroupCommand(List.of(emptyFile));

		doThrow(new cmc.delta.domain.problem.application.exception.ProblemValidationException("업로드 파일이 비어있습니다."))
			.when(uploadValidator).validateFileNotEmpty(emptyFile);

		// when & then
		assertThatThrownBy(() -> sut.createScanGroup(99L, command))
			.isInstanceOf(cmc.delta.domain.problem.application.exception.ProblemValidationException.class);

		verifyNoInteractions(scanGroupRepositoryPort);
	}

	@Test
	@DisplayName("createScanGroup: 스토리지 업로드 디렉토리에 userId와 날짜가 포함된다")
	void createScanGroup_uploadsToDirectoryContainingUserIdAndDate() {
		// given
		long userId = 10L;
		UploadFile file = new UploadFile("img".getBytes(), "image/png", "a.png");
		CreateScanGroupCommand command = new CreateScanGroupCommand(List.of(file));

		givenCreateGroupFlowOk(userId, command);

		// when
		sut.createScanGroup(userId, command);

		// then — clock이 2026-03-25 이므로 디렉토리에 "2026/03/25"와 userId "10" 포함
		verify(scanImageUploadPort).uploadImage(eq(file),
			argThat(dir -> dir.contains("2026/03/25") && dir.contains("10")));
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	private void givenCreateGroupFlowOk(long userId, CreateScanGroupCommand command) {
		User userRef = mock(User.class);
		ProblemScanGroup group = mock(ProblemScanGroup.class);
		when(group.getId()).thenReturn(99L);
		when(userRepositoryPort.getReferenceById(userId)).thenReturn(userRef);
		when(scanGroupRepositoryPort.save(any(ProblemScanGroup.class))).thenReturn(group);

		for (int i = 0; i < command.files().size(); i++) {
			UploadFile file = command.files().get(i);
			long scanId = i + 1L;
			long assetId = (i + 1L) * 10;
			ProblemScan scan = stubbedScan(scanId);
			Asset asset = stubbedAsset(assetId);
			when(scanImageUploadPort.uploadImage(eq(file), anyString()))
				.thenReturn(new ScanImageUploadPort.UploadResult("key/" + file.originalFilename(), 100, 200));
			when(scanRepositoryPort.save(any(ProblemScan.class))).thenReturn(scan);
			when(assetRepositoryPort.save(any(Asset.class))).thenReturn(asset);
		}
	}

	private ProblemScan stubbedScan(long id) {
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(id);
		when(scan.getStatus()).thenReturn(cmc.delta.domain.problem.model.enums.ScanStatus.UPLOADED);
		return scan;
	}

	private Asset stubbedAsset(long id) {
		Asset asset = mock(Asset.class);
		when(asset.getId()).thenReturn(id);
		return asset;
	}
}
