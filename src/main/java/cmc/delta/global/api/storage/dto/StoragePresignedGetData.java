package cmc.delta.global.api.storage.dto;

public record StoragePresignedGetData(
	String url,
	int expiresInSeconds
) {}