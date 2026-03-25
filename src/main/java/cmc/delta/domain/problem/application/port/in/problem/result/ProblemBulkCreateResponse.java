package cmc.delta.domain.problem.application.port.in.problem.result;

import java.util.List;

public record ProblemBulkCreateResponse(List<ProblemCreateResponse> problems) {
}
