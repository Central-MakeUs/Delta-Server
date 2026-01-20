package cmc.delta.domain.curriculum.adapter.out.persistence.jpa;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
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
        order by t.sortOrder asc
        """)
	List<ProblemType> findAllActiveForUser(@Param("userId") Long userId);
}

