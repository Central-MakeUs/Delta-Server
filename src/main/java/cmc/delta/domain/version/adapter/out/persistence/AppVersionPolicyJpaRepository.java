package cmc.delta.domain.version.adapter.out.persistence;

import cmc.delta.domain.version.model.AppVersionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppVersionPolicyJpaRepository extends JpaRepository<AppVersionPolicy, Long> {
}
