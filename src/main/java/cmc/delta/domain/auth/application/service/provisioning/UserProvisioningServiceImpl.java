package cmc.delta.domain.auth.application.service.provisioning;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.application.validation.SocialUserProvisionValidator;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final SocialAccountRepositoryPort socialAccountRepositoryPort;
	private final SocialUserProvisionValidator validator;

	@Override
	@Transactional
	public ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command) {
		validator.validate(command);

		return socialAccountRepositoryPort
			.findByProviderAndProviderUserId(command.provider(), command.providerUserId())
			.map(this::toExistingUserResult)
			.orElseGet(() -> createOrSyncByRaceCondition(command));
	}

	private ProvisioningResult toExistingUserResult(SocialAccount account) {
		User user = requireActive(account.getUser());
		return new ProvisioningResult(user.getId(), user.getEmail(), user.getNickname(), false);
	}

	private ProvisioningResult createOrSyncByRaceCondition(SocialUserProvisionCommand command) {
		try {
			return createAndLink(command);
		} catch (DataIntegrityViolationException e) {
			return syncExistingAfterDuplicate(command, e);
		}
	}

	private ProvisioningResult createAndLink(SocialUserProvisionCommand command) {
		User user = userRepositoryPort.save(User.createProvisioned(command.email(), command.nickname()));
		SocialAccount account = SocialAccount.link(command.provider(), command.providerUserId(), user);
		socialAccountRepositoryPort.save(account);
		return new ProvisioningResult(user.getId(), user.getEmail(), user.getNickname(), true);
	}

	private ProvisioningResult syncExistingAfterDuplicate(
		SocialUserProvisionCommand command,
		DataIntegrityViolationException originalException) {
		SocialAccount existing = socialAccountRepositoryPort
			.findByProviderAndProviderUserId(command.provider(), command.providerUserId())
			.orElseThrow(() -> originalException);

		User user = requireActive(existing.getUser());

		String email = command.email();
		String nickname = command.nickname();

		if (StringUtils.hasText(email) || StringUtils.hasText(nickname)) {
			user.syncProfile(email, nickname);
		}

		return new ProvisioningResult(user.getId(), user.getEmail(), user.getNickname(), false);
	}

	// NOTE:
	// Sign in with Apple은 email/name이 항상 전달되지 않습니다. (최초 1회 제공/사용자 선택)
	// 신규 유저라도 프로필이 비어 있으면 ONBOARDING_REQUIRED 플로우로 이어지도록 허용합니다.

	private User requireActive(User user) {
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		return user;
	}
}
