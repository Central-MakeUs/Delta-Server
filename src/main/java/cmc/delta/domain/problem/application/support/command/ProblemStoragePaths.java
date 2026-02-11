package cmc.delta.domain.problem.application.support.command;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ProblemStoragePaths {

	public static final String ORIGINAL_DIR = "problem/original";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private ProblemStoragePaths() {
	}

	public static String buildOriginalDirectory(Clock clock, Long userId) {
		String datePath = LocalDate.now(clock).format(DATE_FORMAT);
		return ORIGINAL_DIR + "/" + datePath + "/" + userId;
	}
}
