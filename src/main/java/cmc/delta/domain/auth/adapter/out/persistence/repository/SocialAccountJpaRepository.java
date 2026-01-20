package cmc.delta.domain.auth.adapter.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccount, Long> {
	Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
