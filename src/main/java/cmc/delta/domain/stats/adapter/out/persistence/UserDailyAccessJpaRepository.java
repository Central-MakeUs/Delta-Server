package cmc.delta.domain.stats.adapter.out.persistence;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cmc.delta.domain.stats.model.UserDailyAccess;

public interface UserDailyAccessJpaRepository extends JpaRepository<UserDailyAccess, Long> {

	@Query(value = "INSERT IGNORE INTO user_daily_access (user_id, access_date, created_at, updated_at) "
				   + "VALUES (:userId, :date, NOW(6), NOW(6))", nativeQuery = true)
	void insertIgnore(@Param("userId") Long userId, @Param("date") LocalDate date);
}
