package cmc.delta.domain.problem.adapter.out.ai.gemini;

/**
 * JSON 텍스트를 문자열/이스케이프 상태를 추적하며 중괄호 깊이를 스캔한다.
 * 균형이 맞는 객체를 찾으면 그 끝 인덱스를, 못 찾으면 잘린 JSON 복구에 필요한 최종 상태를 돌려준다.
 */
final class JsonObjectScanner {

	static final int NOT_BALANCED = -1;

	private JsonObjectScanner() {}

	record ScanResult(int balancedEndIndex, boolean inString, boolean escaped, int openDepth) {

		boolean isBalanced() {
			return balancedEndIndex >= 0;
		}
	}

	static ScanResult scan(String text, int startIndex, int endExclusive) {
		ScanState state = new ScanState();

		for (int index = startIndex; index < endExclusive; index++) {
			char current = text.charAt(index);

			if (state.inString) {
				consumeStringChar(state, current);
				continue;
			}
			if (consumeStructuralChar(state, current)) {
				return new ScanResult(index, false, false, 0);
			}
		}

		return new ScanResult(NOT_BALANCED, state.inString, state.escaped, state.depth);
	}

	private static void consumeStringChar(ScanState state, char current) {
		if (state.escaped) {
			state.escaped = false;
			return;
		}
		if (current == '\\') {
			state.escaped = true;
			return;
		}
		if (current == '"') {
			state.inString = false;
		}
	}

	// 최상위 객체가 닫혔을 때만 true를 돌려준다.
	private static boolean consumeStructuralChar(ScanState state, char current) {
		if (current == '"') {
			state.inString = true;
			return false;
		}
		if (current == '{') {
			state.depth += 1;
			return false;
		}
		if (current == '}' && state.depth > 0) {
			state.depth -= 1;
			return state.depth == 0;
		}
		return false;
	}

	private static final class ScanState {
		private boolean inString;
		private boolean escaped;
		private int depth;
	}
}
