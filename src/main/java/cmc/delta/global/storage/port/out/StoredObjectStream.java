package cmc.delta.global.storage.port.out;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/** 사용 후 반드시 close해야 S3 커넥션이 풀로 반환된다. */
public record StoredObjectStream(InputStream stream, long contentLength) implements Closeable {

	@Override
	public void close() throws IOException {
		stream.close();
	}
}
