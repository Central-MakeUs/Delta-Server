package cmc.delta.domain.problem.application.port.in.support;

public record UploadFile(
	byte[] bytes,
	String contentType,
	String originalFilename
) {
	public boolean isEmpty() {
		return bytes == null || bytes.length == 0;
	}
}
