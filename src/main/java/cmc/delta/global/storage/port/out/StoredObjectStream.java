package cmc.delta.global.storage.port.out;

import java.io.InputStream;

public record StoredObjectStream(InputStream stream, long contentLength) {
}
