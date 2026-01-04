package cmc.delta.domain.auth.persistence;

import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningService {

	private final UserJpaRepository userJpaRepository;
	private final SocialAccountJpaRepository socialAccountJpaRepository;
	private final UserValidator userValidator;

	@Override
	@Transactional
	public ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command) {
		userValidator.validateProvision(command);

		return socialAccountJpaRepository
			.findByProviderAndProviderUserId(command.provider(), command.providerUserId())
			.map(this::syncExistingUser)
			.orElseGet(() -> createUserAndLinkAccountSafely(command));
	}

	private ProvisioningResult syncExistingUser(SocialAccount account) {
		User user = account.getUser();
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		// profile 동기화는 호출부에서 제공한 값이 있어야 의미 있으므로, command가 필요하면 여기 구조를 약간 바꾸면 됨.
		// (현재는 create 경로에서만 sync, 아래에서 처리)
		return new ProvisioningResult(user, false);
	}

	private ProvisioningResult createUserAndLinkAccountSafely(SocialUserProvisionCommand command) {
		try {
			User user = userJpaRepository.save(User.create(command.email(), command.nickname()));
			SocialAccount account = SocialAccount.link(command.provider(), command.providerUserId(), user);
			socialAccountJpaRepository.save(account);
			return new ProvisioningResult(user, true);
		} catch (DataIntegrityViolationException e) {
			// 동시 로그인(중복 생성) 상황:
			// (provider, providerUserId) UNIQUE에 걸려 실패할 수 있으니 재조회로 멱등 보장
			SocialAccount existing = socialAccountJpaRepository
				.findByProviderAndProviderUserId(command.provider(), command.providerUserId())
				.orElseThrow(() -> e);

			User user = existing.getUser();
			if (user.isWithdrawn()) {
				throw UserException.userWithdrawn();
			}

			user.syncProfile(command.email(), command.nickname());
			return new ProvisioningResult(user, false);
		}
	}
}
