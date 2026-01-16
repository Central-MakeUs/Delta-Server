package cmc.delta.domain.problem.api.scan.dto.response;

import java.math.BigDecimal;

public record CandidateResponse(
	String id,
	String name,
	BigDecimal score
) { }
