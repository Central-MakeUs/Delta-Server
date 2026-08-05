package cmc.delta.domain.report.adapter.out.persistence;

import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.report.application.dto.ReportSourceSignature;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import cmc.delta.domain.report.application.port.out.ReportSourcePort;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 리포트 관점의 오답 읽기 뷰. problem 테이블을 조회하지만 problem 애그리거트를 변경하지 않는다.
 */
public interface ReportSourceRepository extends Repository<Problem, Long>, ReportSourcePort {

	long countByUser_Id(Long userId);

	long countByUser_IdAndCompletedAtIsNotNull(Long userId);

	@Query("select max(p.updatedAt) from Problem p where p.user.id = :userId")
	LocalDateTime findLastUpdatedAt(@Param("userId")
	Long userId);

	@Query("""
			select new cmc.delta.domain.report.application.dto.WrongAnswerSample(
				u.name, t.name, p.problemMarkdown, p.memoText)
			  from Problem p
			  join p.finalUnit u
			  join p.finalType t
			 where p.user.id = :userId
			 order by case when p.completedAt is null then 0 else 1 end asc, p.createdAt desc
		""")
	List<WrongAnswerSample> findSamples(@Param("userId")
	Long userId, Pageable pageable);

	@Override
	default ReportSourceSignature loadSignature(Long userId) {
		return new ReportSourceSignature(
			countByUser_Id(userId),
			countByUser_IdAndCompletedAtIsNotNull(userId),
			findLastUpdatedAt(userId));
	}

	@Override
	default List<WrongAnswerSample> loadSamples(Long userId, int limit) {
		return findSamples(userId, PageRequest.of(0, limit));
	}
}
