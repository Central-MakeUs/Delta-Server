package cmc.delta.domain.problem.application.port.out.ai.dto;

public record ProblemAiSolveResult(
	String solutionLatex,
	String solutionText) {
}
