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
		return new ProvisioningResult(user.getId(), false);
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
		return new ProvisioningResult(user.getId(), true);
	}

	private ProvisioningResult syncExistingAfterDuplicate(
		SocialUserProvisionCommand command,
		DataIntegrityViolationException originalException
	) {
		SocialAccount existing = socialAccountRepositoryPort
			.findByProviderAndProviderUserId(command.provider(), command.providerUserId())
			.orElseThrow(() -> originalException);

		User user = requireActive(existing.getUser());
		user.syncProfile(command.email(), command.nickname());
		return new ProvisioningResult(user.getId(), false);
	}

	private User requireActive(User user) {
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		return user;
	}
}
