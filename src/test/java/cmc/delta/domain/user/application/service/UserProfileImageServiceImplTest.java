package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.domain.user.application.port.out.ProfileImageStoragePort;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class UserProfileImageServiceImplTest {

	private static final long USER_ID = 10L;
	private static final long MISSING_USER_ID = 999L;
	private static final long SIZE_BYTES = 10L;
	private static final int ZERO = 0;
	private static final int TTL_SECONDS = 60;
	private static final String EMAIL = "user@example.com";
	private static final String NICKNAME = "delta";
	private static final String OLD_KEY = "old.png";
	private static final String NEW_KEY = "new.png";
	private static final String CONTENT_TYPE = "image/png";
	private static final String FILE_NAME = "p.png";
	private static final String PRESIGNED_URL = "https://read/new.png";
	private static final byte[] EMPTY_BYTES = new byte[ZERO];
	private static final byte[] SAMPLE_BYTES = "x".getBytes();

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("프로필 이미지 업로드: 업로드 + user storageKey 갱신 + presigned 발급 후 결과를 반환")
	void uploadMyProfileImage_success_updatesUserAndReturnsResult() {
		// given
		UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned(EMAIL, NICKNAME);
		setId(user, USER_ID);
		user.updateProfileImage(OLD_KEY);
		when(userRepository.findActiveById(USER_ID)).thenReturn(user);

		StorageUploadData uploaded = new StorageUploadData(NEW_KEY, null, CONTENT_TYPE, SIZE_BYTES, null, null);
		when(storagePort.uploadImage(any(), eq(CONTENT_TYPE), eq(FILE_NAME), any())).thenReturn(uploaded);

		StoragePresignedGetData presigned = new StoragePresignedGetData(PRESIGNED_URL, TTL_SECONDS);
		when(storagePort.issueReadUrl(NEW_KEY, null)).thenReturn(presigned);

		TransactionSynchronizationManager.initSynchronization();

		// when
		UserProfileImageResult result = sut.uploadMyProfileImage(
			USER_ID,
			new ProfileImageUploadCommand(SAMPLE_BYTES, CONTENT_TYPE, FILE_NAME));

		// then
		assertThat(result.storageKey()).isEqualTo(NEW_KEY);
		assertThat(result.viewUrl()).isEqualTo(PRESIGNED_URL);
		assertThat(result.ttlSeconds()).isEqualTo(TTL_SECONDS);
		assertThat(user.getProfileImageStorageKey()).isEqualTo(NEW_KEY);

		// afterCommit: old 삭제
		triggerAfterCommit();
		verify(storagePort).deleteImage(OLD_KEY);
	}

	@Test
	@DisplayName("프로필 이미지 업로드: 유저가 없으면 USER_NOT_FOUND")
	void uploadMyProfileImage_whenUserMissing_thenThrowsUserNotFound() {
		// given
		UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		when(userRepository.findActiveById(MISSING_USER_ID)).thenThrow(UserException.userNotFound());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> sut.uploadMyProfileImage(MISSING_USER_ID,
				new ProfileImageUploadCommand(EMPTY_BYTES, CONTENT_TYPE, FILE_NAME)),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("프로필 이미지 조회: storageKey가 null이면 empty를 반환")
	void getMyProfileImage_whenNoKey_thenReturnsEmpty() {
		// given
		UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned(EMAIL, NICKNAME);
		setId(user, USER_ID);
		when(userRepository.findActiveById(USER_ID)).thenReturn(user);

		// when
		UserProfileImageResult result = sut.getMyProfileImage(USER_ID);

		// then
		assertThat(result).isEqualTo(UserProfileImageResult.empty());
		verify(storagePort, never()).issueReadUrl(any(), any());
	}

	@Test
	@DisplayName("프로필 이미지 삭제: profile key를 비우고 afterCommit에 기존 파일 삭제를 등록")
	void deleteMyProfileImage_success_clearsAndDeletesAfterCommit() {
		// given
		UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned(EMAIL, NICKNAME);
		setId(user, USER_ID);
		user.updateProfileImage(OLD_KEY);
		when(userRepository.findActiveById(USER_ID)).thenReturn(user);

		TransactionSynchronizationManager.initSynchronization();

		// when
		sut.deleteMyProfileImage(USER_ID);

		// then
		assertThat(user.getProfileImageStorageKey()).isNull();

		triggerAfterCommit();
		verify(storagePort).deleteImage(OLD_KEY);
	}

	@Test
	@DisplayName("프로필 이미지: 탈퇴 유저면 USER_WITHDRAWN")
	void profileImage_whenWithdrawn_thenThrowsUserWithdrawn() {
		// given
		UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned(EMAIL, NICKNAME);
		user.withdraw();
		setId(user, USER_ID);
		when(userRepository.findActiveById(USER_ID)).thenThrow(UserException.userWithdrawn());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> sut.getMyProfileImage(USER_ID),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN);
	}

	private void triggerAfterCommit() {
		List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
		for (TransactionSynchronization sync : syncs) {
			sync.afterCommit();
		}
		TransactionSynchronizationManager.clearSynchronization();
	}

	private void setId(User user, long id) {
		try {
			Field field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 id 세팅 실패", e);
		}
	}
}
