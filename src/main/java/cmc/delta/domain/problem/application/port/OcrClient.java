package cmc.delta.domain.problem.application.port;

public interface OcrClient {
	OcrResult recognizeMath(byte[] imageBytes, String filename);
}
