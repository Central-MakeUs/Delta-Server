package cmc.delta.domain.curriculum.application.validation;

import cmc.delta.domain.curriculum.application.exception.ProblemTypeException;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemTypeCommandValidator {

	private static final int MAX_NAME_LEN = 100;
	private static final int MIN_SORT_ORDER = 1;

	private static final String NAME_REQUIRED_MESSAGE = "name은 필수입니다.";
	private static final String NAME_TOO_LONG_MESSAGE = "name은 " + MAX_NAME_LEN + "자 이하여야 합니다.";
	private static final String SORT_ORDER_MIN_MESSAGE = "sortOrder는 " + MIN_SORT_ORDER + " 이상이어야 합니다.";

	private final ProblemTypeRepositoryPort problemTypeRepositoryPort;

	/** name 필드가 보내진 경우에도 blank는 허용하지 않으므로 필수 검증과 동일하다. */
	public String requireName(String raw) {
		String name = trimToEmptyNull(raw);
		if (name == null) {
			throw ProblemTypeException.invalid(NAME_REQUIRED_MESSAGE);
		}
		validateNameLength(name);
		return name;
	}

	public void ensureNoDuplicateCustomName(Long userId, String name) {
		if (problemTypeRepositoryPort.existsCustomByUserIdAndName(userId, name)) {
			throw ProblemTypeException.duplicateName();
		}
	}

	public void validateSortOrder(Integer sortOrder) {
		if (sortOrder == null)
			return;
		if (sortOrder.intValue() < MIN_SORT_ORDER) {
			throw ProblemTypeException.invalid(SORT_ORDER_MIN_MESSAGE);
		}
	}

	private void validateNameLength(String name) {
		if (name.length() > MAX_NAME_LEN) {
			throw ProblemTypeException.invalid(NAME_TOO_LONG_MESSAGE);
		}
	}

	private String trimToEmptyNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
