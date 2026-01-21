package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserProfileImageUseCase;
import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.domain.user.application.port.out.ProfileImageStoragePort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileImageServiceImpl implements UserProfileImageUseCase {

	private static final String PROFILE_DIR = "users/profile";

	private final UserJpaRepository userRepository;
	private final ProfileImageStoragePort storagePort;

	@Override
	public UserProfileImageResult uploadMyProfileImage(Long userId, ProfileImageUploadCommand command) {
		User user = findActiveUser(userId);

		String oldKey = user.getProfileImageStorageKey();

		StorageUploadData uploaded = storagePort.uploadImage(
			command.bytes(),
			command.contentType(),
			command.originalFilename(),
			PROFILE_DIR
		);

		String newKey = uploaded.storageKey();

		user.updateProfileImage(newKey);

		StoragePresignedGetData presigned = storagePort.issueReadUrl(newKey, null);

		afterCommit(() -> deleteOldBestEffort(oldKey, newKey));

		return new UserProfileImageResult(
			newKey,
			presigned.url(),
			presigned.expiresInSeconds()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileImageResult getMyProfileImage(Long userId) {
		User user = findActiveUser(userId);

		String key = user.getProfileImageStorageKey();
		if (key == null) return UserProfileImageResult.empty();

		StoragePresignedGetData presigned = storagePort.issueReadUrl(key, null);

		return new UserProfileImageResult(
			key,
			presigned.url(),
			presigned.expiresInSeconds()
		);
	}

	@Override
	public void deleteMyProfileImage(Long userId) {
		User user = findActiveUser(userId);

		String oldKey = user.getProfileImageStorageKey();
		user.clearProfileImage();

		afterCommit(() -> deleteOldBestEffort(oldKey, null));
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		if (user.isWithdrawn()) {
			throw new UserException(ErrorCode.USER_WITHDRAWN);
		}
		return user;
	}

	private void deleteOldBestEffort(String oldKey, String newKey) {
		if (oldKey == null) return;
		if (newKey != null && oldKey.equals(newKey)) return;

		try {
			storagePort.deleteImage(oldKey);
		} catch (Exception e) {
			log.warn("프로필 이미지 이전 파일 삭제 실패 storageKey={}", oldKey);
		}
	}

	private void afterCommit(Runnable runnable) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				runnable.run();
			}
		});
	}
}
