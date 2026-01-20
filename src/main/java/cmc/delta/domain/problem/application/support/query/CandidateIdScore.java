package cmc.delta.domain.problem.application.support.query;

import java.math.BigDecimal;

public record CandidateIdScore(
	String id,
	BigDecimal score
) { }
