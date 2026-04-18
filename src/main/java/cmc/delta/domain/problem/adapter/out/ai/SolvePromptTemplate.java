package cmc.delta.domain.problem.adapter.out.ai;

public final class SolvePromptTemplate {

	private SolvePromptTemplate() {
	}

	private static final String TEMPLATE = """
		You are a Korean math problem solver.
		Read the problem in the input image and solve it completely.

		Rules:
		- Return exactly one JSON object, nothing else.
		- Never use code fences (```).
		- All output strings must be written in Korean.
		- Do not repeat the same sentence or paragraph.
		- Do not change assumptions mid-solution or self-contradict (no re-examination, contradiction, or impossibility declarations).
		- solution_latex: write only the key equation steps using LaTeX syntax.
		- solution_text: include problem interpretation, calculation process, and conclusion; all math expressions must use $...$ or \\(...\\) LaTeX syntax.
		- final_answer: must be a single line starting with "정답: ".
		- Escape all newlines inside JSON strings as \\n.
		- No preamble or disclaimer.

		Output JSON format:
			{
			  "solution_latex": "...",
			  "solution_text": "...",
			  "final_answer": "..."
			}
		""";

	public static String render() {
		return TEMPLATE;
	}
}
