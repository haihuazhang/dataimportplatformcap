package customer.batchimportcat.service.cqn.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

import javax.sql.rowset.serial.SerialBlob;

import org.junit.jupiter.api.Test;

class BinaryContentReaderTest {
    @Test
    void readNullableSupportsInputStream() {
        byte[] content = new byte[] { 1, 2, 3 };

        byte[] actual = BinaryContentReader.readNullable(new ByteArrayInputStream(content), "read failed");

        assertArrayEquals(content, actual);
    }

    @Test
    void readNullableSupportsByteArray() {
        byte[] content = new byte[] { 1, 2, 3 };

        byte[] actual = BinaryContentReader.readNullable(content, "read failed");

        assertArrayEquals(content, actual);
    }

    @Test
    void readNullableSupportsBlob() throws Exception {
        byte[] content = new byte[] { 4, 5, 6 };

        byte[] actual = BinaryContentReader.readNullable(new SerialBlob(content), "read failed");

        assertArrayEquals(content, actual);
    }

    @Test
    void readNullableSupportsByteBuffer() {
        byte[] content = new byte[] { 7, 8, 9 };

        byte[] actual = BinaryContentReader.readNullable(ByteBuffer.wrap(content), "read failed");

        assertArrayEquals(content, actual);
    }

    @Test
    void readRequiredRejectsNullContent() {
        assertThrows(IllegalStateException.class, () -> BinaryContentReader.readRequired(null, "content required"));
    }

    @Test
    void readNullableRejectsUnsupportedContentType() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> BinaryContentReader.readNullable("not-binary", "read failed"));

        assertEquals("read failed Unsupported binary content type: java.lang.String.", exception.getMessage());
    }
}
