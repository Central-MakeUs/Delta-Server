package cmc.delta.domain.problem.application.port.out.scan;

import cmc.delta.domain.problem.model.scan.ProblemScanGroup;

public interface ProblemScanGroupRepositoryPort {

	ProblemScanGroup save(ProblemScanGroup group);
}
