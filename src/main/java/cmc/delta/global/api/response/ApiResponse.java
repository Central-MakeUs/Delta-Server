package cmc.delta.global.api.response;

public record ApiResponse<T>(int status, String code, T data, String message) {
}
