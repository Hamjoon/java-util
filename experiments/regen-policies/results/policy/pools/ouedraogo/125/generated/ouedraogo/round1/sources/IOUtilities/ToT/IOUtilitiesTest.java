package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

import static org.junit.Assert.*;

public class IOUtilitiesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /* ---------------------------------------------------------------------- */
    /* Helper classes used throughout the test suite                           */
    /* ---------------------------------------------------------------------- */

    /**
     * A very small in‑memory URLConnection.  It allows us to control the
     * InputStream, OutputStream and the {@code Content‑Encoding} header.
     */
    private static class InMemoryURLConnection extends URLConnection {
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private String contentEncoding;

        InMemoryURLConnection(byte[] data, String encoding) {
            super(null);
            this.input = new ByteArrayInputStream(data);
            this.contentEncoding = encoding;
        }

        @Override
        public void connect() { /* no real connection */ }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public String getContentEncoding() {
            return contentEncoding;
        }

        public byte[] getWrittenBytes() {
            return output.toByteArray();
        }
    }

    /**
     * A {@link Closeable} that throws an {@link IOException} when {@code close()}
     * is called.  Used to verify that {@code IOUtilities.close(Closeable)} suppresses
     * the exception.
     */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("bad close");
        }
    }

    /**
     * A {@link Flushable} that throws an {@link IOException} when {@code flush()}
     * is called.  Used to verify that {@code IOUtilities.flush(Flushable)} suppresses
     * the exception.
     */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("bad flush");
        }
    }

    /**
     * Simple {@link TransferCallback} that records every call to
     * {@code bytesTransferred} and can be configured to cancel after a
     * specific number of bytes.
     */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final List<Integer> counts = new ArrayList<>();
        private final int cancelAfter; // cancel after this many total bytes, -1 = never

        private int total = 0;

        RecordingCallback() {
            this(-1);
        }

        RecordingCallback(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            counts.add(count);
            total += count;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter >= 0 && total >= cancelAfter;
        }

        public List<Integer> getCounts() {
            return counts;
        }
    }

    /**
     * An {@link InputStream} that throws an {@link IOException} on the first read.
     * Used to test the error‑handling path of {@code inputStreamToBytes}.
     */
    private static class BadInputStream extends InputStream {
        private boolean firstRead = true;

        @Override
        public int read() throws IOException {
            if (firstRead) {
                firstRead = false;
                throw new IOException("bad read");
            }
            return -1;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. getInputStream() – normal, gzip and deflate handling                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetInputStream_NoCompression() throws Exception {
        byte[] data = "plain data".getBytes("UTF-8");
        InMemoryURLConnection conn = new InMemoryURLConnection(data, null);

        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(data, read);
    }

    @Test
    public void testGetInputStream_GzipCompression() throws Exception {
        byte[] original = "gzip data".getBytes("UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(original);
        }
        byte[] gzipped = baos.toByteArray();

        InMemoryURLConnection conn = new InMemoryURLConnection(gzipped, "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(original, read);
    }

    @Test
    public void testGetInputStream_DeflateCompression() throws Exception {
        byte[] original = "deflate data".getBytes("UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(original);
        }
        byte[] deflated = baos.toByteArray();

        InMemoryURLConnection conn = new InMemoryURLConnection(deflated, "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(original, read);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. transfer(File, URLConnection, TransferCallback)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        File src = tmp.newFile("src.txt");
        byte[] content = "file to conn".getBytes("UTF-8");
        Files.write(src.toPath(), content);

        InMemoryURLConnection conn = new InMemoryURLConnection(new byte[0], null);
        IOUtilities.transfer(src, conn, null);

        assertArrayEquals(content, conn.getWrittenBytes());
    }

    @Test
    public void testTransferFileToURLConnection_WithCallbackAndCancel() throws Exception {
        // 100 KB file, buffer size is 32 KiB => 4 reads
        byte[] data = new byte[100 * 1024];
        new Random().nextBytes(data);
        File src = tmp.newFile("big.bin");
        Files.write(src.toPath(), data);

        InMemoryURLConnection conn = new InMemoryURLConnection(new byte[0], null);
        // Cancel after 40 KiB transferred
        RecordingCallback cb = new RecordingCallback(40 * 1024);
        IOUtilities.transfer(src, conn, cb);

        // Verify that at most 40 KiB were written
        assertTrue(conn.getWrittenBytes().length <= 40 * 1024);
        // Verify that the callback saw the cancel flag
        assertTrue(cb.isCancelled());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. transfer(URLConnection, File, TransferCallback)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile() throws Exception {
        byte[] payload = "conn to file".getBytes("UTF-8");
        InMemoryURLConnection conn = new InMemoryURLConnection(payload, null);
        File dest = tmp.newFile("dest.txt");

        IOUtilities.transfer(conn, dest, null);
        assertArrayEquals(payload, Files.readAllBytes(dest.toPath()));
    }

    @Test
    public void testTransferURLConnectionToFile_WithGzipAndCallback() throws Exception {
        byte[] original = "compressed via gzip".getBytes("UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(original);
        }
        byte[] gzipped = baos.toByteArray();

        InMemoryURLConnection conn = new InMemoryURLConnection(gzipped, "gzip");
        File dest = tmp.newFile("dest2.txt");
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(conn, dest, cb);
        assertArrayEquals(original, Files.readAllBytes(dest.toPath()));
        // At least one callback invocation must have happened
        assertFalse(cb.getCounts().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. transfer(InputStream, File, TransferCallback)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        byte[] data = "stream->file".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        File dest = tmp.newFile("streamFile.txt");

        IOUtilities.transfer(in, dest, null);
        assertArrayEquals(data, Files.readAllBytes(dest.toPath()));
    }

    @Test
    public void testTransferInputStreamToFile_WithCallbackCancel() throws Exception {
        byte[] data = new byte[70 * 1024];
        new Random().nextBytes(data);
        InputStream in = new ByteArrayInputStream(data);
        File dest = tmp.newFile("partial.txt");

        // Cancel after 30 KiB
        RecordingCallback cb = new RecordingCallback(30 * 1024);
        IOUtilities.transfer(in, dest, cb);

        byte[] written = Files.readAllBytes(dest.toPath());
        assertTrue(written.length <= 30 * 1024);
        assertTrue(cb.isCancelled());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. transfer(InputStream, OutputStream, TransferCallback)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToOutputStream() throws Exception {
        byte[] data = "in->out".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RecordingCallback cb = new RecordingCallback();
        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertEquals(Collections.singletonList(data.length), cb.getCounts());
    }

    @Test
    public void testTransferInputStreamToOutputStream_CancelMidway() throws Exception {
        byte[] data = new byte[50 * 1024];
        new Random().nextBytes(data);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Cancel after 20 KiB
        RecordingCallback cb = new RecordingCallback(20 * 1024);
        IOUtilities.transfer(in, out, cb);

        assertTrue(out.size() <= 20 * 1024);
        assertTrue(cb.isCancelled());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. transfer(InputStream, byte[]) – exact‑size read                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToByteArrayExactSize() throws Exception {
        byte[] data = "exact".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] target = new byte[data.length];

        IOUtilities.transfer(in, target);
        assertArrayEquals(data, target);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArrayTooShort() throws Exception {
        byte[] data = "short".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] target = new byte[data.length + 5]; // larger than source

        IOUtilities.transfer(in, target); // should throw IOException
    }

    /* ---------------------------------------------------------------------- */
    /* 7. transfer(InputStream, OutputStream) – simple copy                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToOutputStream_NoCallback() throws Exception {
        byte[] data = "simple copy".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. transfer(File, OutputStream) – flush verification                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream_FlushCalled() throws Exception {
        byte[] data = "file->out".getBytes("UTF-8");
        File src = tmp.newFile("srcFlush.txt");
        Files.write(src.toPath(), data);

        BadFlushable badFlush = new BadFlushable(); // will swallow exception
        OutputStream out = new FilterOutputStream(badFlush) {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                super.write(b, off, len);
            }
        };
        // No exception should propagate from flush()
        IOUtilities.transfer(src, out);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. close() overloads – safe handling of null and exception‑throwing     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloseNullsDoNothing() {
        // All three overloads should accept null without throwing
        IOUtilities.close((Closeable) null);
        IOUtilities.close((XMLStreamReader) null);
        IOUtilities.close((XMLStreamWriter) null);
    }

    @Test
    public void testCloseCloseableWithExceptionIsSuppressed() {
        IOUtilities.close(new BadCloseable()); // should not throw
    }

    @Test
    public void testFlushFlushableWithExceptionIsSuppressed() {
        IOUtilities.flush(new BadFlushable()); // should not throw
    }

    /* ---------------------------------------------------------------------- */
    /* 10. inputStreamToBytes – normal and error path                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytes_Normal() throws Exception {
        byte[] data = "some bytes".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, result);
    }

    @Test
    public void testInputStreamToBytes_OnExceptionReturnsNull() {
        InputStream bad = new BadInputStream();
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. transfer(URLConnection, byte[]) – write bytes to output stream       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionWithByteArray() throws Exception {
        InMemoryURLConnection conn = new InMemoryURLConnection(new byte[0], null);
        byte[] payload = "payload".getBytes("UTF-8");
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. compressBytes / uncompressBytes – round‑trip & edge cases            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompressAndUncompress_ByteArray_RoundTrip() {
        byte[] original = "compress me please".getBytes();
        byte[] compressed = IOUtilities.compressBytes(original);
        assertNotNull(compressed);
        assertTrue(compressed.length < original.length); // compression should shrink

        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testCompressAndUncompress_ByteArray_SubRange() {
        byte[] original = "0123456789ABCDEF".getBytes();
        // compress only the middle 8 bytes
        byte[] compressed = IOUtilities.compressBytes(original, 4, 8);
        byte[] expected = Arrays.copyOfRange(original, 4, 12);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(expected, uncompressed);
    }

    @Test
    public void testUncompressBytes_NonGzippedReturnsOriginal() {
        byte[] data = "plain text".getBytes();
        assertSame(data, IOUtilities.uncompressBytes(data));
    }

    @Test
    public void testCompressBytes_ByteArrayOutputStream_VariousLengths() throws Exception {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.write("short".getBytes());

        FastByteArrayOutputStream dst = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        byte[] uncompressed = IOUtilities.uncompressBytes(dst.toByteArray());
        assertArrayEquals("short".getBytes(), uncompressed);
    }

    @Test
    public void testCompressBytes_FastByteArrayOutputStream_VariousLengths() throws Exception {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.write("a longer string to compress".getBytes());

        FastByteArrayOutputStream dst = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        byte[] uncompressed = IOUtilities.uncompressBytes(dst.toByteArray());
        assertArrayEquals(src.toByteArray(), uncompressed);
    }

    @Test
    public void testCompressBytes_ByteArrayOutputStream_WithEmptyInput() throws Exception {
        ByteArrayOutputStream src = new ByteArrayOutputStream();
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        // Empty input should give a valid GZIP header (minimum 10 bytes)
        assertTrue(dst.size() > 0);
        byte[] uncompressed = IOUtilities.uncompressBytes(dst.toByteArray());
        assertArrayEquals(new byte[0], uncompressed);
    }

    /* ---------------------------------------------------------------------- */
    /* Utility method to read an entire InputStream into a byte[]               */
    /* ---------------------------------------------------------------------- */

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtilities.transfer(is, baos);
        return baos.toByteArray();
    }
}
