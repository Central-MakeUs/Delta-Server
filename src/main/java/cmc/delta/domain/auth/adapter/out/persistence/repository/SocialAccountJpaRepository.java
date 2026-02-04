package cmc.delta.domain.auth.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.model.User;

public interface SocialAccountJpaRepository
    extends JpaRepository<SocialAccount, Long>, SocialAccountRepositoryPort {

    @Override
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    @Override
    Optional<SocialAccount> findByUser(User user);
}
