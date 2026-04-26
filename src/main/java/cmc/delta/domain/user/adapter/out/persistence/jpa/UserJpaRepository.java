package cmc.delta.domain.user.adapter.out.persistence.jpa;

import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.UserWithProvider;
import cmc.delta.domain.user.model.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<User, Long> {

	@Query("""
			select new cmc.delta.domain.user.model.UserWithProvider(u, s.provider)
			  from User u
			  left join SocialAccount s on s.user = u
			 where u.id = :userId
		""")
	Optional<UserWithProvider> findWithProviderById(@Param("userId") Long userId);

	@Query("""
			select u.id
			  from User u
			 where u.status = :status
			   and u.withdrawnAt is not null
			   and u.withdrawnAt < :cutoff
			 order by u.withdrawnAt asc, u.id asc
		""")
	List<Long> findIdsByStatusAndWithdrawnAtBefore(
		@Param("status")
		UserStatus status,
		@Param("cutoff")
		Instant cutoff,
		Pageable pageable);

	long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

	long count();

	long countByStatus(UserStatus status);
}
