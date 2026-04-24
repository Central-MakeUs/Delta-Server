package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserUseCase;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.UserWithProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final UserValidator userValidator;

	@Override
	@Transactional(readOnly = true)
	public UserMeData getMyProfile(long userId) {
		UserWithProvider uwp = userRepositoryPort.findWithProviderById(userId)
			.orElseThrow(UserException::userNotFound);
		return new UserMeData(
			uwp.user().getId(),
			uwp.user().getEmail(),
			uwp.user().getNickname(),
			uwp.provider());
	}

	@Override
	public void withdrawAccount(Long userId) {
		User user = userRepositoryPort.findById(userId).orElseThrow(UserException::userNotFound);
		withdrawUser(user);
		logUserWithdrawn(userId);
	}

	@Override
	public void completeOnboarding(long userId, UserOnboardingRequest request) {
		UserOnboardingRequest validRequest = validateOnboardingRequest(request);
		User user = loadActiveUser(userId);
		completeOnboarding(user, validRequest);
		logOnboardingCompleted(userId);
	}

	@Override
	public void updateMyNickname(long userId,
		UserNicknameUpdateRequest request) {
		String nickname = extractNickname(request);
		User user = loadActiveUser(userId);
		updateNickname(user, nickname);
		logNicknameUpdated(userId);
	}

	private User loadActiveUser(long userId) {
		return userRepositoryPort.findActiveById(userId);
	}

	private String extractNickname(UserNicknameUpdateRequest request) {
		userValidator.validate(request);
		return request.nickname();
	}

	private UserOnboardingRequest validateOnboardingRequest(UserOnboardingRequest request) {
		userValidator.validate(request);
		return request;
	}

	private void completeOnboarding(User user, UserOnboardingRequest request) {
		user.completeOnboarding(request.nickname(), Instant.now());
		userRepositoryPort.save(user);
	}

	private void updateNickname(User user, String nickname) {
		user.updateNickname(nickname);
		userRepositoryPort.save(user);
	}

	private void withdrawUser(User user) {
		if (!user.isWithdrawn()) {
			user.withdraw();
		}
		userRepositoryPort.save(user);
	}

	private void logUserWithdrawn(Long userId) {
		log.debug("event=user.withdraw userId={} result=success", userId);
	}

	private void logOnboardingCompleted(long userId) {
		log.debug("event=user.onboarding.complete userId={} result=success", userId);
	}

	private void logNicknameUpdated(long userId) {
		log.debug("event=user.nickname.update userId={} result=success", userId);
	}
}
