package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserProfileImageUseCase;
import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.domain.user.application.port.out.ProfileImageStoragePort;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
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

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileImageStoragePort storagePort;

	@Override
	public UserProfileImageResult uploadMyProfileImage(Long userId, ProfileImageUploadCommand command) {
		User user = findActiveUser(userId);

		String oldKey = user.getProfileImageStorageKey();

		StorageUploadData uploaded = storagePort.uploadImage(
			command.bytes(),
			command.contentType(),
			command.originalFilename(),
			PROFILE_DIR);

		String newKey = uploaded.storageKey();

		user.updateProfileImage(newKey);

		StoragePresignedGetData presigned = storagePort.issueReadUrl(newKey, null);

		afterCommit(() -> deleteOldBestEffort(oldKey, newKey));

		return new UserProfileImageResult(
			newKey,
			presigned.url(),
			presigned.expiresInSeconds());
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileImageResult getMyProfileImage(Long userId) {
		User user = findActiveUser(userId);

		String key = user.getProfileImageStorageKey();
		if (key == null)
			return UserProfileImageResult.empty();

		StoragePresignedGetData presigned = storagePort.issueReadUrl(key, null);

		return new UserProfileImageResult(
			key,
			presigned.url(),
			presigned.expiresInSeconds());
	}

	@Override
	public void deleteMyProfileImage(Long userId) {
		User user = findActiveUser(userId);

		String oldKey = user.getProfileImageStorageKey();
		user.clearProfileImage();

		afterCommit(() -> deleteOldBestEffort(oldKey, null));
	}

	private User findActiveUser(Long userId) {
		User user = findUserOrThrow(userId);
		ensureActiveUser(user);
		return user;
	}

	private User findUserOrThrow(Long userId) {
		return userRepositoryPort.findById(userId)
			.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
	}

	private void ensureActiveUser(User user) {
		if (user.isWithdrawn()) {
			throw new UserException(ErrorCode.USER_WITHDRAWN);
		}
	}

	private void deleteOldBestEffort(String oldKey, String newKey) {
		if (!shouldDeleteOldKey(oldKey, newKey)) {
			return;
		}
		tryDeleteOldKey(oldKey);
	}

	private boolean shouldDeleteOldKey(String oldKey, String newKey) {
		if (oldKey == null) {
			return false;
		}
		if (newKey == null) {
			return true;
		}
		return !oldKey.equals(newKey);
	}

	private void tryDeleteOldKey(String oldKey) {
		try {
			storagePort.deleteImage(oldKey);
		} catch (Exception e) {
			log.warn("프로필 이미지 이전 파일 삭제 실패 storageKey={}", oldKey);
		}
	}

	private void afterCommit(Runnable runnable) {
		if (!TransactionSynchronizationManager.isSynchronizationActive())
			return;

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				runnable.run();
			}
		});
	}
}
