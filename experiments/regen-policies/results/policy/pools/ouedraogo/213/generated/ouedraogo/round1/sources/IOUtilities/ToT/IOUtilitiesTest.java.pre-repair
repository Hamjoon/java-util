package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.zip.*;

import static org.junit.Assert.*;

public class IOUtilitiesTest {

    /* --------------------------------------------------------------
     * Helper classes used throughout the test suite
     * -------------------------------------------------------------- */

    /**
     * Minimal URLConnection implementation that works with in‑memory byte arrays.
     */
    private static class TestURLConnection extends URLConnection {
        private final byte[] inputData;
        private final String contentEncoding;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final Map<String, List<String>> requestProperties = new HashMap<>();

        protected TestURLConnection(byte[] inputData, String contentEncoding) {
            super(null);
            this.inputData = inputData;
            this.contentEncoding = contentEncoding;
        }

        @Override
        public void connect() {
            // no real connection needed
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(inputData);
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public String getContentEncoding() {
            return contentEncoding;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        public byte[] getWrittenBytes() {
            return output.toByteArray();
        }

        public List<String> getRequestPropertyValues(String key) {
            return requestProperties.get(key);
        }
    }

    /**
     * Simple TransferCallback that records transferred bytes and can be forced
     * to cancel after a given number of bytes.
     */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        private final int cancelAfter; // -1 means never cancel

        RecordingCallback() {
            this(-1);
        }

        RecordingCallback(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            recorded.write(bytes, 0, count);
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter >= 0 && recorded.size() >= cancelAfter;
        }

        public byte[] getRecordedBytes() {
            return recorded.toByteArray();
        }
    }

    /**
     * XMLStreamReader that throws an exception on close().
     */
    private static class ThrowingXMLStreamReader implements XMLStreamReader {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("close failure"); }
        // all other methods throw UnsupportedOperationException – they are never called
        @Override public Object getProperty(String name) { throw new UnsupportedOperationException(); }
        @Override public int next() { throw new UnsupportedOperationException(); }
        @Override public void require(int type, String namespaceURI, String localName) {}
        @Override public String getElementText() { return null; }
        @Override public int nextTag() { return 0; }
        @Override public boolean hasNext() { return false; }
        @Override public void closeQuietly() {}
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public boolean isStartElement() { return false; }
        @Override public boolean isEndElement() { return false; }
        @Override public boolean isCharacters() { return false; }
        @Override public boolean isWhiteSpace() { return false; }
        @Override public String getLocalName() { return null; }
        @Override public boolean hasName() { return false; }
        @Override public String getPrefix() { return null; }
        @Override public String getNamespaceURI() { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public javax.xml.namespace.QName getName() { return null; }
        @Override public String getNamespaceContext() { return null; }
        @Override public String getText() { return null; }
        @Override public char[] getTextCharacters() { return new char[0]; }
        @Override public int getTextStart() { return 0; }
        @Override public int getTextLength() { return 0; }
        @Override public String getEncoding() { return null; }
        @Override public int getEventType() { return 0; }
        @Override public String getLocation() { return null; }
        @Override public boolean isWhiteSpace() { return false; }
        @Override public int getAttributeName(int i) { return 0; }
        @Override public String getAttributeNamespace(int i) { return null; }
        @Override public String getAttributeLocalName(int i) { return null; }
        @Override public String getAttributePrefix(int i) { return null; }
        @Override public String getAttributeType(int i) { return null; }
        @Override public String getAttributeValue(int i) { return null; }
        @Override public boolean isAttributeSpecified(int i) { return false; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespacePrefix(int i) { return null; }
        @Override public String getNamespaceURI(int i) { return null; }
        @Override public javax.xml.namespace.NamespaceContext getNamespaceContext() { return null; }
        @Override public int getEventType() { return 0; }
    }

    /**
     * XMLStreamWriter that throws an exception on close().
     */
    private static class ThrowingXMLStreamWriter implements XMLStreamWriter {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("close failure"); }
        // all other methods throw UnsupportedOperationException – they are never called
        @Override public void writeStartElement(String localName) {}
        @Override public void writeStartElement(String namespaceURI, String localName) {}
        @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
        @Override public void writeEmptyElement(String namespaceURI, String localName) {}
        @Override public void writeEmptyElement(String prefix, String localName, String namespaceURI) {}
        @Override public void writeEmptyElement(String localName) {}
        @Override public void writeEndElement() {}
        @Override public void writeEndDocument() {}
        @Override public void close() {}
        @Override public void flush() {}
        @Override public void writeAttribute(String localName, String value) {}
        @Override public void writeAttribute(String prefix, String namespaceURI, String localName, String value) {}
        @Override public void writeAttribute(String namespaceURI, String localName, String value) {}
        @Override public void writeNamespace(String prefix, String namespaceURI) {}
        @Override public void writeDefaultNamespace(String namespaceURI) {}
        @Override public void writeComment(String data) {}
        @Override public void writeProcessingInstruction(String target) {}
        @Override public void writeProcessingInstruction(String target, String data) {}
        @Override public void writeCData(String data) {}
        @Override public void writeDTD(String dtd) {}
        @Override public void writeEntityRef(String name) {}
        @Override public void writeStartDocument() {}
        @Override public void writeStartDocument(String version) {}
        @Override public void writeStartDocument(String encoding, String version) {}
        @Override public void writeCharacters(String text) {}
        @Override public void writeCharacters(char[] text, int start, int len) {}
        @Override public String getPrefix(String uri) { return null; }
        @Override public void setPrefix(String prefix, String uri) {}
        @Override public void setDefaultNamespace(String uri) {}
        @Override public void setNamespaceContext(javax.xml.namespace.NamespaceContext context) {}
        @Override public javax.xml.namespace.NamespaceContext getNamespaceContext() { return null; }
        @Override public Object getProperty(String name) { return null; }
    }

    /**
     * Closeable that throws IOException on close().
     */
    private static class ThrowingCloseable implements Closeable {
        @Override public void close() throws IOException { throw new IOException("close failure"); }
    }

    /**
     * Flushable that throws IOException on flush().
     */
    private static class ThrowingFlushable implements Flushable {
        @Override public void flush() throws IOException { throw new IOException("flush failure"); }
    }

    /**
     * OutputStream that remembers whether flush() was called.
     */
    private static class FlushDetectingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private boolean flushed = false;

        @Override public void write(int b) { delegate.write(b); }
        @Override public void write(byte[] b, int off, int len) { delegate.write(b, off, len); }
        @Override public void flush() { flushed = true; }
        public byte[] toByteArray() { return delegate.toByteArray(); }
        public boolean isFlushed() { return flushed; }
    }

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("ioutiltest", ".dat");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* --------------------------------------------------------------
     * Tests for getInputStream()
     * -------------------------------------------------------------- */

    @Test
    public void testGetInputStreamPlain() throws Exception {
        byte[] data = "plain text".getBytes("UTF-8");
        TestURLConnection conn = new TestURLConnection(data, null);
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(data, read);
    }

    @Test
    public void testGetInputStreamGzip() throws Exception {
        byte[] original = "gzip compressed".getBytes("UTF-8");
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(original);
        }
        TestURLConnection conn = new TestURLConnection(gz.toByteArray(), "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(original, read);
    }

    @Test
    public void testGetInputStreamDeflate() throws Exception {
        byte[] original = "deflate compressed".getBytes("UTF-8");
        ByteArrayOutputStream def = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(def)) {
            dos.write(original);
        }
        TestURLConnection conn = new TestURLConnection(def.toByteArray(), "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = readAll(is);
        assertArrayEquals(original, read);
    }

    @Test(expected = NullPointerException.class)
    public void testGetInputStreamNullConnection() throws Exception {
        IOUtilities.getInputStream(null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(File, URLConnection, TransferCallback)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferFileToConnection() throws Exception {
        byte[] payload = "file to connection".getBytes("UTF-8");
        writeBytesToFile(tempFile, payload);

        TestURLConnection conn = new TestURLConnection(new byte[0], null);
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(tempFile, conn, cb);

        assertArrayEquals(payload, conn.getWrittenBytes());
        assertArrayEquals(payload, cb.getRecordedBytes());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFileToConnectionNullFile() throws Exception {
        TestURLConnection conn = new TestURLConnection(new byte[0], null);
        IOUtilities.transfer(null, conn, null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFileToConnectionNullConn() throws Exception {
        IOUtilities.transfer(tempFile, null, null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(URLConnection, File, TransferCallback)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferConnectionToFileHandlesGzip() throws Exception {
        byte[] original = "some data for gzip".getBytes("UTF-8");
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(original);
        }
        TestURLConnection conn = new TestURLConnection(gz.toByteArray(), "gzip");
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(conn, tempFile, cb);
        byte[] fromFile = readAll(new FileInputStream(tempFile));

        assertArrayEquals(original, fromFile);
        assertArrayEquals(original, cb.getRecordedBytes());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferConnectionToFileNullConn() throws Exception {
        IOUtilities.transfer(null, tempFile, null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferConnectionToFileNullFile() throws Exception {
        TestURLConnection conn = new TestURLConnection(new byte[0], null);
        IOUtilities.transfer(conn, null, null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(InputStream, File, TransferCallback)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        byte[] data = "stream to file".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(in, tempFile, cb);

        byte[] fromFile = readAll(new FileInputStream(tempFile));
        assertArrayEquals(data, fromFile);
        assertArrayEquals(data, cb.getRecordedBytes());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferInputStreamToFileNullStream() throws Exception {
        IOUtilities.transfer(null, tempFile, null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferInputStreamToFileNullFile() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        IOUtilities.transfer(in, null, null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(InputStream, OutputStream, TransferCallback)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferStreamToStreamWithCallback() throws Exception {
        byte[] data = "callback test".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback();

        IOUtilities.transfer(in, out, cb);

        assertArrayEquals(data, out.toByteArray());
        assertArrayEquals(data, cb.getRecordedBytes());
    }

    @Test
    public void testTransferStreamToStreamCancelMidway() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // cancel after 256 bytes
        RecordingCallback cb = new RecordingCallback(256);

        IOUtilities.transfer(in, out, cb);

        assertEquals(256, out.size());
        assertEquals(256, cb.getRecordedBytes().length);
        assertArrayEquals(Arrays.copyOf(data, 256), out.toByteArray());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferStreamToStreamNullIn() throws Exception {
        IOUtilities.transfer(null, new ByteArrayOutputStream(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferStreamToStreamNullOut() throws Exception {
        IOUtilities.transfer(new ByteArrayInputStream(new byte[0]), null, null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(InputStream, byte[])
     * -------------------------------------------------------------- */

    @Test
    public void testTransferReadFullyExactSize() throws Exception {
        byte[] expected = "exact size".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(expected);
        byte[] buffer = new byte[expected.length];
        IOUtilities.transfer(in, buffer);
        assertArrayEquals(expected, buffer);
    }

    @Test(expected = EOFException.class)
    public void testTransferReadFullyTooShort() throws Exception {
        byte[] data = "short".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] buffer = new byte[data.length + 5];
        IOUtilities.transfer(in, buffer); // should throw EOFException
    }

    @Test(expected = NullPointerException.class)
    public void testTransferReadFullyNullStream() throws Exception {
        IOUtilities.transfer(null, new byte[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferReadFullyNullArray() throws Exception {
        IOUtilities.transfer(new ByteArrayInputStream(new byte[0]), null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(InputStream, OutputStream)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferStreamToStreamNoCallback() throws Exception {
        byte[] payload = "simple copy".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(payload);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out);

        assertArrayEquals(payload, out.toByteArray());
    }

    /* --------------------------------------------------------------
     * Tests for transfer(File, OutputStream)
     * -------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStreamFlushes() throws Exception {
        byte[] data = "file to out".getBytes("UTF-8");
        writeBytesToFile(tempFile, data);
        FlushDetectingOutputStream out = new FlushDetectingOutputStream();

        IOUtilities.transfer(tempFile, out);

        assertArrayEquals(data, out.toByteArray());
        assertTrue("OutputStream.flush() must be called", out.isFlushed());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFileToOutputStreamNullFile() throws Exception {
        IOUtilities.transfer(null, new ByteArrayOutputStream());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferFileToOutputStreamNullOut() throws Exception {
        IOUtilities.transfer(tempFile, null);
    }

    /* --------------------------------------------------------------
     * Tests for close() overloads
     * -------------------------------------------------------------- */

    @Test
    public void testCloseXmlStreamReaderSuppressesException() {
        XMLStreamReader reader = new ThrowingXMLStreamReader();
        // should not throw
        IOUtilities.close(reader);
    }

    @Test
    public void testCloseXmlStreamWriterSuppressesException() {
        XMLStreamWriter writer = new ThrowingXMLStreamWriter();
        IOUtilities.close(writer);
    }

    @Test
    public void testCloseCloseableSuppressesException() {
        Closeable c = new ThrowingCloseable();
        IOUtilities.close(c);
    }

    @Test
    public void testCloseCloseableNullNoException() {
        IOUtilities.close((Closeable) null);
    }

    @Test
    public void testCloseXmlStreamReaderNullNoException() {
        IOUtilities.close((XMLStreamReader) null);
    }

    @Test
    public void testCloseXmlStreamWriterNullNoException() {
        IOUtilities.close((XMLStreamWriter) null);
    }

    /* --------------------------------------------------------------
     * Tests for flush() overloads
     * -------------------------------------------------------------- */

    @Test
    public void testFlushFlushableSuppressesException() {
        Flushable f = new ThrowingFlushable();
        IOUtilities.flush(f);
    }

    @Test
    public void testFlushFlushableNullNoException() {
        IOUtilities.flush((Flushable) null);
    }

    @Test
    public void testFlushXmlStreamWriterSuppressesException() {
        XMLStreamWriter w = new ThrowingXMLStreamWriter();
        IOUtilities.flush(w);
    }

    @Test
    public void testFlushXmlStreamWriterNullNoException() {
        IOUtilities.flush((XMLStreamWriter) null);
    }

    /* --------------------------------------------------------------
     * Tests for inputStreamToBytes()
     * -------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytesSuccess() throws Exception {
        byte[] data = "bytes conversion".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, result);
    }

    private static class BadInputStream extends InputStream {
        @Override public int read() throws IOException { throw new IOException("read failure"); }
    }

    @Test
    public void testInputStreamToBytesFailureReturnsNull() {
        InputStream bad = new BadInputStream();
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamToBytesNullInput() {
        IOUtilities.inputStreamToBytes(null);
    }

    /* --------------------------------------------------------------
     * Tests for transfer(URLConnection, byte[])
     * -------------------------------------------------------------- */

    @Test
    public void testTransferBytesToConnection() throws Exception {
        byte[] payload = "byte array to conn".getBytes("UTF-8");
        TestURLConnection conn = new TestURLConnection(new byte[0], null);
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    @Test(expected = NullPointerException.class)
    public void testTransferBytesToConnectionNullConn() throws Exception {
        IOUtilities.transfer(null, new byte[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testTransferBytesToConnectionNullBytes() throws Exception {
        TestURLConnection conn = new TestURLConnection(new byte[0], null);
        IOUtilities.transfer(conn, null);
    }

    /* --------------------------------------------------------------
     * Tests for compressBytes() overloads (ByteArrayOutputStream)
     * -------------------------------------------------------------- */

    @Test
    public void testCompressBytesByteArrayOutputStream() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("compress me".getBytes("UTF-8"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);

        byte[] gzipped = compressed.toByteArray();
        // Uncompress and compare
        byte[] uncompressed = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original.toByteArray(), uncompressed);
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesByteArrayOutputStreamNullOrig() throws Exception {
        IOUtilities.compressBytes(null, new ByteArrayOutputStream());
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesByteArrayOutputStreamNullComp() throws Exception {
        IOUtilities.compressBytes(new ByteArrayOutputStream(), null);
    }

    /* --------------------------------------------------------------
     * Tests for compressBytes() overloads (FastByteArrayOutputStream)
     * -------------------------------------------------------------- */

    @Test
    public void testCompressBytesFastByteArrayOutputStream() throws Exception {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("fast compress".getBytes("UTF-8"));
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);

        byte[] gzipped = compressed.toByteArray();
        byte[] uncompressed = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original.toByteArray(), uncompressed);
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesFastByteArrayOutputStreamNullOrig() throws Exception {
        IOUtilities.compressBytes((FastByteArrayOutputStream) null, new FastByteArrayOutputStream());
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesFastByteArrayOutputStreamNullComp() throws Exception {
        IOUtilities.compressBytes(new FastByteArrayOutputStream(), (FastByteArrayOutputStream) null);
    }

    /* --------------------------------------------------------------
     * Tests for compressBytes(byte[]) and overloads
     * -------------------------------------------------------------- */

    @Test
    public void testCompressAndUncompressByteArray() {
        byte[] data = "byte array compression".getBytes();
        byte[] compressed = IOUtilities.compressBytes(data);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(data, uncompressed);
    }

    @Test
    public void testCompressByteArrayWithOffsetAndLength() {
        byte[] data = "1234567890".getBytes(); // 10 bytes
        // compress only "3456" (offset 2, len 4)
        byte[] compressed = IOUtilities.compressBytes(data, 2, 4);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("3456".getBytes(), uncompressed);
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesNullArray() {
        IOUtilities.compressBytes((byte[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void testCompressBytesWithOffsetNullArray() {
        IOUtilities.compressBytes(null, 0, 1);
    }

    /* --------------------------------------------------------------
     * Tests for uncompressBytes()
     * -------------------------------------------------------------- */

    @Test
    public void testUncompressBytesGzipped() {
        byte[] original = "already gzipped".getBytes();
        byte[] gzipped = IOUtilities.compressBytes(original);
        byte[] back = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original, back);
    }

    @Test
    public void testUncompressBytesNonGzippedReturnsSame() {
        byte[] raw = "plain data".getBytes();
        byte[] result = IOUtilities.uncompressBytes(raw);
        assertSame("When not gzipped the same reference should be returned", raw, result);
    }

    @Test(expected = NullPointerException.class)
    public void testUncompressBytesNull() {
        IOUtilities.uncompressBytes(null);
    }

    /* --------------------------------------------------------------
     * Utility methods for the tests
     * -------------------------------------------------------------- */

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static void writeBytesToFile(File f, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
    }
}
