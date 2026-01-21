package cmc.delta.domain.problem.application.support;

import java.lang.reflect.Field;

public final class ReflectionIds {

	private ReflectionIds() {}

	public static void setId(Object target, long id) {
		setField(target, "id", id);
	}

	public static void setField(Object target, String fieldName, Object value) {
		try {
			Field f = target.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 필드 세팅 실패: " + target.getClass().getSimpleName() + "." + fieldName, e);
		}
	}
}
