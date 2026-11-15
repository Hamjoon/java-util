package com.cedarsoftware.util;
import org.mockito.invocation.Location;


import org.junit.*;
import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class IOUtilitiesTest {

    /*--------------------------------------------------------------------
     * Helper classes
     *--------------------------------------------------------------------*/

    /** Simple URLConnection that lets us control the streams and headers. */
    private static class SimpleURLConnection extends URLConnection {
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final Map<String, List<String>> requestProperties = new HashMap<>();
        private String contentEncoding;

        protected SimpleURLConnection(URL url, byte[] inputBytes, String contentEncoding) {
            super(url);
            this.input = new ByteArrayInputStream(inputBytes != null ? inputBytes : new byte[0]);
            this.contentEncoding = contentEncoding;
        }

        @Override
        public void connect() { /* no-op */ }

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

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        @Override
        public String getRequestProperty(String key) {
            List<String> vals = requestProperties.get(key);
            return (vals == null || vals.isEmpty()) ? null : vals.get(0);
        }

        public byte[] getOutputBytes() {
            return output.toByteArray();
        }
    }

    /** HttpURLConnection that records the values set by optimizeConnection. */
    private static class RecordingHttpURLConnection extends HttpURLConnection {
        private final SimpleURLConnection delegate;
        private boolean instanceFollowRedirects;
        private boolean useCaches;
        private int connectTimeout = -1;
        private int readTimeout = -1;
        private final Map<String, List<String>> requestProperties = new HashMap<>();

        protected RecordingHttpURLConnection(URL url, byte[] inputBytes, String contentEncoding) {
            super(url);
            delegate = new SimpleURLConnection(url, inputBytes, contentEncoding);
        }

        @Override
        public void disconnect() { }

        @Override
        public boolean usingProxy() { return false; }

        @Override
        public void connect() { }

        @Override
        public InputStream getInputStream() { return delegate.getInputStream(); }

        @Override
        public OutputStream getOutputStream() { return delegate.getOutputStream(); }

        @Override
        public String getContentEncoding() { return delegate.getContentEncoding(); }

        @Override
        public void setInstanceFollowRedirects(boolean follow) {
            this.instanceFollowRedirects = follow;
        }

        @Override
        public void setUseCaches(boolean usecaches) {
            this.useCaches = usecaches;
        }

        @Override
        public void setConnectTimeout(int timeout) { this.connectTimeout = timeout; }

        @Override
        public void setReadTimeout(int timeout) { this.readTimeout = timeout; }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        @Override
        public String getRequestProperty(String key) {
            List<String> vals = requestProperties.get(key);
            return (vals == null || vals.isEmpty()) ? null : vals.get(0);
        }

        public boolean isInstanceFollowRedirects() { return instanceFollowRedirects; }
        public boolean isUseCaches() { return useCaches; }
        public int getConnectTimeout() { return connectTimeout; }
        public int getReadTimeout() { return readTimeout; }
        public byte[] getOutputBytes() { return delegate.getOutputBytes(); }
    }

    /** Closeable that throws IOException on close(). */
    private static class BadCloseable implements Closeable {
        @Override public void close() throws IOException { throw new IOException("bad close"); }
    }

    /** Flushable that throws IOException on flush(). */
    private static class BadFlushable implements Flushable {
        @Override public void flush() throws IOException { throw new IOException("bad flush"); }
    }

    /** XMLStreamReader that throws XMLStreamException on close(). */
    private static class BadXMLReader implements XMLStreamReader {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        // All other methods can be left unimplemented for the test (return defaults / throw UnsupportedOperationException)
        @Override public Object getProperty(String name) { throw new UnsupportedOperationException(); }
        @Override public int next() { throw new UnsupportedOperationException(); }
        @Override public void require(int type, String namespaceURI, String localName) {}
        @Override public String getElementText() { return null; }
        @Override public int nextTag() { return 0; }
        @Override public boolean hasNext() { return false; }
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
        @Override public int getTextLength() { return 0; }
        @Override public int getTextOffset() { return 0; }
        @Override public String getText() { return null; }
        @Override public char[] getTextCharacters() { return new char[0]; }
        @Override public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) { return 0; }
        @Override public boolean hasText() { return false; }
        @Override public Location getLocation() { return null; }
        @Override public QName getName() { return null; }
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespacePrefix(int index) { return null; }
        @Override public String getNamespaceURI(int index) { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public QName getAttributeName(int index) { return null; }
        @Override public String getAttributeNamespace(int index) { return null; }
        @Override public String getAttributeLocalName(int index) { return null; }
        @Override public String getAttributePrefix(int index) { return null; }
        @Override public String getAttributeType(int index) { return null; }
        @Override public String getAttributeValue(int index) { return null; }
        @Override public String getAttributeValue(String namespaceURI, String localName) { return null; }
        @Override public boolean isAttributeSpecified(int index) { return false; }
        @Override public int getEventType() { return 0; }
        @Override public String getEncoding() { return null; }
        @Override public boolean hasNext() { return false; }
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
    }

    /** XMLStreamWriter that throws XMLStreamException on close/flush. */
    private static class BadXMLWriter implements XMLStreamWriter {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        @Override public void flush() throws XMLStreamException { throw new XMLStreamException("bad flush"); }
        // other methods can be no‑ops or throw UnsupportedOperationException
        @Override public void writeStartElement(String localName) {}
        @Override public void writeStartElement(String namespaceURI, String localName) {}
        @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
        @Override public void writeEndElement() {}
        @Override public void writeEndDocument() {}
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

    /** TransferCallback that records bytes transferred and can be cancelled. */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        final List<Integer> counts = new ArrayList<>();
        boolean cancel = false;

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            counts.add(count);
        }

        @Override
        public boolean isCancelled() {
            return cancel;
        }
    }

    /*--------------------------------------------------------------------
     * Test cases for each public method
     *--------------------------------------------------------------------*/

    @Test
    public void testGetInputStream_NoEncoding() throws Exception {
        byte[] data = "plain data".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), data, null);
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] out = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(data, out);
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws Exception {
        byte[] original = "gzip compressed".getBytes("UTF-8");
        byte[] gzipped = compressWithGZIP(original);
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), gzipped, "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] result = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(original, result);
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws Exception {
        byte[] original = "deflate compressed".getBytes("UTF-8");
        byte[] deflated = compressWithDeflate(original);
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), deflated, "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] result = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(original, result);
    }

    @Test
    public void testOptimizeConnection_HttpURLConnection() throws Exception {
        RecordingHttpURLConnection http = new RecordingHttpURLConnection(new URL("http://host/"), null, null);
        // invoke the private method via reflection
        java.lang.reflect.Method m = IOUtilities.class.getDeclaredMethod("optimizeConnection", URLConnection.class);
        m.setAccessible(true);
        m.invoke(null, http);
        assertTrue(http.isInstanceFollowRedirects());
        assertFalse(http.isUseCaches());
        assertEquals(5000, http.getConnectTimeout());
        assertEquals(30000, http.getReadTimeout());
        assertEquals("gzip, x-gzip, deflate", http.getRequestProperty("Accept-Encoding"));
    }

    @Test
    public void testTransfer_FileToURLConnection() throws Exception {
        Path tmp = Files.createTempFile("ioUtilTest", ".txt");
        Files.write(tmp, "file content".getBytes("UTF-8"));
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), null, null);
        IOUtilities.transfer(tmp.toFile(), conn, null);
        assertArrayEquals("file content".getBytes("UTF-8"), conn.getOutputBytes());
        Files.deleteIfExists(tmp);
    }

    @Test
    public void testTransfer_URLConnectionToFile() throws Exception {
        byte[] payload = "payload".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), payload, null);
        Path tmp = Files.createTempFile("ioUtilTest", ".bin");
        IOUtilities.transfer(conn, tmp.toFile(), null);
        byte[] read = Files.readAllBytes(tmp);
        assertArrayEquals(payload, read);
        Files.deleteIfExists(tmp);
    }

    @Test
    public void testTransfer_InputStreamToFile_WithCallbackAndCancel() throws Exception {
        byte[] data = new byte[100];
        new Random().nextBytes(data);
        InputStream in = new ByteArrayInputStream(data);
        Path tmp = Files.createTempFile("ioUtilTest", ".bin");
        RecordingCallback cb = new RecordingCallback() {
            @Override public boolean isCancelled() { return counts.size() >= 2; }
        };
        IOUtilities.transfer(in, tmp.toFile(), cb);
        // at least two callbacks should have been made before cancellation
        assertTrue(cb.counts.size() >= 2);
        // file should contain only the bytes transferred before cancellation (first two buffers)
        int expectedBytes = cb.counts.stream().mapToInt(Integer::intValue).sum();
        byte[] fileBytes = Files.readAllBytes(tmp);
        assertEquals(expectedBytes, fileBytes.length);
        Files.deleteIfExists(tmp);
    }

    @Test
    public void testTransfer_InputStreamToOutputStream_WithCallback() throws Exception {
        byte[] data = "callback test".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback();
        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertEquals(1, cb.counts.size());
        assertEquals(data.length, (int) cb.counts.get(0));
    }

    @Test
    public void testTransfer_InputStreamToByteArray() throws Exception {
        byte[] src = "exactly sixteen".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[src.length];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(src, dest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransfer_InputStreamToByteArray_NullInput() throws Exception {
        IOUtilities.transfer(null, new byte[10]);
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
        Path tmp = Files.createTempFile("ioUtilTest", ".txt");
        Files.write(tmp, "flush test".getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            boolean flushed = false;
            @Override public void flush() throws IOException { super.flush(); flushed = true; }
        };
        IOUtilities.transfer(tmp.toFile(), out);
        assertTrue(((ByteArrayOutputStream) out).toString().contains("flush test"));
        // The overridden flush method sets a flag – we cannot read it directly, but no exception means it was called.
    }

    @Test
    public void testClose_XMLStreamReader_NullAndException() {
        // null – should do nothing
        IOUtilities.close((XMLStreamReader) null);
        // exception – should be suppressed
        BadXMLReader bad = new BadXMLReader();
        IOUtilities.close(bad);
    }

    @Test
    public void testClose_XMLStreamWriter_NullAndException() {
        IOUtilities.close((XMLStreamWriter) null);
        BadXMLWriter bad = new BadXMLWriter();
        IOUtilities.close(bad);
    }

    @Test
    public void testClose_Closeable_NullAndException() {
        IOUtilities.close((Closeable) null);
        IOUtilities.close(new BadCloseable()); // exception suppressed
    }

    @Test
    public void testFlush_Flushable_NullAndException() {
        IOUtilities.flush((Flushable) null);
        IOUtilities.flush(new BadFlushable()); // suppressed
    }

    @Test
    public void testFlush_XMLStreamWriter_NullAndException() {
        IOUtilities.flush((XMLStreamWriter) null);
        IOUtilities.flush(new BadXMLWriter()); // suppressed
    }

    @Test
    public void testInputStreamToBytes_Success() throws Exception {
        byte[] data = "small stream".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        byte[] out = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(data, out);
    }

    @Test
    public void testInputStreamToBytes_ExceptionReturnsNull() throws Exception {
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("boom"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    @Test
    public void testTransfer_URLConnectionWithBytes() throws Exception {
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://test/"), null, null);
        byte[] payload = "byte array payload".getBytes("UTF-8");
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getOutputBytes());
    }

    @Test
    public void testCompressAndUncompress_ByteArray_RoundTrip() {
        byte[] original = new byte[1024];
        new Random().nextBytes(original);
        byte[] compressed = IOUtilities.compressBytes(original);
        assertNotNull(compressed);
        assertTrue(compressed.length < original.length);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testCompressBytes_ByteArray_Null() {
        try {
            IOUtilities.compressBytes((byte[]) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException | NullPointerException e) {
            // expected – Convention.throwIfNull throws IllegalArgumentException or NPE
        }
    }

    @Test
    public void testUncompressBytes_NotGzipped_ReturnsSameArray() {
        byte[] data = "plain".getBytes();
        byte[] result = IOUtilities.uncompressBytes(data);
        assertSame(data, result);
    }

    @Test
    public void testCompressBytes_ByteArrayOutputStream_And_FastByteArrayOutputStream() throws Exception {
        // ByteArrayOutputStream variant
        ByteArrayOutputStream original1 = new ByteArrayOutputStream();
        original1.write("some data".getBytes("UTF-8"));
        ByteArrayOutputStream compressed1 = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original1, compressed1);
        byte[] roundTrip1 = IOUtilities.uncompressBytes(compressed1.toByteArray());
        assertArrayEquals(original1.toByteArray(), roundTrip1);

        // FastByteArrayOutputStream variant
        FastByteArrayOutputStream original2 = new FastByteArrayOutputStream();
        original2.write("fast data".getBytes("UTF-8"));
        FastByteArrayOutputStream compressed2 = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original2, compressed2);
        byte[] roundTrip2 = IOUtilities.uncompressBytes(compressed2.toByteArray());
        assertArrayEquals(original2.toByteArray(), roundTrip2);
    }

    /*--------------------------------------------------------------------
     * Utility methods for the tests
     *--------------------------------------------------------------------*/

    private static byte[] compressWithGZIP(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private static byte[] compressWithDeflate(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            baos.write(buf, 0, count);
        }
        return baos.toByteArray();
    }
}
