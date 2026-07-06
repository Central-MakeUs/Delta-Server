package cmc.delta.domain.report.adapter.out.persistence;

import cmc.delta.domain.report.application.port.out.ReportRepositoryPort;
import cmc.delta.domain.report.model.WrongAnswerReport;
import cmc.delta.domain.report.model.enums.ReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportJpaRepository extends JpaRepository<WrongAnswerReport, Long>, ReportRepositoryPort {

	Optional<WrongAnswerReport> findByIdAndUserId(Long id, Long userId);

	Optional<WrongAnswerReport> findFirstByUserIdOrderByIdDesc(Long userId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r
			  from WrongAnswerReport r
			 where r.status = :status
			 order by r.requestedAt asc
		""")
	List<WrongAnswerReport> findPendingForUpdate(@Param("status") ReportStatus status, Pageable pageable);

	@Override
	default Optional<WrongAnswerReport> findLatestByUserId(Long userId) {
		return findFirstByUserIdOrderByIdDesc(userId);
	}

	@Override
	default Optional<WrongAnswerReport> findNextPendingForUpdate() {
		List<WrongAnswerReport> candidates = findPendingForUpdate(ReportStatus.PENDING, PageRequest.of(0, 1));
		return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
	}
}
