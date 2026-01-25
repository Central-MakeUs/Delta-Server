package cmc.delta.domain.curriculum.application.validation;

import cmc.delta.domain.curriculum.application.exception.ProblemTypeException;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemTypeCommandValidator {

	private static final int MAX_NAME_LEN = 100;

	private final ProblemTypeRepositoryPort problemTypeRepositoryPort;

	public String requireName(String raw) {
		String name = trimToNull(raw);
		if (name == null) {
			throw ProblemTypeException.invalid("name은 필수입니다.");
		}
		validateNameLength(name);
		return name;
	}

	public String requireNameWhenPresent(String raw) {
		// name 필드가 "보내진" 경우, blank를 허용하지 않음
		String name = trimToNull(raw);
		if (name == null) {
			throw ProblemTypeException.invalid("name은 필수입니다.");
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
		if (sortOrder.intValue() < 1) {
			throw ProblemTypeException.invalid("sortOrder는 1 이상이어야 합니다.");
		}
	}

	private void validateNameLength(String name) {
		if (name.length() > MAX_NAME_LEN) {
			throw ProblemTypeException.invalid("name은 100자 이하여야 합니다.");
		}
	}

	private String trimToNull(String v) {
		if (v == null)
			return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}
