package cmc.delta.domain.problem.application.support.query;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnitSubjectResolver {

	private final UnitJpaRepository unitRepository;

	@Transactional(readOnly = true)
	public SubjectInfo resolveByUnitId(String unitId) {
		if (unitId == null || unitId.isBlank()) {
			return SubjectInfo.empty();
		}

		Optional<Unit> unitOpt = unitRepository.findById(unitId);
		if (unitOpt.isEmpty()) {
			return SubjectInfo.empty();
		}

		Unit root = findRoot(unitOpt.get());
		return new SubjectInfo(root.getId(), root.getName());
	}

	private Unit findRoot(Unit unit) {
		Unit cur = unit;
		while (cur.getParent() != null) {
			cur = cur.getParent();
		}
		return cur;
	}
}
