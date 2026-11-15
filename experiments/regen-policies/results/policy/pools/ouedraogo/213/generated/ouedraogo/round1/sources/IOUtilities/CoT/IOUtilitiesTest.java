package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * JUnit‑4 test suite for {@link IOUtilities}.
 * Covers normal behaviour, edge cases and exception handling.
 */
public class IOUtilitiesTest {

    // -----------------------------------------------------------------
    // Helper classes
    // -----------------------------------------------------------------
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        private final int cancelAfter;
        private int total = 0;

        RecordingCallback() { this(-1); }
        RecordingCallback(int cancelAfter) { this.cancelAfter = cancelAfter; }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            recorded.write(bytes, 0, count);
            total += count;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter != -1 && total >= cancelAfter;
        }

        byte[] getRecorded() { return recorded.toByteArray(); }
    }

    /** Minimal URLConnection allowing us to inject streams and headers */
    private static class SimpleURLConnection extends URLConnection {
        private InputStream in;
        private OutputStream out;

        protected SimpleURLConnection() { super(null); }

        void setInputStream(InputStream in) { this.in = in; }
        void setOutputStream(OutputStream out) { this.out = out; }

        @Override public void connect() throws IOException { /* no‑op */ }

        @Override public InputStream getInputStream() throws IOException {
            if (in == null) throw new IOException("InputStream not set");
            return in;
        }

        @Override public OutputStream getOutputStream() throws IOException {
            if (out == null) throw new IOException("OutputStream not set");
            return out;
        }

        @Override public String getContentEncoding() {
            return super.getContentEncoding(); // may be set via setRequestProperty
        }
    }

    // -----------------------------------------------------------------
    // 1. getInputStream
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testGetInputStream_NullConnection() throws IOException {
        IOUtilities.getInputStream(null);
    }

    @Test
    public void testGetInputStream_NoEncoding() throws Exception {
        byte[] data = "plain text".getBytes(StandardCharsets.UTF_8);
        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setInputStream(new ByteArrayInputStream(data));
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(data, read);
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws Exception {
        byte[] original = "gzip test".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = IOUtilities.compressBytes(original);
        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setRequestProperty("Content-Encoding", "gzip");
        conn.setInputStream(new ByteArrayInputStream(gzipped));
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] round = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(original, round);
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws Exception {
        // Use the same compressBytes method but with Deflate (not directly exposed)
        // We'll create a Deflate stream manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write("deflate".getBytes(StandardCharsets.UTF_8));
        }
        byte[] deflated = baos.toByteArray();

        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setRequestProperty("Content-Encoding", "deflate");
        conn.setInputStream(new ByteArrayInputStream(deflated));
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] result = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals("deflate".getBytes(StandardCharsets.UTF_8), result);
    }

    // -----------------------------------------------------------------
    // 2. transfer(File, URLConnection, TransferCallback)
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testTransferFile_NullFile() throws Exception {
        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setOutputStream(new ByteArrayOutputStream());
        IOUtilities.transfer((File) null, conn, null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFile_NullConnection() throws Exception {
        File f = Files.createTempFile("tmp", ".txt").toFile();
        IOUtilities.transfer(f, null, null);
    }

    @Test
    public void testTransferFile_EmptyFile() throws Exception {
        File empty = Files.createTempFile("empty", ".txt").toFile();
        empty.deleteOnExit();
        SimpleURLConnection conn = new SimpleURLConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        conn.setOutputStream(out);
        IOUtilities.transfer(empty, conn, null);
        assertEquals(0, out.size());
    }

    @Test
    public void testTransferFile_WithCallbackAndCancel() throws Exception {
        byte[] data = new byte[120_000];
        new Random().nextBytes(data);
        File src = Files.createTempFile("src", ".bin").toFile();
        Files.write(src.toPath(), data);
        SimpleURLConnection conn = new SimpleURLConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        conn.setOutputStream(out);

        RecordingCallback cb = new RecordingCallback(60_000); // cancel after 60k bytes
        IOUtilities.transfer(src, conn, cb);

        assertTrue(cb.isCancelled());
        assertTrue(out.size() < data.length);
    }

    // -----------------------------------------------------------------
    // 3. transfer(URLConnection, File, TransferCallback)
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testTransferConnToFile_NullConn() throws Exception {
        File f = Files.createTempFile("dst", ".txt").toFile();
        IOUtilities.transfer(null, f, null);
    }

    @Test
    public void testTransferConnToFile_Uncompressed() throws Exception {
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setInputStream(new ByteArrayInputStream(payload));
        File dst = Files.createTempFile("dst", ".txt").toFile();
        IOUtilities.transfer(conn, dst, null);
        byte[] read = Files.readAllBytes(dst.toPath());
        assertArrayEquals(payload, read);
    }

    @Test
    public void testTransferConnToFile_GzipHandled() throws Exception {
        byte[] original = "gzip payload".getBytes(StandardCharsets.UTF_8);
        byte[] gz = IOUtilities.compressBytes(original);
        SimpleURLConnection conn = new SimpleURLConnection();
        conn.setRequestProperty("Content-Encoding", "gzip");
        conn.setInputStream(new ByteArrayInputStream(gz));
        File dst = Files.createTempFile("dst", ".txt").toFile();
        IOUtilities.transfer(conn, dst, null);
        assertArrayEquals(original, Files.readAllBytes(dst.toPath()));
    }

    // -----------------------------------------------------------------
    // 4. transfer(InputStream, File, TransferCallback)
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testTransferInputStreamToFile_NullStream() throws Exception {
        File f = Files.createTempFile("out", ".bin").toFile();
        IOUtilities.transfer((InputStream) null, f, null);
    }

    @Test
    public void testTransferInputStreamToFile_SmallData() throws Exception {
        byte[] data = "stream to file".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(data);
        File outFile = Files.createTempFile("out", ".txt").toFile();
        IOUtilities.transfer(in, outFile, null);
        assertArrayEquals(data, Files.readAllBytes(outFile.toPath()));
    }

    // -----------------------------------------------------------------
    // 5. transfer(InputStream, OutputStream, Callback)
    // -----------------------------------------------------------------
    @Test
    public void testTransferStreamToStream_WithCallback() throws Exception {
        byte[] data = new byte[5000];
        new Random().nextBytes(data);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback();
        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertArrayEquals(data, cb.getRecorded());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferStreamToStream_NullIn() throws Exception {
        IOUtilities.transfer(null, new ByteArrayOutputStream(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferStreamToStream_NullOut() throws Exception {
        IOUtilities.transfer(new ByteArrayInputStream(new byte[0]), null, null);
    }

    // -----------------------------------------------------------------
    // 6. transfer(InputStream, byte[])
    // -----------------------------------------------------------------
    @Test
    public void testTransferIntoByteArray() throws Exception {
        byte[] src = "exactly ten".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[src.length];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(src, dest);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferIntoByteArray_NullStream() throws Exception {
        IOUtilities.transfer(null, new byte[10]);
    }

    @Test(expected = EOFException.class)
    public void testTransferIntoByteArray_StreamTooShort() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[2]);
        byte[] dest = new byte[5];
        IOUtilities.transfer(in, dest); // should throw EOFException from DataInputStream.readFully
    }

    // -----------------------------------------------------------------
    // 7. transfer(InputStream, OutputStream)
    // -----------------------------------------------------------------
    @Test
    public void testTransferInToOut_NoCallback() throws Exception {
        byte[] data = "no callback".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    // -----------------------------------------------------------------
    // 8. transfer(File, OutputStream)
    // -----------------------------------------------------------------
    @Test
    public void testTransferFileToOutputStream_FlushCalled() throws Exception {
        byte[] data = "flush test".getBytes(StandardCharsets.UTF_8);
        File src = Files.createTempFile("src", ".txt").toFile();
        Files.write(src.toPath(), data);

        // custom Flushable that records flush calls
        class RecordingFlushable extends OutputStream implements Flushable {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean flushed = false;
            @Override public void write(int b) throws IOException { baos.write(b); }
            @Override public void flush() { flushed = true; }
            byte[] toByteArray() { return baos.toByteArray(); }
        }

        RecordingFlushable rf = new RecordingFlushable();
        IOUtilities.transfer(src, rf);
        assertTrue(rf.flushed);
        assertArrayEquals(data, rf.toByteArray());
    }

    // -----------------------------------------------------------------
    // 9‑13. close / flush helpers (exception swallowing)
    // -----------------------------------------------------------------
    @Test
    public void testCloseXmlStreamReader_ExceptionSwallowed() throws Exception {
        XMLStreamReader reader = new XMLStreamReader() {
            @Override public void close() throws XMLStreamException { throw new XMLStreamException("boom"); }
            // all other methods can be left empty / default
            @Override public Object getProperty(String name) { return null; }
            @Override public int next() { return 0; }
            @Override public void require(int type, String namespace, String name) {}
            @Override public String getElementText() { return null; }
            @Override public int nextTag() { return 0; }
            @Override public boolean hasNext() { return false; }
            @Override public void close() {}
            @Override public String getNamespaceURI(String prefix) { return null; }
            @Override public boolean isStartElement() { return false; }
            @Override public boolean isEndElement() { return false; }
            @Override public boolean isCharacters() { return false; }
            @Override public boolean isWhiteSpace() { return false; }
            @Override public String getLocalName() { return null; }
            @Override public boolean hasName() { return false; }
            @Override public String getPrefix() { return null; }
            @Override public String getNamespaceURI() { return null; }
            @Override public String getVersion() { return null; }
            @Override public boolean isStandalone() { return false; }
            @Override public boolean standaloneSet() { return false; }
            @Override public String getCharacterEncodingScheme() { return null; }
            @Override public String getPITarget() { return null; }
            @Override public String getPIData() { return null; }
            @Override public int getLocation() { return 0; }
        };
        // should not throw
        IOUtilities.close(reader);
    }

    @Test
    public void testCloseCloseable_ExceptionSwallowed() throws Exception {
        Closeable c = () -> { throw new IOException("close fail"); };
        IOUtilities.close(c); // no exception expected
    }

    @Test
    public void testFlushFlushable_ExceptionSwallowed() throws Exception {
        Flushable f = () -> { throw new IOException("flush fail"); };
        IOUtilities.flush(f); // no exception expected
    }

    // -----------------------------------------------------------------
    // 14. inputStreamToBytes
    // -----------------------------------------------------------------
    @Test
    public void testInputStreamToBytes_HappyPath() throws Exception {
        byte[] data = "bytes".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(data);
        assertArrayEquals(data, IOUtilities.inputStreamToBytes(in));
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamToBytes_Null() {
        IOUtilities.inputStreamToBytes(null);
    }

    @Test
    public void testInputStreamToBytes_ReadErrorReturnsNull() throws Exception {
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("boom"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    // -----------------------------------------------------------------
    // 15. transfer(URLConnection, byte[])
    // -----------------------------------------------------------------
    @Test
    public void testTransferURLConnectionBytes() throws Exception {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        SimpleURLConnection conn = new SimpleURLConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        conn.setOutputStream(out);
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, out.toByteArray());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferURLConnectionBytes_NullConn() throws Exception {
        IOUtilities.transfer(null, new byte[0]);
    }

    // -----------------------------------------------------------------
    // 16‑17. compressBytes (stream overloads)
    // -----------------------------------------------------------------
    @Test
    public void testCompressBytes_ByteArrayOutputStream() throws Exception {
        ByteArrayOutputStream src = new ByteArrayOutputStream();
        src.write("stream data".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        byte[] compressed = dst.toByteArray();
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("stream data".getBytes(StandardCharsets.UTF_8), uncompressed);
    }

    @Test
    public void testCompressBytes_FastByteArrayOutputStream() throws Exception {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.write("fast stream".getBytes(StandardCharsets.UTF_8));
        FastByteArrayOutputStream dst = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        byte[] compressed = dst.toByteArray();
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("fast stream".getBytes(StandardCharsets.UTF_8), uncompressed);
    }

    // -----------------------------------------------------------------
    // 18‑19. compressBytes (byte[] overloads)
    // -----------------------------------------------------------------
    @Test
    public void testCompressBytes_ByteArray() {
        byte[] data = "byte array".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = IOUtilities.compressBytes(data);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        byte[] round = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(data, round);
    }

    @Test
    public void testCompressBytes_ByteArraySlice() {
        byte[] big = new byte[30];
        Arrays.fill(big, (byte) 'x');
        System.arraycopy("slice".getBytes(StandardCharsets.UTF_8), 0, big, 10, 5);
        byte[] compressed = IOUtilities.compressBytes(big, 10, 5);
        byte[] round = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("slice".getBytes(StandardCharsets.UTF_8), round);
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytes_NullArray() {
        IOUtilities.compressBytes((byte[]) null);
    }

    // -----------------------------------------------------------------
    // 20‑21. uncompressBytes
    // -----------------------------------------------------------------
    @Test
    public void testUncompressBytes_Gzipped() {
        byte[] original = "compressed".getBytes(StandardCharsets.UTF_8);
        byte[] gz = IOUtilities.compressBytes(original);
        byte[] un = IOUtilities.uncompressBytes(gz);
        assertArrayEquals(original, un);
    }

    @Test
    public void testUncompressBytes_NotGzipped() {
        byte[] plain = "plain".getBytes(StandardCharsets.UTF_8);
        assertSame(plain, IOUtilities.uncompressBytes(plain));
    }

    @Test(expected = RuntimeException.class)
    public void testUncompressBytes_CorruptData() {
        byte[] corrupt = new byte[]{0,1,2,3,4};
        IOUtilities.uncompressBytes(corrupt);
    }

    @Test
    public void testUncompressBytes_WithOffsetLen() {
        byte[] original = "offset data".getBytes(StandardCharsets.UTF_8);
        byte[] gz = IOUtilities.compressBytes(original);
        // embed gz in a larger array
        byte[] wrapper = new byte[gz.length + 10];
        System.arraycopy(gz, 0, wrapper, 5, gz.length);
        byte[] result = IOUtilities.uncompressBytes(wrapper, 5, gz.length);
        assertArrayEquals(original, result);
    }

    // -----------------------------------------------------------------
    // Utility for creating temporary files with content
    // -----------------------------------------------------------------
    private File createTempFileWithBytes(byte[] data) throws IOException {
        File f = Files.createTempFile("ioUtilTest", ".tmp").toFile();
        f.deleteOnExit();
        Files.write(f.toPath(), data);
        return f;
    }
}
