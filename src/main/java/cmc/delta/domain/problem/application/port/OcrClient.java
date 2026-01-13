package cmc.delta.domain.problem.application.port;

public interface OcrClient {
	OcrResult recognize(byte[] imageBytes, String filename);
}
