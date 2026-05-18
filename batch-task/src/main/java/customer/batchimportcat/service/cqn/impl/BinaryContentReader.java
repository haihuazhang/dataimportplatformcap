package customer.batchimportcat.service.cqn.impl;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;

final class BinaryContentReader {
    private BinaryContentReader() {
    }

    static byte[] readRequired(Object value, String errorMessage) {
        byte[] content = readNullable(value, errorMessage);
        if (content == null) {
            throw new IllegalStateException(errorMessage);
        }
        return content;
    }

    static byte[] readNullable(Object value, String errorMessage) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof InputStream inputStream) {
                try (inputStream) {
                    return inputStream.readAllBytes();
                }
            }
            if (value instanceof Blob blob) {
                try (InputStream inputStream = blob.getBinaryStream()) {
                    return inputStream.readAllBytes();
                }
            }
            if (value instanceof ByteBuffer byteBuffer) {
                ByteBuffer duplicate = byteBuffer.asReadOnlyBuffer();
                byte[] bytes = new byte[duplicate.remaining()];
                duplicate.get(bytes);
                return bytes;
            }
            throw new IllegalStateException(errorMessage + " Unsupported binary content type: "
                    + value.getClass().getName() + ".");
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }
}
