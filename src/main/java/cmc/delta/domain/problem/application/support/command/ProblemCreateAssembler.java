package cmc.delta.domain.problem.application.support.command;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
	import cmc.delta.domain.problem.application.exception.ProblemStateException;
	import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
	import cmc.delta.domain.problem.model.enums.AnswerFormat;
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

		public Problem assemble(
			User userRef,
			ProblemScan scan,
			String originalStorageKey,
			Unit finalUnit,
			ProblemType finalType,
			CreateWrongAnswerCardCommand command) {
			RenderMode renderMode = requireRenderMode(scan);
			String problemMarkdown = resolveProblemMarkdown(scan);
			ProblemCreateValues values = buildCreateValues(command);
			return Problem.create(
				userRef,
				scan,
				originalStorageKey,
				finalUnit,
				finalType,
				renderMode,
				problemMarkdown,
				values.answerFormat(),
				values.answerValue(),
				values.answerChoiceNo(),
				values.memoText());
		}

	private RenderMode requireRenderMode(ProblemScan scan) {
		RenderMode renderMode = scan.getRenderMode();
		if (renderMode == null) {
			throw new ProblemStateException(ErrorCode.PROBLEM_SCAN_RENDER_MODE_MISSING);
		}
		return renderMode;
	}

	private String resolveProblemMarkdown(ProblemScan scan) {
		String ocrText = scan.getOcrPlainText();
		if (ocrText == null) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}
		if (ocrText.trim().isEmpty()) {
			return FALLBACK_PROBLEM_MARKDOWN;
		}
		return ocrText;
	}

	private ProblemCreateValues buildCreateValues(CreateWrongAnswerCardCommand command) {
		return new ProblemCreateValues(
			command.answerFormat(),
			command.answerValue(),
			command.answerChoiceNo(),
			command.memoText());
	}

	private record ProblemCreateValues(
		AnswerFormat answerFormat,
		String answerValue,
		Integer answerChoiceNo,
		String memoText) {
	}
}
