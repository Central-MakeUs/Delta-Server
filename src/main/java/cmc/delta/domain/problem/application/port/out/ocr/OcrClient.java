package cmc.delta.domain.problem.application.port.out.ocr;

import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;

public interface OcrClient {
	OcrResult recognize(byte[] imageBytes, String filename);
}
