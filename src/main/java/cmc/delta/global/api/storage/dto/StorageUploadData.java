package cmc.delta.global.api.storage.dto;

public record StorageUploadData(
	String storageKey,
	String viewUrl,
	String contentType,
	long sizeBytes,
	Integer width,
	Integer height
) {}