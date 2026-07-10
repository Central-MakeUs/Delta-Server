package cmc.delta.domain.version.model;

import cmc.delta.domain.version.application.exception.VersionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SemVersion(int major, int minor, int patch) implements Comparable<SemVersion> {

	private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

	private static final int MAJOR_GROUP = 1;
	private static final int MINOR_GROUP = 2;
	private static final int PATCH_GROUP = 3;

	public static SemVersion parse(String value) {
		if (value == null) {
			throw VersionException.invalidVersion();
		}

		String normalized = value.strip();
		Matcher matcher = VERSION_PATTERN.matcher(normalized);
		if (!matcher.matches()) {
			throw VersionException.invalidVersion();
		}

		try {
			return new SemVersion(
				Integer.parseInt(matcher.group(MAJOR_GROUP)),
				Integer.parseInt(matcher.group(MINOR_GROUP)),
				Integer.parseInt(matcher.group(PATCH_GROUP)));
		} catch (NumberFormatException ex) {
			throw VersionException.invalidVersion();
		}
	}

	@Override
	public int compareTo(SemVersion other) {
		int majorCompare = Integer.compare(major, other.major);
		if (majorCompare != 0) {
			return majorCompare;
		}

		int minorCompare = Integer.compare(minor, other.minor);
		if (minorCompare != 0) {
			return minorCompare;
		}

		return Integer.compare(patch, other.patch);
	}
}
