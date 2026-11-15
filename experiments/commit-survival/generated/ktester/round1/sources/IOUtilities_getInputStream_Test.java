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
import java.util.Random;

@ExtendWith(MockitoExtension.class)
class IOUtilities_getInputStream_Test {

    @Mock
    private URLConnection mockConnection;

    /**
     * Returns a stream that yields the gzip‑compressed representation of {@code data}.
     */
    private InputStream gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Returns a stream that yields the deflate‑compressed representation of {@code data}.
     */
    private InputStream deflateCompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(data);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Generates a deterministic pseudo‑random byte array of the given size.
     */
    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(0xDEADBEEF).nextBytes(bytes);
        return bytes;
    }

    /**
     * When the connection has no content‑encoding header the method should simply
     * wrap the original InputStream in a BufferedInputStream.
     */
    @Test
    void getInputStream_withoutEncoding_returnsBufferedStream() throws IOException {
        // given
        byte[] payload = "plain text".getBytes();
        InputStream raw = new ByteArrayInputStream(payload);
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn(null);
        // when
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // then
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream when no encoding is specified");
        // reading the data should give the original payload
        byte[] read = result.readAllBytes();
        assertArrayEquals(payload, read);
    }

    /**
     * When the connection reports "gzip" (or "x-gzip") the method must wrap the
     * underlying stream in a GZIPInputStream and then in a BufferedInputStream.
     */
    @Test
    void getInputStream_withGzipEncoding_returnsGzipBufferedStream() throws IOException {
        // given – create gzip‑compressed bytes
        byte[] original = "gzip compressed".getBytes();
        ByteArrayOutputStream gzipBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gzipBytes)) {
            gos.write(original);
        }
        InputStream raw = new ByteArrayInputStream(gzipBytes.toByteArray());
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        // when
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // then
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream for gzip encoding");
        // The underlying stream should be a GZIPInputStream – we can verify by reading
        // the data and checking it matches the original uncompressed payload.
        byte[] read = result.readAllBytes();
        assertArrayEquals(original, read);
    }

    /**
     * When the connection reports "deflate" the method must wrap the underlying
     * stream in an InflaterInputStream and then in a BufferedInputStream.
     */
    @Test
    void getInputStream_withDeflateEncoding_returnsDeflateBufferedStream() throws IOException {
        // given – create deflate‑compressed bytes
        byte[] original = "deflate compressed".getBytes();
        ByteArrayOutputStream deflateBytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(deflateBytes)) {
            dos.write(original);
        }
        InputStream raw = new ByteArrayInputStream(deflateBytes.toByteArray());
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        // when
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // then
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream for deflate encoding");
        // Verify that reading yields the original uncompressed data.
        byte[] read = result.readAllBytes();
        assertArrayEquals(original, read);
    }

    /**
     * The method should treat the alternative gzip token "x-gzip" the same as "gzip".
     */
    @Test
    void getInputStream_withXGzipEncoding_returnsGzipBufferedStream() throws IOException {
        // given – gzip payload
        byte[] original = "x-gzip payload".getBytes();
        ByteArrayOutputStream gzipBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gzipBytes)) {
            gos.write(original);
        }
        InputStream raw = new ByteArrayInputStream(gzipBytes.toByteArray());
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("x-gzip");
        // when
        InputStream result = IOUtilities.getInputStream(mockConnection);
        // then
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream for x-gzip encoding");
        assertArrayEquals(original, result.readAllBytes());
    }

    @Test
    void getInputStream_unknownEncoding_returnsPlainBufferedStream() throws IOException {
        byte[] payload = "Some data".getBytes();
        InputStream raw = new ByteArrayInputStream(payload);
        when(mockConnection.getInputStream()).thenReturn(raw);
        // unsupported
        when(mockConnection.getContentEncoding()).thenReturn("compress");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Unsupported encoding should result in a plain BufferedInputStream");
        assertArrayEquals(payload, result.readAllBytes());
    }

    @Test
    void getInputStream_identityEncoding_returnsPlainBufferedStream() throws IOException {
        byte[] payload = "Identity payload".getBytes();
        InputStream raw = new ByteArrayInputStream(payload);
        when(mockConnection.getInputStream()).thenReturn(raw);
        // standard no‑op
        when(mockConnection.getContentEncoding()).thenReturn("identity");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream, "Identity encoding should result in a plain BufferedInputStream");
        assertArrayEquals(payload, result.readAllBytes());
    }

    @Test
    void getInputStream_emptyGzipStream_returnsEmptyBufferedStream() throws IOException {
        // Empty payload compressed with gzip (valid empty gzip stream)
        InputStream raw = gzipCompress(new byte[0]);
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        assertEquals(-1, result.read(), "Empty gzip stream should immediately return EOF");
    }

    @Test
    void getInputStream_emptyDeflateStream_returnsEmptyBufferedStream() throws IOException {
        // Empty payload compressed with deflate (valid empty deflate stream)
        InputStream raw = deflateCompress(new byte[0]);
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        assertEquals(-1, result.read(), "Empty deflate stream should immediately return EOF");
    }

    @Test
    void getInputStream_largeGzipPayload_isDecompressedCorrectly() throws IOException {
        // 1 MiB
        byte[] original = randomBytes(1_048_576);
        InputStream raw = gzipCompress(original);
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        byte[] read = result.readAllBytes();
        assertArrayEquals(original, read, "Large gzip payload must be fully restored");
    }

    @Test
    void getInputStream_largeDeflatePayload_isDecompressedCorrectly() throws IOException {
        // 1 MiB
        byte[] original = randomBytes(1_048_576);
        InputStream raw = deflateCompress(original);
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        byte[] read = result.readAllBytes();
        assertArrayEquals(original, read, "Large deflate payload must be fully restored");
    }

    @Test
    void getInputStream_connectionThrowsIOException_propagatesException() throws IOException {
        when(mockConnection.getInputStream()).thenThrow(new IOException("boom"));
        when(mockConnection.getContentEncoding()).thenReturn(null);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.getInputStream(mockConnection), "IOException from URLConnection.getInputStream should be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void getInputStream_nullInputStream_throwsNullPointerException() {
        when(mockConnection.getInputStream()).thenReturn(null);
        when(mockConnection.getContentEncoding()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> IOUtilities.getInputStream(mockConnection), "Null InputStream should cause NullPointerException when BufferedInputStream is constructed");
    }

    @Test
    void getInputStream_invalidGzipData_throwsZipExceptionOnRead() throws IOException {
        // Payload that is NOT a valid gzip stream
        InputStream raw = new ByteArrayInputStream("not a gzip payload".getBytes());
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        // Reading should trigger a ZipException (subclass of IOException)
        assertThrows(ZipException.class, result::readAllBytes, "Reading invalid gzip data must raise ZipException");
    }

    @Test
    void getInputStream_invalidDeflateData_throwsIOExceptionOnRead() throws IOException {
        // Payload that is NOT a valid deflate stream
        InputStream raw = new ByteArrayInputStream("not a deflate payload".getBytes());
        when(mockConnection.getInputStream()).thenReturn(raw);
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        // InflaterInputStream wraps the underlying IOException; we assert IOException.
        IOException ex = assertThrows(IOException.class, result::readAllBytes, "Reading invalid deflate data must raise IOException");
        // The cause is typically a DataFormatException; we verify it exists.
        assertNotNull(ex.getCause(), "IOException should have a cause (DataFormatException)");
    }

    @Test
    void getInputStream_underlyingIOException_gzip_propagatesOnRead() throws IOException {
        // InputStream that throws on any read operation
        InputStream failing = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("read failure");
            }
        };
        when(mockConnection.getInputStream()).thenReturn(failing);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        IOException ex = assertThrows(IOException.class, result::readAllBytes, "IOException from underlying stream should propagate through GZIPInputStream");
        assertEquals("read failure", ex.getMessage());
    }

    @Test
    void getInputStream_underlyingIOException_deflate_propagatesOnRead() throws IOException {
        // InputStream that throws on any read operation
        InputStream failing = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("read failure");
            }
        };
        when(mockConnection.getInputStream()).thenReturn(failing);
        when(mockConnection.getContentEncoding()).thenReturn("deflate");
        InputStream result = IOUtilities.getInputStream(mockConnection);
        assertNotNull(result);
        assertTrue(result instanceof BufferedInputStream);
        IOException ex = assertThrows(IOException.class, result::readAllBytes, "IOException from underlying stream should propagate through InflaterInputStream");
        assertEquals("read failure", ex.getMessage());
    }
}
