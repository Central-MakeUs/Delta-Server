package cmc.delta.domain.problem.adapter.out.persistence.scan;

import cmc.delta.domain.problem.model.scan.ProblemScan;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScanRepository extends JpaRepository<ProblemScan, Long> {

	Optional<ProblemScan> findByIdAndUserId(Long id, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select s
		  from ProblemScan s
		 where s.id = :scanId
		   and s.user.id = :userId
	""")
	Optional<ProblemScan> findOwnedByForUpdate(
		@Param("scanId") Long scanId,
		@Param("userId") Long userId
	);
}
