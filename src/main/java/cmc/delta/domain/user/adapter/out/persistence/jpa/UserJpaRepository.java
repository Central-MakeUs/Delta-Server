package cmc.delta.domain.user.adapter.out.persistence.jpa;

import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.enums.UserStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<User, Long> {

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
}
