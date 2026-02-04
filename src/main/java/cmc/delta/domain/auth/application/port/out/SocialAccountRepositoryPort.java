package cmc.delta.domain.auth.application.port.out;

import java.util.Optional;

import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.model.User;

public interface SocialAccountRepositoryPort {
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    SocialAccount save(SocialAccount account);

    Optional<SocialAccount> findByUser(User user);
}
