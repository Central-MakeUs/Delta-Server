package cmc.delta.domain.problem.application.port.out.ocr;

import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import java.io.InputStream;

public interface OcrClient {
	OcrResult recognize(InputStream imageStream, long contentLength, String filename);
}
