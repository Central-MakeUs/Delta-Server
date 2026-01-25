package cmc.delta.domain.curriculum.adapter.out.persistence.jpa;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProblemTypeJpaRepository extends JpaRepository<ProblemType, String> {

	@Query("""
		      select t
		      from ProblemType t
		      where t.active = true
		        and (
		              t.custom = false
		              or (t.custom = true and t.createdByUser.id = :userId)
		  )
		order by t.sortOrder asc, t.id asc
		""")
	List<ProblemType> findAllActiveForUser(@Param("userId")
	Long userId);

	@Query("""
		select t
		from ProblemType t
		where (
		      t.custom = false
		      or (t.custom = true and t.createdByUser.id = :userId)
		)
		order by t.sortOrder asc, t.id asc
		""")
	List<ProblemType> findAllForUser(@Param("userId")
	Long userId);

	@Query("""
		select t
		from ProblemType t
		where t.custom = true
		  and t.createdByUser.id = :userId
		  and t.id = :typeId
		""")
	Optional<ProblemType> findOwnedCustomById(@Param("userId")
	Long userId, @Param("typeId")
	String typeId);

	boolean existsByCreatedByUserIdAndCustomTrueAndName(Long userId, String name);

	@Query("""
		select coalesce(max(t.sortOrder), 0)
		from ProblemType t
		where (
		      t.custom = false
		      or (t.custom = true and t.createdByUser.id = :userId)
		)
		""")
	int findMaxSortOrderVisibleForUser(@Param("userId")
	Long userId);

	@Query("""
		select t
		from ProblemType t
		where t.active = true
		  and t.id = :typeId
		  and (
		        t.custom = false
		        or (t.custom = true and t.createdByUser.id = :userId)
		  )
		""")
	Optional<ProblemType> findActiveVisibleById(@Param("userId")
	Long userId, @Param("typeId")
	String typeId);

	@Query("""
		select t
		from ProblemType t
		where t.active = true
		  and t.id in :typeIds
		  and (
		        t.custom = false
		        or (t.custom = true and t.createdByUser.id = :userId)
		  )
		""")
	List<ProblemType> findActiveVisibleByIds(
		@Param("userId")
		Long userId,
		@Param("typeIds")
		List<String> typeIds);
}
