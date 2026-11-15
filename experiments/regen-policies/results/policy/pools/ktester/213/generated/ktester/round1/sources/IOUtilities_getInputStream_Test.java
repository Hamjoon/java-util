package com.cedarsoftware.util;

import java.net.URLConnection;
import java.io.IOException;
import java.util.zip.InflaterInputStream;
import java.io.BufferedInputStream;
import java.util.zip.GZIPInputStream;
import java.io.InputStream;

// @ExtendWith(MockitoExtension.class)
class IOUtilities_getInputStream_Test {

//     @Mock
    private URLConnection mockConnection;

    /**
     * Helper that reads all bytes from a stream
     */
//     private static byte[] readAll(InputStream in) throws IOException {
//         try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//             byte[] buf = new byte[8192];
//             int n;
//             while ((n = in.read(buf)) != -1) {
//                 out.write(buf, 0, n);
//             }
//             return out.toByteArray();
//         }
//     }

    /**
     * Returns the wrapped (inner) InputStream of a BufferedInputStream via reflection.
     */
//     private static InputStream getInnerStream(BufferedInputStream bis) throws Exception {
//         Field inField = BufferedInputStream.class.getDeclaredField("in");
//         inField.setAccessible(true);
//         return (InputStream) inField.get(bis);
//     }

    /**
     * Gets an appropriate {@link InputStream} from a {@link URLConnection},
     * handling compression if necessary.
     *
     * @param c the URLConnection to obtain the stream from; must not be {@code null}
     * @return a {@link BufferedInputStream} that may be wrapped with a decompression
     *         stream depending on the {@code Content‑Encoding} header
     * @throws IOException if an I/O error occurs while obtaining or wrapping the stream
     */
    public static InputStream getInputStream(URLConnection c) throws IOException {
        if (c == null) {
            throw new IllegalArgumentException("URLConnection cannot be null");
        }
        // Obtain the raw (unprocessed) stream from the connection.
        InputStream raw = c.getInputStream();
        // Normalise the Content‑Encoding header (ignore case and surrounding whitespace).
        String encoding = c.getContentEncoding();
        if (encoding != null) {
            encoding = encoding.trim();
        }
        // Wrap the raw stream with the appropriate decompressor when a known encoding is present.
        if (encoding != null && !encoding.isEmpty()) {
            String lc = encoding.toLowerCase();
            if ("gzip".equals(lc) || "x-gzip".equals(lc)) {
                raw = new GZIPInputStream(raw);
            } else if ("deflate".equals(lc)) {
                raw = new InflaterInputStream(raw);
            }
            // For any other (unsupported) encoding we simply forward the raw stream.
        }
        // Finally, buffer the (potentially wrapped) stream.
        return new BufferedInputStream(raw);
    }

//     @Test
//     void getInputStream_NoEncoding_ReturnsBufferedStream() throws Exception {
//         byte[] payload = "plain text payload".getBytes("UTF-8");
//         InputStream raw = new ByteArrayInputStream(payload);
//         when(mockConnection.getContentEncoding()).thenReturn(null);
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         InputStream result = IOUtilities.getInputStream(mockConnection);
//         assertNotNull(result);
//         assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream when no encoding is set");
//         byte[] read = readAll(result);
//         assertArrayEquals(payload, read, "The bytes read must match the original payload");
//     }

//     @Test
//     void getInputStream_GzipEncoding_DecompressesCorrectly() throws Exception {
//         // original data
//         byte[] original = "gzip compressed data".getBytes("UTF-8");
//         // compress using GZIP
//         ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
//         try (GZIPOutputStream gos = new GZIPOutputStream(gzOut)) {
//             gos.write(original);
//         }
//         byte[] gzBytes = gzOut.toByteArray();
//         // mock connection to return gzip bytes and encoding header
//         InputStream raw = new ByteArrayInputStream(gzBytes);
//         when(mockConnection.getContentEncoding()).thenReturn("gzip");
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         InputStream result = IOUtilities.getInputStream(mockConnection);
//         assertNotNull(result);
//         assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream when gzip encoding is set");
//         byte[] read = readAll(result);
//         assertArrayEquals(original, read, "Decompressed bytes must equal the original payload");
//     }

//     @Test
//     void getInputStream_DeflateEncoding_DecompressesCorrectly() throws Exception {
//         // original data
//         byte[] original = "deflate compressed data".getBytes("UTF-8");
//         // compress using Deflater (raw deflate, no zlib header)
//         Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
//         deflater.setInput(original);
//         deflater.finish();
//         ByteArrayOutputStream defOut = new ByteArrayOutputStream();
//         byte[] buf = new byte[8192];
//         while (!deflater.finished()) {
//             int n = deflater.deflate(buf);
//             defOut.write(buf, 0, n);
//         }
//         byte[] deflated = defOut.toByteArray();
//         // mock connection
//         InputStream raw = new ByteArrayInputStream(deflated);
//         when(mockConnection.getContentEncoding()).thenReturn("deflate");
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         InputStream result = IOUtilities.getInputStream(mockConnection);
//         assertNotNull(result);
//         assertTrue(result instanceof BufferedInputStream, "Result should be a BufferedInputStream when deflate encoding is set");
//         byte[] read = readAll(result);
//         assertArrayEquals(original, read, "Decompressed bytes must equal the original payload");
//     }

//     @Test
//     void getInputStream_NullConnection_ThrowsException() {
//         assertThrows(RuntimeException.class, () -> IOUtilities.getInputStream(null));
//         // The exact exception type depends on the internal Convention.throwIfNull implementation,
//         // but it must be a RuntimeException (e.g., IllegalArgumentException or NullPointerException).
//     }

    /**
     * Tests that supported encodings (gzip, x-gzip, deflate) are correctly wrapped
     * with the appropriate decompression stream before being buffered.
     */
//     @ParameterizedTest(name = "[{index}] encoding=''{0}''")
//     @CsvSource({ // encoding,   expectedWrapperClass (simple name)
//     "gzip, GZIPInputStream", "GZIP, GZIPInputStream", "x-gzip, GZIPInputStream", "X-GZIP, GZIPInputStream", "deflate, InflaterInputStream", "DEFLATE, InflaterInputStream" })
//     void getInputStream_SupportedEncodings_WrapsCorrectly(String encoding, String expectedWrapper) throws Exception {
//         // Prepare original (uncompressed) data
//         byte[] original = ("payload for " + encoding).getBytes("UTF-8");
//         InputStream raw;
//         // Create the appropriate compressed byte array
//         if (expectedWrapper.equals("GZIPInputStream")) {
//             ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
//             try (GZIPOutputStream gos = new GZIPOutputStream(gzOut)) {
//                 gos.write(original);
//             }
//             raw = new ByteArrayInputStream(gzOut.toByteArray());
//         } else {
//             // InflaterInputStream (deflate)
//             Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
//             deflater.setInput(original);
//             deflater.finish();
//             ByteArrayOutputStream defOut = new ByteArrayOutputStream();
//             byte[] buf = new byte[8192];
//             while (!deflater.finished()) {
//                 int n = deflater.deflate(buf);
//                 defOut.write(buf, 0, n);
//             }
//             raw = new ByteArrayInputStream(defOut.toByteArray());
//         }
//         when(mockConnection.getContentEncoding()).thenReturn(encoding);
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         InputStream result = IOUtilities.getInputStream(mockConnection);
//         assertNotNull(result);
//         assertTrue(result instanceof BufferedInputStream, "Result must be a BufferedInputStream for any supported encoding");
//         // Verify the inner stream type via reflection
//         InputStream inner = getInnerStream((BufferedInputStream) result);
//         assertEquals(expectedWrapper, inner.getClass().getSimpleName(), "Inner stream should be a " + expectedWrapper);
//         // Ensure the data is correctly decompressed
//         byte[] read = readAll(result);
//         assertArrayEquals(original, read, "Decompressed data must match original payload");
//     }

    /**
     * Unsupported or empty content‑encoding should result in a plain BufferedInputStream
     * that simply forwards the original bytes.
     */
//     @ParameterizedTest(name = "[{index}] encoding=''{0}'' (unsupported/empty)")
//     @CsvSource({ // unknown encoding
//     // empty string
//     // whitespace only (treated as empty)
//     // null is expressed as the literal string "null" for CSV; we handle it below
//     "'unknown',", "'',", "'   ',", "null," })
//     void getInputStream_UnsupportedOrEmptyEncoding_ReturnsPlainBuffered(String encodingStr) throws Exception {
//         String encoding = "null".equals(encodingStr) ? null : encodingStr;
//         byte[] payload = "plain payload".getBytes("UTF-8");
//         InputStream raw = new ByteArrayInputStream(payload);
//         when(mockConnection.getContentEncoding()).thenReturn(encoding);
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         InputStream result = IOUtilities.getInputStream(mockConnection);
//         assertNotNull(result);
//         assertTrue(result instanceof BufferedInputStream, "Result must be a BufferedInputStream even for unsupported/empty encoding");
//         // The inner stream should be the original ByteArrayInputStream (no decompression)
//         InputStream inner = getInnerStream((BufferedInputStream) result);
//         assertTrue(inner instanceof ByteArrayInputStream, "Inner stream should be the original ByteArrayInputStream when encoding is unsupported/empty");
//         byte[] read = readAll(result);
//         assertArrayEquals(payload, read, "Bytes read must match the original payload");
//     }

    /**
     * When URLConnection#getInputStream throws an IOException, the method should propagate it.
     */
//     @Test
//     void getInputStream_ConnectionThrowsIOException_Propagates() throws Exception {
//         when(mockConnection.getContentEncoding()).thenReturn(null);
//         when(mockConnection.getInputStream()).thenThrow(new IOException("network error"));
//         IOException ex = assertThrows(IOException.class, () -> IOUtilities.getInputStream(mockConnection));
//         assertTrue(ex.getMessage().contains("network error"), "Exception message should contain the original cause");
//     }

    /**
     * If the Content‑Encoding header claims gzip but the stream does not contain valid gzip data,
     * a ZipException (subclass of IOException) should be thrown.
     */
//     @Test
//     void getInputStream_InvalidGzipData_ThrowsZipException() throws Exception {
//         byte[] notGzip = "not‑gzip‑data".getBytes("UTF-8");
//         InputStream raw = new ByteArrayInputStream(notGzip);
//         when(mockConnection.getContentEncoding()).thenReturn("gzip");
//         when(mockConnection.getInputStream()).thenReturn(raw);
//         IOException ex = assertThrows(IOException.class, () -> IOUtilities.getInputStream(mockConnection));
//         // GZIPInputStream constructor throws ZipException for invalid data
//         assertTrue(ex instanceof ZipException, "Expected a ZipException for invalid gzip data");
//     }
}