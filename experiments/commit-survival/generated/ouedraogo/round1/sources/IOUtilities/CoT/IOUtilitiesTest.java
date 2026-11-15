package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

public class IOUtilitiesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /* ---------------------------------------------------------------------- */
    /* Helper classes                                                         */
    /* ---------------------------------------------------------------------- */

    /** Simple TransferCallback that records bytes transferred and can be cancelled */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        private boolean cancel = false;

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            recorded.write(bytes, 0, count);
        }

        @Override
        public boolean isCancelled() {
            return cancel;
        }

        public void setCancelled(boolean c) { this.cancel = c; }

        public byte[] getRecorded() { return recorded.toByteArray(); }
    }

    /** Closeable that throws on close – used to verify swallowing of exceptions */
    private static class BadCloseable implements Closeable {
        @Override public void close() throws IOException { throw new IOException("bad close"); }
    }

    /** Flushable that throws on flush – used to verify swallowing of exceptions */
    private static class BadFlushable implements Flushable {
        @Override public void flush() throws IOException { throw new IOException("bad flush"); }
    }

    /** URLConnection that writes to an in‑memory ByteArrayOutputStream */
    private static class MemoryURLConnection extends URLConnection {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        protected MemoryURLConnection() { super(null); }

        @Override public void connect() { }

        @Override public OutputStream getOutputStream() { return out; }

        public byte[] getBytes() { return out.toByteArray(); }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. getInputStream                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetInputStream_NoEncoding() throws Exception {
        URLConnection conn = new URL("data:text/plain,hello").openConnection();
        InputStream is = IOUtilities.getInputStream(conn);
        assertTrue(is instanceof BufferedInputStream);
        assertEquals('h', is.read());
        is.close();
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws Exception {
        // Prepare gzipped data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream gzip = new AdjustableGZIPOutputStream(baos, Deflater.BEST_SPEED)) {
            gzip.write("gzipped".getBytes());
        }
        byte[] gzipped = baos.toByteArray();

        // Mock a URLConnection that returns gzipped bytes and reports gzip encoding
        URLConnection conn = new URL("http://example.com").openConnection();
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Encoding", "gzip");
        // Use a custom URLConnection subclass to supply the stream
        URLConnection mock = new URLConnection(conn.getURL()) {
            @Override public void connect() {}
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(gzipped); }
            @Override public String getContentEncoding() { return "gzip"; }
        };
        InputStream is = IOUtilities.getInputStream(mock);
        assertTrue(is instanceof BufferedInputStream);
        // The returned stream should be a GZIPInputStream wrapped inside BufferedInputStream
        GZIPInputStream gis = new GZIPInputStream(is);
        byte[] out = new byte[10];
        int read = gis.read(out);
        assertEquals("gzipped", new String(out, 0, read));
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws Exception {
        // Create a deflated stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Inflater inflater = new Inflater();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater())) {
            dos.write("deflated".getBytes());
        }
        byte[] deflated = baos.toByteArray();

        URLConnection mock = new URLConnection(new URL("http://example.com")) {
            @Override public void connect() {}
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(deflated); }
            @Override public String getContentEncoding() { return "deflate"; }
        };
        InputStream is = IOUtilities.getInputStream(mock);
        assertTrue(is instanceof BufferedInputStream);
        // read through InflaterInputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertEquals("deflated", out.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* 2. transfer(File, URLConnection, TransferCallback)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        File src = folder.newFile("src.txt");
        byte[] data = "file transfer".getBytes();
        try (FileOutputStream fos = new FileOutputStream(src)) { fos.write(data); }

        MemoryURLConnection conn = new MemoryURLConnection();
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(src, conn, cb);
        assertArrayEquals(data, conn.getBytes());
        assertArrayEquals(data, cb.getRecorded());
    }

    @Test(expected = FileNotFoundException.class)
    public void testTransferFileToURLConnection_FileNotFound() throws Exception {
        File missing = new File(folder.getRoot(), "doesNotExist.txt");
        MemoryURLConnection conn = new MemoryURLConnection();
        IOUtilities.transfer(missing, conn, null);
    }

    @Test
    public void testTransferFileToURLConnection_CancelledCallback() throws Exception {
        File src = folder.newFile("big.txt");
        byte[] data = new byte[100_000];
        Arrays.fill(data, (byte) 'x');
        try (FileOutputStream fos = new FileOutputStream(src)) { fos.write(data); }

        MemoryURLConnection conn = new MemoryURLConnection();
        RecordingCallback cb = new RecordingCallback();
        cb.setCancelled(true); // cancel immediately

        IOUtilities.transfer(src, conn, cb);
        // Because cancellation happens after first chunk, output should be smaller than full size
        assertTrue(conn.getBytes().length < data.length);
        assertTrue(cb.getRecorded().length < data.length);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. transfer(URLConnection, File, TransferCallback)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile_Gzip() throws Exception {
        // Prepare gzipped payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream gzip = new AdjustableGZIPOutputStream(baos, Deflater.BEST_SPEED)) {
            gzip.write("payload".getBytes());
        }
        byte[] gzipped = baos.toByteArray();

        URLConnection mock = new URLConnection(new URL("http://example.com")) {
            @Override public void connect() {}
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(gzipped); }
            @Override public String getContentEncoding() { return "gzip"; }
        };

        File dest = folder.newFile("dest.txt");
        IOUtilities.transfer(mock, dest, null);
        byte[] result = readAllBytes(dest);
        assertEquals("payload", new String(result));
    }

    @Test(expected = IOException.class)
    public void testTransferURLConnectionToFile_GzipInvalidData() throws Exception {
        // Not actually gzipped but header says gzip
        URLConnection mock = new URLConnection(new URL("http://example.com")) {
            @Override public void connect() {}
            @Override public InputStream getInputStream() { return new ByteArrayInputStream("notgz".getBytes()); }
            @Override public String getContentEncoding() { return "gzip"; }
        };
        File dest = folder.newFile("dest.txt");
        IOUtilities.transfer(mock, dest, null); // should throw when GZIPInputStream reads
    }

    /* ---------------------------------------------------------------------- */
    /* 4. transfer(InputStream, File, TransferCallback)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        byte[] data = "stream to file".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        File outFile = folder.newFile("out.txt");
        IOUtilities.transfer(in, outFile, null);
        assertArrayEquals(data, readAllBytes(outFile));
    }

    @Test
    public void testTransferInputStreamToFile_EmptyStream() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        File outFile = folder.newFile("empty.txt");
        IOUtilities.transfer(in, outFile, null);
        assertEquals(0, outFile.length());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. transfer(InputStream, OutputStream, TransferCallback)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferStreamWithCallback() throws Exception {
        byte[] data = "callback test".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertArrayEquals(data, cb.getRecorded());
    }

    @Test
    public void testTransferStream_NoCallback() throws Exception {
        byte[] data = "no callback".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out, null);
        assertArrayEquals(data, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. transfer(InputStream, byte[])                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToByteArrayExact() throws Exception {
        byte[] src = "exact".getBytes();
        InputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[src.length];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(src, dest);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArrayTooShort() throws Exception {
        byte[] src = "longer".getBytes();
        InputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[3]; // too short
        IOUtilities.transfer(in, dest); // should throw
    }

    /* ---------------------------------------------------------------------- */
    /* 7. transfer(InputStream, OutputStream)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferStreamToStream() throws Exception {
        byte[] data = "stream2stream".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. transfer(File, OutputStream)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream() throws Exception {
        File src = folder.newFile("src2.txt");
        byte[] data = "file to out".getBytes();
        try (FileOutputStream fos = new FileOutputStream(src)) { fos.write(data); }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(src, out);
        assertArrayEquals(data, out.toByteArray());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFileToNullOutputStream() throws Exception {
        File src = folder.newFile("src3.txt");
        IOUtilities.transfer(src, null);
    }

    /* ---------------------------------------------------------------------- */
    /* 9‑13. close / flush helpers                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloseAndFlushSwallowExceptions() {
        // close() should swallow IOException
        IOUtilities.close(new BadCloseable());

        // flush() should swallow IOException
        IOUtilities.flush(new BadFlushable());

        // Null arguments must be safely ignored
        IOUtilities.close((Closeable) null);
        IOUtilities.flush((Flushable) null);
        IOUtilities.close((XMLStreamReader) null);
        IOUtilities.close((XMLStreamWriter) null);
        IOUtilities.flush((XMLStreamWriter) null);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. inputStreamToBytes                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytes() {
        byte[] data = "to bytes".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, result);
    }

    @Test
    public void testInputStreamToBytes_OnErrorReturnsNull() {
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("boom"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* ---------------------------------------------------------------------- */
    /* 15. transfer(URLConnection, byte[])                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionBytes() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection();
        byte[] payload = "bytes payload".getBytes();
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* 16‑17. compressBytes (stream variants)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_StreamVariant() throws Exception {
        ByteArrayOutputStream src = new ByteArrayOutputStream();
        src.write("stream data".getBytes());

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(src, compressed);
        assertTrue(ByteUtilities.isGzipped(compressed.toByteArray()));
    }

    @Test
    public void testCompressBytes_FastByteArrayVariant() throws Exception {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.write("fast byte data".getBytes());

        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(src, compressed);
        assertTrue(ByteUtilities.isGzipped(compressed.toByteArray()));
    }

    /* ---------------------------------------------------------------------- */
    /* 18‑19. compressBytes(byte[]) / compressBytes(byte[],int,int)             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_ArrayFull() {
        byte[] data = "array data".getBytes();
        byte[] compressed = IOUtilities.compressBytes(data);
        assertTrue(ByteUtilities.isGzipped(compressed));
        // round‑trip
        assertArrayEquals(data, IOUtilities.uncompressBytes(compressed));
    }

    @Test
    public void testCompressBytes_ArrayPartial() {
        byte[] data = "partial array data".getBytes();
        int offset = 8; // skip "partial "
        int len = data.length - offset;
        byte[] compressed = IOUtilities.compressBytes(data, offset, len);
        assertTrue(ByteUtilities.isGzipped(compressed));
        byte[] expected = Arrays.copyOfRange(data, offset, data.length);
        assertArrayEquals(expected, IOUtilities.uncompressBytes(compressed));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testCompressBytes_ArrayInvalidRange() {
        byte[] data = "short".getBytes();
        IOUtilities.compressBytes(data, 2, 10); // exceeds array length
    }

    /* ---------------------------------------------------------------------- */
    /* 20‑21. uncompressBytes                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUncompressBytes_Gzipped() {
        byte[] original = "to uncompress".getBytes();
        byte[] gzipped = IOUtilities.compressBytes(original);
        byte[] result = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original, result);
    }

    @Test
    public void testUncompressBytes_NotGzipped() {
        byte[] plain = "plain data".getBytes();
        assertSame(plain, IOUtilities.uncompressBytes(plain));
    }

    @Test(expected = NullPointerException.class)
    public void testUncompressBytes_Null() {
        IOUtilities.uncompressBytes(null);
    }

    /* ---------------------------------------------------------------------- */
    /* Utility method for reading whole file                                    */
    /* ---------------------------------------------------------------------- */
    private static byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtilities.transfer(fis, baos);
            return baos.toByteArray();
        }
    }
}
