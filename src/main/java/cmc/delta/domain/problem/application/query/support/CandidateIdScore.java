package cmc.delta.domain.problem.application.query.support;

import java.math.BigDecimal;

public record CandidateIdScore(
	String id,
	BigDecimal score
) { }
