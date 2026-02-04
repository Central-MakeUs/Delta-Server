package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class ProblemMonthlyProgressValidator {

	public YearMonth validateAndParse(Integer year, Integer month) {
		if (year == null || month == null) {
			throw new ProblemValidationException("year/month가 필요합니다.");
		}
		try {
			return YearMonth.of(year, month);
		} catch (Exception e) {
			throw new ProblemValidationException("year/month 값이 올바르지 않습니다.");
		}
	}
}
