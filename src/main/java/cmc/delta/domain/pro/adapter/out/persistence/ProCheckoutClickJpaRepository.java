package cmc.delta.domain.pro.adapter.out.persistence;

import cmc.delta.domain.pro.model.ProCheckoutClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProCheckoutClickJpaRepository extends JpaRepository<ProCheckoutClick, Long> {

	@Query("select count(distinct c.userId) from ProCheckoutClick c")
	long countDistinctUsers();
}
