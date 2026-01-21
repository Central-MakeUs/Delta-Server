package cmc.delta.domain.user.application.port.in.dto;

public record ProfileImageUploadCommand(
	byte[] bytes,
	String contentType,
	String originalFilename
) {
}
