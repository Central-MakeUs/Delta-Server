package cmc.delta.domain.user.application.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserUseCase;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final SocialAccountRepositoryPort socialAccountRepositoryPort;
    private final UserValidator userValidator;

	@Override
	@Transactional(readOnly = true)
	public UserMeData getMyProfile(long userId) {
		User user = loadUser(userId);
		return buildMyProfile(user);
	}

	@Override
	public void withdrawAccount(Long userId) {
		User user = loadUser(userId);
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

	private UserMeData buildMyProfile(User user) {
		Optional<SocialAccount> socialAccount = socialAccountRepositoryPort.findByUser(user);
		SocialProvider provider = socialAccount
			.map(a -> a.getProvider())
			.orElse(null);
		return new UserMeData(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			provider);
	}

	private User loadUser(long userId) {
		return userRepositoryPort.findById(userId)
			.orElseThrow(UserException::userNotFound);
	}

	private User loadActiveUser(long userId) {
		User user = loadUser(userId);
		ensureActiveUser(user);
		return user;
	}

	private String extractNickname(UserNicknameUpdateRequest request) {
		userValidator.validate(request);
		return request.nickname();
	}

	private UserOnboardingRequest validateOnboardingRequest(UserOnboardingRequest request) {
		userValidator.validate(request);
		return request;
	}

	private void ensureActiveUser(User user) {
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
	}

	private void completeOnboarding(User user, UserOnboardingRequest request) {
		user.completeOnboarding(request.nickname(), request.birthDate(), Instant.now());
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
		log.info("event=user.withdraw userId={} result=success", userId);
	}

	private void logOnboardingCompleted(long userId) {
		log.info("event=user.onboarding.complete userId={} result=success", userId);
	}

	private void logNicknameUpdated(long userId) {
		log.info("event=user.nickname.update userId={} result=success", userId);
	}
}
