package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.domain.user.application.port.out.ProfileImageStoragePort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class UserProfileImageServiceImplTest {

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
		UserJpaRepository userRepository = mock(UserJpaRepository.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned("user@example.com", "delta");
		setId(user, 10L);
		user.updateProfileImage("old.png");
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));

		StorageUploadData uploaded = new StorageUploadData("new.png", null, "image/png", 10L, null, null);
		when(storagePort.uploadImage(any(), eq("image/png"), eq("p.png"), any())).thenReturn(uploaded);

		StoragePresignedGetData presigned = new StoragePresignedGetData("https://read/new.png", 60);
		when(storagePort.issueReadUrl("new.png", null)).thenReturn(presigned);

		TransactionSynchronizationManager.initSynchronization();

		// when
		UserProfileImageResult result = sut.uploadMyProfileImage(
			10L,
			new ProfileImageUploadCommand("x".getBytes(), "image/png", "p.png"));

		// then
		assertThat(result.storageKey()).isEqualTo("new.png");
		assertThat(result.viewUrl()).isEqualTo("https://read/new.png");
		assertThat(result.ttlSeconds()).isEqualTo(60);
		assertThat(user.getProfileImageStorageKey()).isEqualTo("new.png");

		// afterCommit: old 삭제
		triggerAfterCommit();
		verify(storagePort).deleteImage("old.png");
	}

	@Test
	@DisplayName("프로필 이미지 업로드: 유저가 없으면 USER_NOT_FOUND")
	void uploadMyProfileImage_whenUserMissing_thenThrowsUserNotFound() {
		// given
		UserJpaRepository userRepository = mock(UserJpaRepository.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> sut.uploadMyProfileImage(999L, new ProfileImageUploadCommand(new byte[0], "image/png", "p.png")),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("프로필 이미지 조회: storageKey가 null이면 empty를 반환")
	void getMyProfileImage_whenNoKey_thenReturnsEmpty() {
		// given
		UserJpaRepository userRepository = mock(UserJpaRepository.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned("user@example.com", "delta");
		setId(user, 10L);
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));

		// when
		UserProfileImageResult result = sut.getMyProfileImage(10L);

		// then
		assertThat(result).isEqualTo(UserProfileImageResult.empty());
		verify(storagePort, never()).issueReadUrl(any(), any());
	}

	@Test
	@DisplayName("프로필 이미지 삭제: profile key를 비우고 afterCommit에 기존 파일 삭제를 등록")
	void deleteMyProfileImage_success_clearsAndDeletesAfterCommit() {
		// given
		UserJpaRepository userRepository = mock(UserJpaRepository.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned("user@example.com", "delta");
		setId(user, 10L);
		user.updateProfileImage("old.png");
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));

		TransactionSynchronizationManager.initSynchronization();

		// when
		sut.deleteMyProfileImage(10L);

		// then
		assertThat(user.getProfileImageStorageKey()).isNull();

		triggerAfterCommit();
		verify(storagePort).deleteImage("old.png");
	}

	@Test
	@DisplayName("프로필 이미지: 탈퇴 유저면 USER_WITHDRAWN")
	void profileImage_whenWithdrawn_thenThrowsUserWithdrawn() {
		// given
		UserJpaRepository userRepository = mock(UserJpaRepository.class);
		ProfileImageStoragePort storagePort = mock(ProfileImageStoragePort.class);
		UserProfileImageServiceImpl sut = new UserProfileImageServiceImpl(userRepository, storagePort);

		User user = User.createProvisioned("user@example.com", "delta");
		user.withdraw();
		setId(user, 10L);
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));

		// when
		BusinessException ex = catchThrowableOfType(
			() -> sut.getMyProfileImage(10L),
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
