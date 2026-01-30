package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserUseCase;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
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
		return userRepositoryPort.findById(userId)
			.map(u -> new UserMeData(u.getId(), u.getEmail(), u.getNickname()))
			.orElseThrow(UserException::userNotFound);
	}

	@Override
	public void withdrawAccount(Long userId) {
		User user = userRepositoryPort.findById(userId)
			.orElseThrow(UserException::userNotFound);

		userRepositoryPort.delete(user);

		log.info("event=user.delete userId={} result=success", userId);
	}

	@Override
    public void completeOnboarding(long userId, UserOnboardingRequest request) {
        userValidator.validate(request);

		User user = userRepositoryPort.findById(userId)
			.orElseThrow(UserException::userNotFound);

		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}

        // migrate: use nickname from request instead of name
        user.completeOnboarding(request.nickname(), request.birthDate(), Instant.now());
        userRepositoryPort.save(user);

		log.info("event=user.onboarding.complete userId={} result=success", userId);
	}

	@Override
    public void updateMyNickname(long userId, cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest request) {
        // validate nickname
        if (request == null || request.nickname() == null || request.nickname().trim().isEmpty()) {
            throw UserException.invalidRequest();
        }

        User user = userRepositoryPort.findById(userId)
            .orElseThrow(UserException::userNotFound);

        if (user.isWithdrawn()) {
            throw UserException.userWithdrawn();
        }

        user.updateName(request.nickname());
        userRepositoryPort.save(user);

        log.info("event=user.nickname.update userId={} result=success", userId);
    }
}
