package cmc.delta.domain.problem.application.support.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemCreateAssembler {

	private static final String FALLBACK_PROBLEM_MARKDOWN = "(문제 텍스트 없음)";

	private final ProblemCreateScanValidator scanValidator;

	public Problem assemble(
		User userRef,
		ProblemScan scan,
		Unit finalUnit,
		ProblemType finalType,
		CreateWrongAnswerCardCommand command
	) {
		RenderMode renderMode = extractRenderMode(scan);
		String problemMarkdown = extractProblemMarkdown(scan);

		return Problem.create(
			userRef,
			scan,
			finalUnit,
			finalType,
			renderMode,
			problemMarkdown,
			command.answerFormat(),
			command.answerValue(),
			command.answerChoiceNo(),
			command.solutionText()
		);
	}

	private RenderMode extractRenderMode(ProblemScan scan) {
		RenderMode renderMode = scan.getRenderMode();
		if (renderMode == null) {
			throw new ProblemStateException(ErrorCode.PROBLEM_SCAN_RENDER_MODE_MISSING);
		}
		return renderMode;
	}

	private String extractProblemMarkdown(ProblemScan scan) {
		String ocrText = scan.getOcrPlainText();
		if (ocrText == null) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}

		String trimmed = ocrText.trim();
		if (trimmed.isEmpty()) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}

		return ocrText;
	}
}
