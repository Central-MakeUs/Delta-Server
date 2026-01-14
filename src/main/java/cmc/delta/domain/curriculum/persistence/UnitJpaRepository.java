package cmc.delta.domain.curriculum.persistence;

import cmc.delta.domain.curriculum.model.Unit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnitJpaRepository extends JpaRepository<Unit, String> {

	List<Unit> findAllByActiveTrueOrderBySortOrderAsc();

	@Query("""
		select u
		from Unit u
		where u.active = true
		  and u.parent is null
		order by u.sortOrder asc
		""")
	List<Unit> findAllRootUnitsActive();
}
