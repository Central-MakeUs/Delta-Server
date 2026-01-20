package cmc.delta.domain.auth.application.port.out;

import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import java.util.Optional;

public interface SocialAccountRepositoryPort {
	Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
	SocialAccount save(SocialAccount account);
}
