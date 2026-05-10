package cmc.delta.domain.user.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import cmc.delta.domain.auth.application.port.out.AdminUserQueryPort;
import cmc.delta.domain.stats.application.port.out.StatsUserQueryPort;
import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.UserWithProvider;
import cmc.delta.domain.user.model.enums.UserRole;
import cmc.delta.domain.user.model.enums.UserStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort, StatsUserQueryPort, AdminUserQueryPort {

	private final UserJpaRepository jpaRepository;
	private final EntityManager em;

	@Override
	public Optional<User> findById(Long id) {
		return jpaRepository.findById(id);
	}

	@Override
	public Optional<UserWithProvider> findWithProviderById(Long id) {
		return jpaRepository.findWithProviderById(id);
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

	@Override
	public long count() {
		return jpaRepository.count();
	}

	@Override
	public long countAll() {
		return jpaRepository.count();
	}

	@Override
	public long countByStatus(UserStatus status) {
		return jpaRepository.countByStatus(status);
	}

	@Override
	public long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
		return jpaRepository.countByCreatedAtBetween(from, to);
	}

	@Override
	public long countAllExcludingAdmin() {
		return jpaRepository.countByRoleNot(UserRole.ADMIN);
	}

	@Override
	public long countByStatusExcludingAdmin(UserStatus status) {
		return jpaRepository.countByStatusAndRoleNot(status, UserRole.ADMIN);
	}

	@Override
	public long countByCreatedAtBetweenExcludingAdmin(LocalDateTime from, LocalDateTime to) {
		return jpaRepository.countByRoleNotAndCreatedAtBetween(UserRole.ADMIN, from, to);
	}

	@Override
	public Optional<User> findAdminByUsername(String username) {
		return jpaRepository.findByEmailAndRole(username, UserRole.ADMIN);
	}
}
