package com.cedarsoftware.util;
import org.mockito.invocation.Location;


import static org.junit.Assert.*;

import java.io.*;
import java.net.URLConnection;
import java.util.zip.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link IOUtilities}.
 */
public class IOUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* ---------------------------------------------------------------------- */

    /** Simple {@link TransferCallback} that records transferred bytes and can cancel. */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        private final int cancelAfterBytes;
        private int total = 0;

        RecordingCallback() {
            this(-1);
        }

        RecordingCallback(int cancelAfterBytes) {
            this.cancelAfterBytes = cancelAfterBytes;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            recorded.write(bytes, 0, count);
            total += count;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfterBytes >= 0 && total >= cancelAfterBytes;
        }

        byte[] getRecordedBytes() {
            return recorded.toByteArray();
        }
    }

    /** Minimal {@link URLConnection} that works with in‑memory streams. */
    private static class InMemoryURLConnection extends URLConnection {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private ByteArrayInputStream in;
        private String contentEncoding = null;

        InMemoryURLConnection() {
            super(null);
        }

        @Override
        public void connect() {
            // no real connection needed
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public InputStream getInputStream() {
            return in != null ? in : new ByteArrayInputStream(new byte[0]);
        }

        void setInputBytes(byte[] bytes) {
            this.in = new ByteArrayInputStream(bytes);
        }

        void setContentEncoding(String enc) {
            this.contentEncoding = enc;
        }

        @Override
        public String getContentEncoding() {
            return this.contentEncoding;
        }

        byte[] getWrittenBytes() {
            return out.toByteArray();
        }
    }

    /** Closeable that throws IOException on close – used to verify swallow. */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("forced");
        }
    }

    /** Flushable that throws IOException on flush – used to verify swallow. */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("forced");
        }
    }

    /** InputStream that throws IOException on read – used to test inputStreamToBytes error handling. */
    private static class BadInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("forced");
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for getInputStream()                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetInputStreamPlain() throws Exception {
        String payload = "plain text";
        InMemoryURLConnection conn = new InMemoryURLConnection();
        conn.setInputBytes(payload.getBytes("UTF-8"));
        conn.setContentEncoding(null);

        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtilities.transfer(is, baos);
        assertArrayEquals(payload.getBytes("UTF-8"), baos.toByteArray());
    }

    @Test
    public void testGetInputStreamGzip() throws Exception {
        byte[] original = "gzip content".getBytes("UTF-8");
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(original);
        }
        InMemoryURLConnection conn = new InMemoryURLConnection();
        conn.setInputBytes(gz.toByteArray());
        conn.setContentEncoding("gzip");

        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertArrayEquals(original, out.toByteArray());
    }

    @Test
    public void testGetInputStreamDeflate() throws Exception {
        byte[] original = "deflate content".getBytes("UTF-8");
        ByteArrayOutputStream def = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        deflater.setInput(original);
        deflater.finish();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int cnt = deflater.deflate(buf);
            def.write(buf, 0, cnt);
        }
        deflater.end();

        InMemoryURLConnection conn = new InMemoryURLConnection();
        conn.setInputBytes(def.toByteArray());
        conn.setContentEncoding("deflate");

        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertArrayEquals(original, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream, TransferCallback)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferWithCallbackAndNoCancel() throws Exception {
        byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertArrayEquals(data, cb.getRecordedBytes());
    }

    @Test
    public void testTransferWithCallbackAndCancel() throws Exception {
        byte[] data = "0123456789".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Cancel after 5 bytes
        RecordingCallback cb = new RecordingCallback(5);

        IOUtilities.transfer(in, out, cb);
        assertEquals(5, out.toByteArray().length);
        assertArrayEquals(Arrays.copyOf(data, 5), out.toByteArray());
        assertArrayEquals(Arrays.copyOf(data, 5), cb.getRecordedBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, byte[])                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferToByteArrayExactLength() throws Exception {
        byte[] source = "exact length".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        byte[] target = new byte[source.length];
        IOUtilities.transfer(in, target);
        assertArrayEquals(source, target);
    }

    @Test(expected = IOException.class)
    public void testTransferToByteArrayInsufficientData() throws Exception {
        byte[] source = "short".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        byte[] target = new byte[source.length + 10];
        IOUtilities.transfer(in, target); // should throw
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(File, OutputStream)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream() throws Exception {
        File temp = File.createTempFile("ioUtilTest", ".txt");
        temp.deleteOnExit();
        byte[] content = "file content".getBytes("UTF-8");
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(content);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(temp, out);
        assertArrayEquals(content, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(File, URLConnection, TransferCallback)              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        File src = File.createTempFile("src", ".bin");
        src.deleteOnExit();
        byte[] data = "data for conn".getBytes("UTF-8");
        try (FileOutputStream fos = new FileOutputStream(src)) {
            fos.write(data);
        }

        InMemoryURLConnection conn = new InMemoryURLConnection();
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(src, conn, cb);
        assertArrayEquals(data, conn.getWrittenBytes());
        assertArrayEquals(data, cb.getRecordedBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, File, TransferCallback)             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile() throws Exception {
        byte[] payload = "payload".getBytes("UTF-8");
        InMemoryURLConnection conn = new InMemoryURLConnection();
        conn.setInputBytes(payload);
        conn.setContentEncoding(null);

        File dest = File.createTempFile("dest", ".bin");
        dest.deleteOnExit();

        RecordingCallback cb = new RecordingCallback();
        IOUtilities.transfer(conn, dest, cb);
        byte[] fileBytes = java.nio.file.Files.readAllBytes(dest.toPath());
        assertArrayEquals(payload, fileBytes);
        assertArrayEquals(payload, cb.getRecordedBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for close(Closeable) and flush(Flushable)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloseSwallowsIOException() {
        // Should not throw
        IOUtilities.close(new BadCloseable());
    }

    @Test
    public void testFlushSwallowsIOException() {
        // Should not throw
        IOUtilities.flush(new BadFlushable());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for inputStreamToBytes                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytesSuccess() {
        byte[] data = "stream bytes".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, result);
    }

    @Test
    public void testInputStreamToBytesFailureReturnsNull() {
        InputStream bad = new BadInputStream();
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, byte[])                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionByteArray() throws Exception {
        InMemoryURLConnection conn = new InMemoryURLConnection();
        byte[] payload = "byte array payload".getBytes("UTF-8");
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for compressBytes / uncompressBytes                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompressAndUncompressByteArray() {
        byte[] original = "some data that will be compressed".getBytes();
        byte[] compressed = IOUtilities.compressBytes(original);
        assertTrue(ByteUtilities.isGzipped(compressed));
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testCompressByteArrayWithOffsetAndLength() {
        byte[] source = "ABCDEFGHIJ".getBytes(); // 10 bytes
        // compress only "CDE"
        byte[] compressed = IOUtilities.compressBytes(source, 2, 3);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("CDE".getBytes(), uncompressed);
    }

    @Test
    public void testUncompressNonGzippedReturnsSameArray() {
        byte[] data = "plain".getBytes();
        assertSame(data, IOUtilities.uncompressBytes(data));
    }

    @Test
    public void testCompressBytesFastByteArrayOutputStream() throws Exception {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("fast stream data".getBytes());

        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);

        // Decompress using the public API and verify
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed);
    }

    @Test
    public void testCompressBytesByteArrayOutputStream() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("byte array output".getBytes());

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);

        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for close(XMLStreamReader) and close(XMLStreamWriter)            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloseXmlStreamReaderSwallowsException() {
        XMLStreamReader reader = new XMLStreamReader() {
            @Override public void close() throws XMLStreamException { throw new XMLStreamException("forced"); }
            // All other methods can throw UnsupportedOperationException – they are never called.
            @Override public Object getProperty(String name) { throw new UnsupportedOperationException(); }
            @Override public int next() { throw new UnsupportedOperationException(); }
            @Override public void require(int type, String namespace, String name) { throw new UnsupportedOperationException(); }
            @Override public String getElementText() { throw new UnsupportedOperationException(); }
            @Override public int nextTag() { throw new UnsupportedOperationException(); }
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }
            @Override public void close() throws XMLStreamException { }
            @Override public String getNamespaceURI(String prefix) { throw new UnsupportedOperationException(); }
            @Override public boolean isStartElement() { throw new UnsupportedOperationException(); }
            @Override public boolean isEndElement() { throw new UnsupportedOperationException(); }
            @Override public boolean isCharacters() { throw new UnsupportedOperationException(); }
            @Override public boolean isWhiteSpace() { throw new UnsupportedOperationException(); }
            @Override public String getLocalName() { throw new UnsupportedOperationException(); }
            @Override public boolean hasName() { throw new UnsupportedOperationException(); }
            @Override public String getPrefix() { throw new UnsupportedOperationException(); }
            @Override public String getNamespaceURI() { throw new UnsupportedOperationException(); }
            @Override public int getNamespaceCount() { throw new UnsupportedOperationException(); }
            @Override public String getNamespacePrefix(int index) { throw new UnsupportedOperationException(); }
            @Override public String getNamespaceURI(int index) { throw new UnsupportedOperationException(); }
            @Override public int getEventType() { throw new UnsupportedOperationException(); }
            @Override public String getText() { throw new UnsupportedOperationException(); }
            @Override public char[] getTextCharacters() { throw new UnsupportedOperationException(); }
            @Override public int getTextStart() { throw new UnsupportedOperationException(); }
            @Override public int getTextLength() { throw new UnsupportedOperationException(); }
            @Override public String getEncoding() { throw new UnsupportedOperationException(); }
            @Override public boolean hasText() { throw new UnsupportedOperationException(); }
            @Override public Location getLocation() { throw new UnsupportedOperationException(); }
            @Override public QName getName() { throw new UnsupportedOperationException(); }
            @Override public String getNamespaceURI(String prefix) { throw new UnsupportedOperationException(); }
        };
        // Should not propagate exception
        IOUtilities.close(reader);
    }

    @Test
    public void testCloseXmlStreamWriterSwallowsException() {
        XMLStreamWriter writer = new XMLStreamWriter() {
            @Override public void close() throws XMLStreamException { throw new XMLStreamException("forced"); }
            // Stub other methods
            @Override public void writeStartElement(String localName) { }
            @Override public void writeStartElement(String namespaceURI, String localName) { }
            @Override public void writeStartElement(String prefix, String localName, String namespaceURI) { }
            @Override public void writeEndElement() { }
            @Override public void writeEndDocument() { }
            @Override public void close() throws XMLStreamException { }
            @Override public void flush() throws XMLStreamException { }
            @Override public void writeAttribute(String localName, String value) { }
            @Override public void writeAttribute(String prefix, String namespaceURI, String localName, String value) { }
            @Override public void writeAttribute(String namespaceURI, String localName, String value) { }
            @Override public void writeNamespace(String prefix, String namespaceURI) { }
            @Override public void writeDefaultNamespace(String namespaceURI) { }
            @Override public void writeComment(String data) { }
            @Override public void writeProcessingInstruction(String target) { }
            @Override public void writeProcessingInstruction(String target, String data) { }
            @Override public void writeCData(String data) { }
            @Override public void writeDTD(String dtd) { }
            @Override public void writeEntityRef(String name) { }
            @Override public void writeStartDocument() { }
            @Override public void writeStartDocument(String version) { }
            @Override public void writeStartDocument(String encoding, String version) { }
            @Override public void writeCharacters(String text) { }
            @Override public void writeCharacters(char[] text, int start, int len) { }
            @Override public String getPrefix(String uri) { return null; }
            @Override public void setPrefix(String prefix, String uri) { }
            @Override public void setDefaultNamespace(String uri) { }
            @Override public void setNamespaceContext(NamespaceContext context) { }
            @Override public NamespaceContext getNamespaceContext() { return null; }
            @Override public Object getProperty(String name) { return null; }
        };
        IOUtilities.close(writer);
    }
}
