package cmc.delta.domain.problem.adapter.out.persistence.scan;

import cmc.delta.domain.problem.model.scan.ProblemScanGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanGroupRepository extends JpaRepository<ProblemScanGroup, Long> {}
