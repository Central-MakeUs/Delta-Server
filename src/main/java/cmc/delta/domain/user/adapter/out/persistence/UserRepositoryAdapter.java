package cmc.delta.domain.user.adapter.out.persistence;

import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

	private final UserJpaRepository jpaRepository;
	private final EntityManager em;

	@Override
	public Optional<User> findById(Long id) {
		return jpaRepository.findById(id);
	}

	@Override
	public User getReferenceById(Long id) {
		return em.getReference(User.class, id);
	}

	@Override
	public User save(User user) {
		return jpaRepository.save(user);
	}

	@Override
	public void delete(User user) {
		jpaRepository.delete(user);
	}
}
