package cmc.delta.domain.problem.application.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.mapper.command.ProblemCreateMapper;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemBulkCreateResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemCreateResponse;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.support.cache.ProblemScrollCacheEpochStore;
import cmc.delta.domain.problem.application.support.cache.ProblemStatsCacheEpochStore;
import cmc.delta.domain.problem.application.support.command.ProblemCreateAssembler;
import cmc.delta.domain.problem.application.validation.command.*;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ProblemServiceImplTest {

	private ProblemRepositoryPort problemRepositoryPort;
	private UserRepositoryPort userRepositoryPort;
	private AssetRepositoryPort assetRepositoryPort;
	private StoragePort storagePort;

	private ProblemCreateRequestValidator requestValidator;
	private ProblemCreateScanValidator scanValidator;
	private ProblemCreateCurriculumValidator curriculumValidator;

	private ProblemCreateAssembler assembler;
	private ProblemCreateMapper mapper;
	private ProblemUpdateRequestValidator updateRequestValidator;
	private ProblemScrollCacheEpochStore scrollCacheEpochStore;
	private ProblemStatsCacheEpochStore statsCacheEpochStore;

	private Clock fixedClock;

	private ProblemServiceImpl sut;

	@BeforeEach
	void setUp() {
		problemRepositoryPort = mock(ProblemRepositoryPort.class);
		userRepositoryPort = mock(UserRepositoryPort.class);
		assetRepositoryPort = mock(AssetRepositoryPort.class);
		storagePort = mock(StoragePort.class);

		requestValidator = mock(ProblemCreateRequestValidator.class);
		scanValidator = mock(ProblemCreateScanValidator.class);
		curriculumValidator = mock(ProblemCreateCurriculumValidator.class);

		assembler = mock(ProblemCreateAssembler.class);
		mapper = mock(ProblemCreateMapper.class);
		updateRequestValidator = mock(ProblemUpdateRequestValidator.class);
		scrollCacheEpochStore = mock(ProblemScrollCacheEpochStore.class);
		statsCacheEpochStore = mock(ProblemStatsCacheEpochStore.class);

		fixedClock = Clock.fixed(Instant.parse("2026-01-21T00:00:00Z"), ZoneId.of("UTC"));

		sut = new ProblemServiceImpl(
			problemRepositoryPort,
			userRepositoryPort,
			assetRepositoryPort,
			storagePort,
			requestValidator,
			scanValidator,
			curriculumValidator,
			assembler,
			mapper,
			updateRequestValidator,
			scrollCacheEpochStore,
			statsCacheEpochStore,
			fixedClock);
	}

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("createWrongAnswerCard: 성공 시 mapper 응답을 반환한다")
	void createWrongAnswerCard_success() {
		// given
		CreateWrongAnswerCardCommand cmd = mock(CreateWrongAnswerCardCommand.class);
		ProblemCreateResponse expected = givenCreateFlowOk(10L, cmd);

		// when
		ProblemCreateResponse res = sut.createWrongAnswerCard(10L, cmd);

		// then
		assertThat(res).isSameAs(expected);
	}

	@Test
	@DisplayName("createBulkWrongAnswerCards: 커맨드 수만큼 결과가 반환된다")
	void createBulkWrongAnswerCards_returnsResultPerCommand() {
		// given — userRef를 공유하도록 먼저 고정
		User sharedUserRef = mock(User.class);
		when(userRepositoryPort.getReferenceById(10L)).thenReturn(sharedUserRef);

		CreateWrongAnswerCardCommand cmd1 = mock(CreateWrongAnswerCardCommand.class);
		CreateWrongAnswerCardCommand cmd2 = mock(CreateWrongAnswerCardCommand.class);
		ProblemCreateResponse expected1 = givenCreateFlowOkWithRef(10L, sharedUserRef, cmd1, 1L);
		ProblemCreateResponse expected2 = givenCreateFlowOkWithRef(10L, sharedUserRef, cmd2, 2L);

		// when
		ProblemBulkCreateResponse result = sut.createBulkWrongAnswerCards(10L, List.of(cmd1, cmd2));

		// then
		assertThat(result.problems()).containsExactly(expected1, expected2);
	}

	@Test
	@DisplayName("createBulkWrongAnswerCards: 빈 리스트이면 빈 응답을 반환한다")
	void createBulkWrongAnswerCards_emptyCommands_returnsEmptyResponse() {
		// given
		when(userRepositoryPort.getReferenceById(10L)).thenReturn(mock(User.class));

		// when
		ProblemBulkCreateResponse result = sut.createBulkWrongAnswerCards(10L, List.of());

		// then
		assertThat(result.problems()).isEmpty();
	}

	@Test
	@DisplayName("createBulkWrongAnswerCards: 처리 후 캐시 epoch가 한 번만 bump된다")
	void createBulkWrongAnswerCards_bumpsCachesOnce() {
		// given
		User sharedUserRef = mock(User.class);
		when(userRepositoryPort.getReferenceById(10L)).thenReturn(sharedUserRef);

		CreateWrongAnswerCardCommand cmd1 = mock(CreateWrongAnswerCardCommand.class);
		CreateWrongAnswerCardCommand cmd2 = mock(CreateWrongAnswerCardCommand.class);
		givenCreateFlowOkWithRef(10L, sharedUserRef, cmd1, 1L);
		givenCreateFlowOkWithRef(10L, sharedUserRef, cmd2, 2L);

		// when
		sut.createBulkWrongAnswerCards(10L, List.of(cmd1, cmd2));

		// then
		verify(scrollCacheEpochStore, times(1)).bumpAfterCommit(10L);
		verify(statsCacheEpochStore, times(1)).bumpAfterCommit(10L);
	}

	@Test
	@DisplayName("completeWrongAnswerCard: problem.complete가 호출된다")
	void completeWrongAnswerCard_success() {
		// given
		Problem p = givenProblemOwned(10L, 1L);
		LocalDateTime now = LocalDateTime.now(fixedClock);

		// when
		sut.completeWrongAnswerCard(10L, 1L, "sol");

		// then
		verify(p).complete("sol", now);
	}

	@Test
	@DisplayName("updateWrongAnswerCard: validateAndNormalize 결과로 applyUpdate가 호출된다")
	void updateWrongAnswerCard_success() {
		// given
		Problem p = givenProblemOwned(10L, 1L);
		UpdateWrongAnswerCardCommand req = mock(UpdateWrongAnswerCardCommand.class);
		ProblemUpdateCommand update = mock(ProblemUpdateCommand.class);

		when(updateRequestValidator.validateAndNormalize(p, req)).thenReturn(update);

		// when
		sut.updateWrongAnswerCard(10L, 1L, req);

		// then
		verify(p).applyUpdate(update);
	}

	@Test
	@DisplayName("deleteWrongAnswerCard: delete 후 afterCommit에서 이미지 삭제를 시도한다")
	void deleteWrongAnswerCard_success() {
		// given
		Problem p = givenProblemOwned(10L, 1L);
		when(p.getOriginalStorageKey()).thenReturn("s3/problem.png");
		when(problemRepositoryPort.existsByOriginalStorageKey("s3/problem.png")).thenReturn(false);
		TransactionSynchronizationManager.initSynchronization();

		// when
		sut.deleteWrongAnswerCard(10L, 1L);

		// then
		verify(problemRepositoryPort).delete(p);
		verify(scrollCacheEpochStore).bumpAfterCommit(10L);
		verify(statsCacheEpochStore).bumpAfterCommit(10L);
		triggerAfterCommit();
		verify(storagePort).deleteImage("s3/problem.png");
	}

	@Test
	@DisplayName("deleteWrongAnswerCard: 다른 문제에서 참조중이면 이미지를 삭제하지 않는다")
	void deleteWrongAnswerCard_whenOtherProblemUsesImage_thenSkipDelete() {
		// given
		Problem p = givenProblemOwned(10L, 1L);
		when(p.getOriginalStorageKey()).thenReturn("s3/problem.png");
		when(problemRepositoryPort.existsByOriginalStorageKey("s3/problem.png")).thenReturn(true);
		TransactionSynchronizationManager.initSynchronization();

		// when
		sut.deleteWrongAnswerCard(10L, 1L);
		triggerAfterCommit();

		// then
		verify(storagePort, never()).deleteImage(anyString());
	}

	private ProblemCreateResponse givenCreateFlowOk(long userId, CreateWrongAnswerCardCommand cmd) {
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(1L);
		Unit unit = mock(Unit.class);
		ProblemType type = mock(ProblemType.class);
		when(cmd.finalTypeIds()).thenReturn(java.util.List.of("T1"));

		when(scanValidator.getOwnedScan(userId, cmd.scanId())).thenReturn(scan);
		when(curriculumValidator.getFinalUnit(cmd.finalUnitId())).thenReturn(unit);
		when(curriculumValidator.getFinalTypes(eq(10L), eq(java.util.List.of("T1"))))
			.thenReturn(java.util.List.of(type));

		User userRef = mock(User.class);
		when(userRepositoryPort.getReferenceById(userId)).thenReturn(userRef);

		Asset original = mock(Asset.class);
		when(original.getStorageKey()).thenReturn("s3/scan.png");
		when(assetRepositoryPort.findOriginalByScanId(1L)).thenReturn(java.util.Optional.of(original));
		when(storagePort.copyImage(eq("s3/scan.png"), anyString())).thenReturn("s3/problem.png");

		Problem newProblem = mock(Problem.class);
		when(assembler.assemble(userRef, scan, "s3/problem.png", unit, type, cmd)).thenReturn(newProblem);

		Problem saved = mock(Problem.class);
		when(problemRepositoryPort.save(newProblem)).thenReturn(saved);

		ProblemCreateResponse expected = mock(ProblemCreateResponse.class);
		when(mapper.toResponse(saved)).thenReturn(expected);

		return expected;
	}

	/**
	 * bulk 테스트용: userRef를 외부에서 주입받아 getReferenceById를 중복 스텁하지 않는 플로우 설정.
	 * cmd mock에 scanId/finalUnitId/finalTypeIds를 명시적으로 지정해 커맨드 간 스텁 충돌을 방지한다.
	 */
	private ProblemCreateResponse givenCreateFlowOkWithRef(
		long userId, User userRef, CreateWrongAnswerCardCommand cmd, long scanIdSeed) {
		// cmd mock에 식별 가능한 값 지정 (null 반환으로 인한 스텁 충돌 방지)
		when(cmd.scanId()).thenReturn(scanIdSeed);
		when(cmd.finalUnitId()).thenReturn("unit" + scanIdSeed);
		when(cmd.finalTypeIds()).thenReturn(java.util.List.of("T" + scanIdSeed));

		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getId()).thenReturn(scanIdSeed);
		Unit unit = mock(Unit.class);
		ProblemType type = mock(ProblemType.class);

		when(scanValidator.getOwnedScan(userId, scanIdSeed)).thenReturn(scan);
		when(curriculumValidator.getFinalUnit("unit" + scanIdSeed)).thenReturn(unit);
		when(curriculumValidator.getFinalTypes(eq(userId), eq(java.util.List.of("T" + scanIdSeed))))
			.thenReturn(java.util.List.of(type));

		String srcKey = "s3/scan" + scanIdSeed + ".png";
		String destKey = "s3/problem" + scanIdSeed + ".png";
		Asset original = mock(Asset.class);
		when(original.getStorageKey()).thenReturn(srcKey);
		when(assetRepositoryPort.findOriginalByScanId(scanIdSeed)).thenReturn(java.util.Optional.of(original));
		when(storagePort.copyImage(eq(srcKey), anyString())).thenReturn(destKey);

		Problem newProblem = mock(Problem.class);
		when(assembler.assemble(userRef, scan, destKey, unit, type, cmd)).thenReturn(newProblem);

		Problem saved = mock(Problem.class);
		when(problemRepositoryPort.save(newProblem)).thenReturn(saved);

		ProblemCreateResponse expected = mock(ProblemCreateResponse.class);
		when(mapper.toResponse(saved)).thenReturn(expected);

		return expected;
	}

	private Problem givenProblemOwned(long userId, long problemId) {
		Problem p = mock(Problem.class);
		when(problemRepositoryPort.findByIdAndUserId(problemId, userId)).thenReturn(java.util.Optional.of(p));
		return p;
	}

	private void triggerAfterCommit() {
		List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
		for (TransactionSynchronization sync : syncs) {
			sync.afterCommit();
		}
	}
}
