package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_uncompressBytes_2_Test {

    /**
     * Helper that returns a GZIP‑compressed representation of the supplied text.
     */
    private static byte[] gzipCompress(String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    @Test
    void uncompressBytes_nullArray_throwsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null, 0, 0));
    }

    @Test
    void uncompressBytes_notGzipped_returnsSameArray() {
        byte[] data = "plain text".getBytes(StandardCharsets.UTF_8);
        // The method should detect that the array is not gzipped and simply return it.
        byte[] result = IOUtilities.uncompressBytes(data, 0, data.length);
        assertSame(data, result, "When input is not gzipped the original array must be returned");
    }

    @Test
    void uncompressBytes_gzipped_returnsDecompressed() throws IOException {
        String original = "The quick brown fox jumps over the lazy dog";
        byte[] compressed = gzipCompress(original);
        // Call the method with exact offset/length that cover the whole compressed payload.
        byte[] result = IOUtilities.uncompressBytes(compressed, 0, compressed.length);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), result, "Decompressed bytes should match the original payload");
    }

    @Test
    void uncompressBytes_gzippedWithOffsetAndLength_returnsDecompressed() throws IOException {
        String original = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        byte[] compressed = gzipCompress(original);
        // Surround the compressed payload with unrelated bytes to test offset/len handling.
        byte[] prefix = new byte[] { 0x01, 0x02, 0x03 };
        byte[] suffix = new byte[] { 0x0A, 0x0B };
        int offset = prefix.length;
        int len = compressed.length;
        byte[] wrapper = new byte[prefix.length + compressed.length + suffix.length];
        System.arraycopy(prefix, 0, wrapper, 0, prefix.length);
        System.arraycopy(compressed, 0, wrapper, offset, compressed.length);
        System.arraycopy(suffix, 0, wrapper, offset + compressed.length, suffix.length);
        byte[] result = IOUtilities.uncompressBytes(wrapper, offset, len);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), result, "Method must correctly honour offset and length when decompressing");
    }

    @Test
    void uncompressBytes_nonGzippedWithOffset_returnsSameArrayInstance() {
        byte[] data = new byte[] { 10, 20, 30, 40, 50 };
        // offset 1, len 3 -> sub‑range 20,30,40 but method should still return the original array
        byte[] result = IOUtilities.uncompressBytes(data, 1, 3);
        assertSame(data, result, "Non‑gzipped data must be returned unchanged even when offset/len are supplied");
    }

    @Test
    void uncompressBytes_emptyArray_returnsSameArray() {
        byte[] empty = new byte[0];
        byte[] result = IOUtilities.uncompressBytes(empty, 0, 0);
        assertSame(empty, result, "Empty array should be returned unchanged");
    }

    @Test
    void uncompressBytes_gzippedTruncatedLength_throwsRuntimeException() throws IOException {
        String payload = "Hello World";
        byte[] compressed = gzipCompress(payload);
        // truncate 5 bytes from the end
        int truncatedLen = compressed.length - 5;
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(compressed, 0, truncatedLen), "Truncated gzip stream should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_gzippedOffsetInsideStream_throwsRuntimeException() throws IOException {
        String payload = "Hello World";
        byte[] compressed = gzipCompress(payload);
        // start 5 bytes into the gzip header (invalid)
        int offset = 5;
        int len = compressed.length - offset;
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(compressed, offset, len), "Offset inside gzip header should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_invalidOffsetLength_throwsIndexOutOfBoundsException() {
        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        // offset + len exceeds array length
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtilities.uncompressBytes(data, 5, 6), "offset+len beyond array length must throw IndexOutOfBoundsException");
        // negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtilities.uncompressBytes(data, -1, 3), "Negative offset must throw IndexOutOfBoundsException");
        // negative length
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtilities.uncompressBytes(data, 0, -2), "Negative length must throw IndexOutOfBoundsException");
    }

    @Test
    void uncompressBytes_zeroLengthGzippedInput_throwsRuntimeException() throws IOException {
        byte[] compressed = gzipCompress("Hello World");
        // length 0 -> empty sub‑stream, but isGzipped sees the full array as gzipped
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(compressed, 0, 0), "Zero length with gzipped source should result in RuntimeException");
    }

    @Test
    void uncompressBytes_offsetAtEndWithGzippedData_throwsRuntimeException() throws IOException {
        byte[] compressed = gzipCompress("Hello World");
        // point to the end of the array
        int offset = compressed.length;
        int len = 0;
        // isGzipped will still return true because it checks the whole original array
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(compressed, offset, len), "Offset at end with zero length should cause RuntimeException");
    }
}
