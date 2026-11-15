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
import java.util.Arrays;
import java.util.zip.*;

import static org.junit.Assert.*;

public class IOUtilitiesTest {

    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("ioutil_test", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(SAMPLE_DATA);
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* ---------------------------------------------------------------------- */

    /** Simple URLConnection that works with in‑memory streams. */
    private static class MemoryURLConnection extends URLConnection {
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final String contentEncoding;

        protected MemoryURLConnection(URL url, byte[] data, String encoding) {
            super(url);
            this.input = new ByteArrayInputStream(data);
            this.contentEncoding = encoding;
        }

        @Override
        public void connect() { /* no network */ }

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

    /** Simple TransferCallback implementation that records calls and can cancel. */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private int totalBytes = 0;
        private int callCount = 0;
        private final int cancelAfter; // cancel after this many bytes, -1 = never

        public RecordingCallback(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            totalBytes += count;
            callCount++;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter >= 0 && totalBytes >= cancelAfter;
        }

        public int getTotalBytes() {
            return totalBytes;
        }

        public int getCallCount() {
            return callCount;
        }
    }

    /** Closeable that always throws IOException on close. */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("bad close");
        }
    }

    /** Flushable that always throws IOException on flush. */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("bad flush");
        }
    }

    /** XMLStreamReader that throws on close. */
    private static class BadXMLReader implements XMLStreamReader {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        // All other methods are irrelevant for this test – we provide empty bodies.
        @Override public Object getProperty(String name) { return null; }
        @Override public int next() { return 0; }
        @Override public void require(int type, String namespaceURI, String localName) {}
        @Override public String getElementText() { return null; }
        @Override public int nextTag() { return 0; }
        @Override public boolean hasNext() { return false; }
        @Override public void closeElement() {}
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public String getPrefix(String uri) { return null; }
        @Override public int getEventType() { return 0; }
        @Override public String getText() { return null; }
        @Override public char[] getTextCharacters() { return new char[0]; }
        @Override public int getTextStart() { return 0; }
        @Override public int getTextLength() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public boolean hasName() { return false; }
        @Override public String getNamespaceURI() { return null; }
        @Override public String getPrefix() { return null; }
        @Override public String getVersion() { return null; }
        @Override public boolean isStandalone() { return false; }
        @Override public boolean standaloneSet() { return false; }
        @Override public String getCharacterEncodingScheme() { return null; }
        @Override public String getPITarget() { return null; }
        @Override public String getPIData() { return null; }
        @Override public Object getProperty(Object name) { return null; }
        @Override public void setProperty(Object name, Object value) {}
        @Override public void setNamespaceContext(javax.xml.namespace.NamespaceContext context) {}
        @Override public javax.xml.namespace.NamespaceContext getNamespaceContext() { return null; }
        @Override public void setLocation(String location) {}
        @Override public String getLocation() { return null; }
    }

    /** XMLStreamWriter that throws on close/flush. */
    private static class BadXMLWriter implements XMLStreamWriter {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        @Override public void flush() throws XMLStreamException { throw new XMLStreamException("bad flush"); }
        // All other methods are no‑ops.
        @Override public void writeStartElement(String localName) {}
        @Override public void writeStartElement(String namespaceURI, String localName) {}
        @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
        @Override public void writeEndElement() {}
        @Override public void writeEndDocument() {}
        @Override public void closeTag() {}
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

    /* ---------------------------------------------------------------------- */
    /* Tests for getInputStream                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetInputStreamGzip() throws Exception {
        // compress SAMPLE_DATA using GZIP
        ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gzOut)) {
            gos.write(SAMPLE_DATA);
        }
        byte[] gzipped = gzOut.toByteArray();

        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), gzipped, "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        IOUtilities.transfer(is, result);
        assertArrayEquals(SAMPLE_DATA, result.toByteArray());
        is.close();
    }

    @Test
    public void testGetInputStreamDeflate() throws Exception {
        // raw deflate (no ZLIB header) using Deflater
        Deflater deflater = new Deflater();
        deflater.setInput(SAMPLE_DATA);
        deflater.finish();
        byte[] buffer = new byte[256];
        int len = deflater.deflate(buffer);
        byte[] deflated = Arrays.copyOf(buffer, len);

        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), deflated, "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        IOUtilities.transfer(is, result);
        assertArrayEquals(SAMPLE_DATA, result.toByteArray());
        is.close();
    }

    @Test
    public void testGetInputStreamNoEncoding() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), SAMPLE_DATA, null);
        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
        is.close();
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(File, URLConnection, TransferCallback)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), new byte[0], null);
        RecordingCallback cb = new RecordingCallback(-1);
        IOUtilities.transfer(tempFile, conn, cb);
        assertArrayEquals(SAMPLE_DATA, conn.getWrittenBytes());
        assertEquals(SAMPLE_DATA.length, cb.getTotalBytes());
        assertTrue(cb.getCallCount() > 0);
    }

    @Test
    public void testTransferFileToURLConnectionCancelled() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), new byte[0], null);
        // Cancel after half of the data has been transferred
        RecordingCallback cb = new RecordingCallback(SAMPLE_DATA.length / 2);
        IOUtilities.transfer(tempFile, conn, cb);
        byte[] written = conn.getWrittenBytes();
        // Written bytes should be <= half of original length
        assertTrue(written.length <= SAMPLE_DATA.length / 2 + 1);
        assertTrue(cb.isCancelled());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, File, TransferCallback)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), SAMPLE_DATA, null);
        File outFile = File.createTempFile("ioutil_out", ".tmp");
        outFile.deleteOnExit();
        RecordingCallback cb = new RecordingCallback(-1);
        IOUtilities.transfer(conn, outFile, cb);
        byte[] fileBytes = readAllBytes(outFile);
        assertArrayEquals(SAMPLE_DATA, fileBytes);
        assertEquals(SAMPLE_DATA.length, cb.getTotalBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, File, TransferCallback)                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        File outFile = File.createTempFile("ioutil_is2file", ".tmp");
        outFile.deleteOnExit();
        RecordingCallback cb = new RecordingCallback(-1);
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, outFile, cb);
        }
        assertArrayEquals(SAMPLE_DATA, readAllBytes(outFile));
        assertEquals(SAMPLE_DATA.length, cb.getTotalBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream, TransferCallback)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferStreamWithCallback() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback(-1);
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, out, cb);
        }
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
        assertEquals(SAMPLE_DATA.length, cb.getTotalBytes());
    }

    @Test
    public void testTransferStreamWithCancellation() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Cancel after 10 bytes
        RecordingCallback cb = new RecordingCallback(10);
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, out, cb);
        }
        assertTrue(out.size() <= 10);
        assertTrue(cb.isCancelled());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, byte[])                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToByteArrayExact() throws Exception {
        byte[] target = new byte[SAMPLE_DATA.length];
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, target);
        }
        assertArrayEquals(SAMPLE_DATA, target);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArrayShort() throws Exception {
        byte[] target = new byte[SAMPLE_DATA.length + 10]; // larger than source
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, target);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            IOUtilities.transfer(in, out);
        }
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(File, OutputStream)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(tempFile, out);
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for close methods                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloseNullSafe() {
        // All close methods should ignore null without throwing
        IOUtilities.close((Closeable) null);
        IOUtilities.close((XMLStreamReader) null);
        IOUtilities.close((XMLStreamWriter) null);
    }

    @Test
    public void testCloseBadCloseableSwallowsException() {
        IOUtilities.close(new BadCloseable()); // should not propagate
    }

    @Test
    public void testCloseBadXMLReaderSwallowsException() {
        IOUtilities.close(new BadXMLReader());
    }

    @Test
    public void testCloseBadXMLWriterSwallowsException() {
        IOUtilities.close(new BadXMLWriter());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for flush methods                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFlushNullSafe() {
        IOUtilities.flush((Flushable) null);
        IOUtilities.flush((XMLStreamWriter) null);
    }

    @Test
    public void testFlushBadFlushableSwallowsException() {
        IOUtilities.flush(new BadFlushable()); // should not throw
    }

    @Test
    public void testFlushBadXMLWriterSwallowsException() {
        IOUtilities.flush(new BadXMLWriter());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for inputStreamToBytes                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytesSuccess() throws Exception {
        try (InputStream in = new ByteArrayInputStream(SAMPLE_DATA)) {
            byte[] result = IOUtilities.inputStreamToBytes(in);
            assertArrayEquals(SAMPLE_DATA, result);
        }
    }

    @Test
    public void testInputStreamToBytesOnErrorReturnsNull() throws Exception {
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("forced"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, byte[])                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionWithBytes() throws Exception {
        MemoryURLConnection conn = new MemoryURLConnection(new URL("http://example.com"), new byte[0], null);
        IOUtilities.transfer(conn, SAMPLE_DATA);
        assertArrayEquals(SAMPLE_DATA, conn.getWrittenBytes());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for compression utilities                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompressBytesByteArray() {
        byte[] compressed = IOUtilities.compressBytes(SAMPLE_DATA);
        // Decompress and verify equality
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    @Test
    public void testCompressBytesWithOffsetAndLength() {
        byte[] prefix = "prefix-".getBytes();
        byte[] suffix = "-suffix".getBytes();
        byte[] combined = new byte[prefix.length + SAMPLE_DATA.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(SAMPLE_DATA, 0, combined, prefix.length, SAMPLE_DATA.length);
        System.arraycopy(suffix, 0, combined, prefix.length + SAMPLE_DATA.length, suffix.length);

        byte[] compressed = IOUtilities.compressBytes(combined, prefix.length, SAMPLE_DATA.length);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    @Test
    public void testCompressBytesByteArrayOutputStream() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(SAMPLE_DATA);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    @Test
    public void testCompressBytesFastByteArrayOutputStream() throws Exception {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(SAMPLE_DATA);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    @Test
    public void testUncompressBytesNonGzippedReturnsOriginal() {
        byte[] notGzipped = new byte[]{1, 2, 3, 4, 5};
        assertArrayEquals(notGzipped, IOUtilities.uncompressBytes(notGzipped));
    }

    /* ---------------------------------------------------------------------- */
    /* Helper method to read entire file into a byte array                     */
    /* ---------------------------------------------------------------------- */

    private static byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtilities.transfer(fis, baos);
            return baos.toByteArray();
        }
    }
}
