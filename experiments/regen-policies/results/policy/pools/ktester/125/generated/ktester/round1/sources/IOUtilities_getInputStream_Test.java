package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URLConnection;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_getInputStream_Test {

    /**
     * Sample data used for all tests.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    @Mock
    private URLConnection mockConnection;

    /**
     * Helper that reads all bytes from the supplied InputStream.
     */
    private static byte[] readAll(InputStream in) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Utility to gzip‑compress a byte array.
     */
    private static byte[] gzip(byte[] src) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(src);
        }
        return bos.toByteArray();
    }

    /**
     * Utility to deflate‑compress a byte array.
     */
    private static byte[] deflate(byte[] src) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(bos)) {
            dos.write(src);
        }
        return bos.toByteArray();
    }

    @Test
    void getInputStream_NoEncoding_ReturnsOriginalBytes() throws Exception {
        // GIVEN – a connection that returns raw (uncompressed) data
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(SAMPLE_DATA));
        when(mockConnection.getContentEncoding()).thenReturn(null);
        // WHEN
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // THEN – the stream should be readable and return the original bytes
        assertNotNull(result, "Resulting InputStream must not be null");
        byte[] actual = readAll(result);
        assertArrayEquals(SAMPLE_DATA, actual, "Bytes read must match the original data");
    }

    @Test
    void getInputStream_GzipEncoding_DecompressesCorrectly() throws Exception {
        // GIVEN – gzip‑compressed payload
        ByteArrayOutputStream gzipBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gzipBytes)) {
            gos.write(SAMPLE_DATA);
        }
        byte[] compressed = gzipBytes.toByteArray();
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        // WHEN
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // THEN – reading from the stream must yield the original (decompressed) data
        assertNotNull(result, "Resulting InputStream must not be null");
        byte[] actual = readAll(result);
        assertArrayEquals(SAMPLE_DATA, actual, "Decompressed bytes must equal the original data");
    }

    @Test
    void getInputStream_DeflateEncoding_DecompressesCorrectly() throws Exception {
        // GIVEN – deflate‑compressed payload
        ByteArrayOutputStream deflateBytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(deflateBytes)) {
            dos.write(SAMPLE_DATA);
        }
        byte[] compressed = deflateBytes.toByteArray();
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        // WHEN
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // THEN – reading from the stream must yield the original (decompressed) data
        assertNotNull(result, "Resulting InputStream must not be null");
        byte[] actual = readAll(result);
        assertArrayEquals(SAMPLE_DATA, actual, "Decompressed bytes must equal the original data");
    }

    @Test
    void getInputStream_UnknownEncoding_IsIgnored() throws Exception {
        // GIVEN – an unknown encoding should be treated as no compression
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(SAMPLE_DATA));
        when(mockConnection.getContentEncoding()).thenReturn("unknown-encoding");
        // WHEN
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // THEN – the stream should simply return the raw data unchanged
        assertNotNull(result, "Resulting InputStream must not be null");
        byte[] actual = readAll(result);
        assertArrayEquals(SAMPLE_DATA, actual, "Bytes should be unchanged for unknown encoding");
    }

    @Test
    void getInputStream_XGzipEncoding_DecompressesCorrectly() throws Exception {
        byte[] compressed = gzip("test".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("x-gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertArrayEquals("test".getBytes(), readAll(result));
    }

    @Test
    void getInputStream_GetInputStreamThrowsIOException_Propagates() throws Exception {
        when(mockConnection.getInputStream()).thenThrow(new IOException("boom"));
        when(mockConnection.getContentEncoding()).thenReturn(null);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.getInputStream(mockConnection));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void getInputStream_GzipUpperCase_IsHandledCaseInsensitively() throws Exception {
        byte[] compressed = gzip("case".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("GZIP");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertArrayEquals("case".getBytes(), readAll(result));
    }

    @Test
    void getInputStream_DeflateMixedCase_IsHandledCaseInsensitively() throws Exception {
        byte[] compressed = deflate("insensitive".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("DeFlAtE");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertArrayEquals("insensitive".getBytes(), readAll(result));
    }

    @Test
    void getInputStream_EmptyStringEncoding_TreatedAsUnknown() throws Exception {
        byte[] payload = "empty".getBytes();
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(payload));
        when(mockConnection.getContentEncoding()).thenReturn("");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertArrayEquals(payload, readAll(result));
    }

    @Test
    void getInputStream_WhitespaceAroundGzipEncoding_NoDecompression() throws Exception {
        byte[] compressed = gzip("space".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        // leading/trailing spaces
        when(mockConnection.getContentEncoding()).thenReturn(" gzip ");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        // Since the encoding does not match, we should get the raw compressed bytes.
        assertArrayEquals(compressed, readAll(result));
    }

    @Test
    void getInputStream_GzipEncoding_WithInvalidData_ThrowsZipExceptionOnRead() throws Exception {
        byte[] notGzipped = "notgz".getBytes();
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(notGzipped));
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        // The GZIPInputStream will throw a ZipException when read is attempted.
        assertThrows(ZipException.class, () -> readAll(result));
    }

    @Test
    void getInputStream_DeflateEncoding_WithInvalidData_ThrowsIOExceptionOnRead() throws Exception {
        byte[] notDeflated = "notdef".getBytes();
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(notDeflated));
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        // InflaterInputStream wraps the underlying IOException (DataFormatException) in an IOException.
        IOException ex = assertThrows(IOException.class, () -> readAll(result));
        assertTrue(ex.getMessage().contains("inflater"));
    }

    @Test
    void getInputStream_ReturnsBufferedInputStream_NoEncoding() throws Exception {
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream("type".getBytes()));
        when(mockConnection.getContentEncoding()).thenReturn(null);
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream regardless of encoding");
    }

    @Test
    void getInputStream_ReturnsBufferedInputStream_GzipEncoding() throws Exception {
        byte[] compressed = gzip("type".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream even when wrapped with GZIPInputStream");
    }

    @Test
    void getInputStream_ReturnsBufferedInputStream_DeflateEncoding() throws Exception {
        byte[] compressed = deflate("type".getBytes());
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(compressed));
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream even when wrapped with InflaterInputStream");
    }

    @Test
    void getInputStream_NullURLConnection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.getInputStream(null));
    }

    @Test
    void getInputStream_ContentEncodingThrowsRuntimeException_Propagates() throws Exception {
        // getContentEncoding throws; getInputStream should not catch it.
        when(mockConnection.getContentEncoding()).thenThrow(new RuntimeException("boom"));
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(SAMPLE_DATA));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.getInputStream(mockConnection));
        assertEquals("boom", ex.getMessage());
    }
}
