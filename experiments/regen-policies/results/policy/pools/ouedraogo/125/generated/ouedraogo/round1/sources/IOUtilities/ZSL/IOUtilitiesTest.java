package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.*;

public class IOUtilitiesTest {

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("ioutil-test", ".bin");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* -------------------------------------------------------------
     * Helper classes
     * ------------------------------------------------------------- */

    /** Simple URLConnection that returns a preset InputStream and
     *  records what is written to its OutputStream. */
    private static class SimpleURLConnection extends URLConnection {
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private String contentEncoding;

        protected SimpleURLConnection(URL url, byte[] data, String encoding) {
            super(url);
            this.input = new ByteArrayInputStream(data);
            this.contentEncoding = encoding;
        }

        @Override
        public void connect() {
            // No real connection needed.
        }

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

    /** Closeable that throws IOException on close – used to test
     *  that IOUtilities.close suppresses the exception. */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("Bad close");
        }
    }

    /** Flushable that throws IOException on flush – used to test
     *  that IOUtilities.flush suppresses the exception. */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("Bad flush");
        }
    }

    /** TransferCallback that records transferred bytes and can be
     *  cancelled after a given amount. */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final ByteArrayOutputStream record = new ByteArrayOutputStream();
        private final int cancelAfter;
        private int total = 0;

        RecordingCallback() {
            this(-1);
        }

        RecordingCallback(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            record.write(bytes, 0, count);
            total += count;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter > 0 && total >= cancelAfter;
        }

        public byte[] getRecorded() {
            return record.toByteArray();
        }
    }

    /* -------------------------------------------------------------
     * Tests for getInputStream (gzip, deflate, none)
     * ------------------------------------------------------------- */

    @Test
    public void testGetInputStream_NoEncoding() throws Exception {
        byte[] data = "plain data".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), data, null);
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(data, read);
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws Exception {
        byte[] original = "gzip compressed".getBytes("UTF-8");
        byte[] gzipped = IOUtilities.compressBytes(original);
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), gzipped, "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        // should be a GZIPInputStream wrapped inside BufferedInputStream
        assertTrue(is instanceof BufferedInputStream);
        byte[] read = readAll(is);
        assertArrayEquals(original, read);
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws Exception {
        // Build a deflate-compressed byte array manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new AdjustableGZIPOutputStream(baos, Deflater.DEFAULT_COMPRESSION);
        deflater.write("deflate data".getBytes("UTF-8"));
        deflater.close();
        byte[] deflated = baos.toByteArray();

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), deflated, "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        assertTrue(is instanceof BufferedInputStream);
        byte[] read = readAll(is);
        assertArrayEquals("deflate data".getBytes("UTF-8"), read);
    }

    /* -------------------------------------------------------------
     * Tests for transfer methods (File <-> URLConnection, streams)
     * ------------------------------------------------------------- */

    @Test
    public void testTransfer_FileToURLConnection() throws Exception {
        byte[] payload = "file to connection".getBytes("UTF-8");
        Files.write(tempFile.toPath(), payload);
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new byte[0], null);
        IOUtilities.transfer(tempFile, conn, null);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    @Test
    public void testTransfer_URLConnectionToFile() throws Exception {
        byte[] payload = "connection to file".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), payload, null);
        IOUtilities.transfer(conn, tempFile, null);
        byte[] fromFile = Files.readAllBytes(tempFile.toPath());
        assertArrayEquals(payload, fromFile);
    }

    @Test
    public void testTransfer_InputStreamToFile_WithCallback() throws Exception {
        byte[] source = new byte[100_000];
        for (int i = 0; i < source.length; i++) source[i] = (byte) (i % 256);
        InputStream in = new ByteArrayInputStream(source);
        RecordingCallback cb = new RecordingCallback(); // no cancellation
        IOUtilities.transfer(in, tempFile, cb);
        byte[] fromFile = Files.readAllBytes(tempFile.toPath());
        assertArrayEquals(source, fromFile);
        assertArrayEquals(source, cb.getRecorded());
    }

    @Test
    public void testTransfer_InputStreamToOutputStream_WithCancellation() throws Exception {
        byte[] data = new byte[5000];
        Arrays.fill(data, (byte) 1);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Cancel after 2000 bytes
        RecordingCallback cb = new RecordingCallback(2000);
        IOUtilities.transfer(in, out, cb);
        assertTrue(out.size() <= 2000 + IOUtilities.TRANSFER_BUFFER); // may write a buffer beyond cancel point
        assertTrue(cb.isCancelled());
    }

    @Test
    public void testTransfer_InputStreamToByteArrayExact() throws Exception {
        byte[] data = "exact read".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] target = new byte[data.length];
        IOUtilities.transfer(in, target);
        assertArrayEquals(data, target);
    }

    @Test(expected = IOException.class)
    public void testTransfer_InputStreamToByteArrayInsufficient() throws Exception {
        byte[] data = "short".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] target = new byte[data.length + 10];
        IOUtilities.transfer(in, target); // should throw IOException
    }

    @Test
    public void testTransfer_InputStreamToOutputStream_NoCallback() throws Exception {
        byte[] data = "no callback".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testTransfer_FileToOutputStream_Flushes() throws Exception {
        byte[] data = "flush test".getBytes("UTF-8");
        Files.write(tempFile.toPath(), data);
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            private boolean flushed = false;
            @Override
            public void flush() throws IOException {
                super.flush();
                flushed = true;
            }
            boolean isFlushed() {
                return flushed;
            }
        };
        IOUtilities.transfer(tempFile, out);
        assertTrue(((ByteArrayOutputStream) out).size() > 0);
        assertTrue(((ByteArrayOutputStream) out).toString().contains("flush test"));
        // verify that flush was called (our subclass records it)
        assertTrue(((ByteArrayOutputStream) out).isFlushed());
    }

    /* -------------------------------------------------------------
     * Tests for close and flush helpers
     * ------------------------------------------------------------- */

    @Test
    public void testClose_CloseableSuppressesException() {
        BadCloseable bad = new BadCloseable();
        // Should not propagate exception
        IOUtilities.close(bad);
    }

    @Test
    public void testClose_NullDoesNotThrow() {
        IOUtilities.close((Closeable) null);
        IOUtilities.close((XMLStreamReader) null);
        IOUtilities.close((XMLStreamWriter) null);
    }

    @Test
    public void testFlush_FlushableSuppressesException() {
        BadFlushable bad = new BadFlushable();
        IOUtilities.flush(bad);
    }

    @Test
    public void testFlush_NullDoesNotThrow() {
        IOUtilities.flush((Flushable) null);
        IOUtilities.flush((XMLStreamWriter) null);
    }

    /* -------------------------------------------------------------
     * Tests for inputStreamToBytes (normal & error case)
     * ------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytes_Normal() throws Exception {
        byte[] data = "stream to bytes".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, result);
    }

    @Test
    public void testInputStreamToBytes_WithExceptionReturnsNull() throws Exception {
        InputStream bad = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* -------------------------------------------------------------
     * Tests for transfer(URLConnection, byte[])
     * ------------------------------------------------------------- */

    @Test
    public void testTransfer_URLConnectionByteArray() throws Exception {
        byte[] payload = "byte array write".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new byte[0], null);
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    /* -------------------------------------------------------------
     * Tests for compressBytes / uncompressBytes (byte[] overloads)
     * ------------------------------------------------------------- */

    @Test
    public void testCompressAndUncompressRoundTrip() {
        byte[] original = new byte[1024];
        for (int i = 0; i < original.length; i++) original[i] = (byte) (i % 128);
        byte[] compressed = IOUtilities.compressBytes(original);
        assertNotNull(compressed);
        assertTrue(compressed.length < original.length); // should be smaller
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testUncompressBytes_NotGzippedReturnsOriginal() {
        byte[] data = "not gzipped".getBytes();
        byte[] result = IOUtilities.uncompressBytes(data);
        assertSame(data, result);
    }

    @Test
    public void testCompressBytes_ByteArrayOutputStream() throws Exception {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("fast stream".getBytes("UTF-8"));
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed);
    }

    @Test
    public void testCompressBytes_ByteArrayOutputStream_ByteArray() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("regular baos".getBytes("UTF-8"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed);
    }

    /* -------------------------------------------------------------
     * Utility method to read all bytes from an InputStream
     * ------------------------------------------------------------- */
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtilities.transfer(in, baos);
        return baos.toByteArray();
    }
}
