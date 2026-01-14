package cmc.delta.domain.problem.application.port.ocr;

public interface OcrClient {
	OcrResult recognize(byte[] imageBytes, String filename);
}
