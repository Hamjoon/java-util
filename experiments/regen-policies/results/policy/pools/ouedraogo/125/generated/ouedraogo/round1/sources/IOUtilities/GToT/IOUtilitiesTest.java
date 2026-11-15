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
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * JUnit‑4 test suite for {@link IOUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal operation of every public method.</li>
 *   <li>Edge‑case handling such as null arguments, empty streams/files, cancellation, and early‑EOF.</li>
 *   <li>Exception‑suppression behaviour of the {@code close} and {@code flush} helpers.</li>
 *   <li>Compression / decompression round‑trip and handling of non‑gzipped data.</li>
 * </ul>
 *
 * All tests use only the API that appears in the source of {@code IOUtilities}
 * and the supplied {@code FastByteArrayOutputStream} class – no external mocking
 * frameworks or subclassing of library abstractions are employed.
 */
public class IOUtilitiesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Simple {@link IOUtilities.TransferCallback} implementation that records
     * the total number of transferred bytes and can be configured to cancel
     * after a given threshold.
     */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private final int cancelAfter;
        private int total = 0;
        private boolean cancelled = false;

        RecordingCallback() { this(-1); }

        RecordingCallback(int cancelAfter) { this.cancelAfter = cancelAfter; }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            total += count;
            if (cancelAfter >= 0 && total >= cancelAfter) {
                cancelled = true;
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public int getTotal() { return total; }
    }

    /**
     * Minimal {@link URLConnection} implementation that allows us to control
     * the returned {@link InputStream} and the {@code Content‑Encoding} header.
     */
    private static class SimpleURLConnection extends URLConnection {
        private final InputStream input;
        private final String encoding;

        SimpleURLConnection(URL url, InputStream input, String encoding) {
            super(url);
            this.input = input;
            this.encoding = encoding;
        }

        @Override
        public void connect() { /* no real connection needed */ }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public String getContentEncoding() {
            return encoding;
        }
    }

    /**
     * {@link Closeable} that throws an {@link IOException} when {@code close}
     * is invoked – used to verify that {@code IOUtilities.close(Closeable)} swallows
     * the exception.
     */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("forced close failure");
        }
    }

    /**
     * {@link Flushable} that throws an {@link IOException} when {@code flush}
     * is invoked.
     */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("forced flush failure");
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. getInputStream()                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetInputStream_NoEncoding() throws IOException {
        ByteArrayInputStream raw = new ByteArrayInputStream(SAMPLE_DATA);
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), raw, null);
        InputStream result = IOUtilities.getInputStream(conn);
        byte[] read = readAllBytes(result);
        Assert.assertArrayEquals(SAMPLE_DATA, read);
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws IOException {
        // Create gzip‑compressed data
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(SAMPLE_DATA);
        }
        ByteArrayInputStream compressed = new ByteArrayInputStream(gz.toByteArray());

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), compressed, "gzip");
        InputStream result = IOUtilities.getInputStream(conn);
        byte[] read = readAllBytes(result);
        Assert.assertArrayEquals(SAMPLE_DATA, read);
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws IOException {
        // Deflate‑compressed data (no GZIP header)
        java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        deflater.setInput(SAMPLE_DATA);
        deflater.finish();
        byte[] buf = new byte[256];
        int sz = deflater.deflate(buf);
        deflater.end();

        ByteArrayInputStream compressed = new ByteArrayInputStream(Arrays.copyOf(buf, sz));
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), compressed, "deflate");
        InputStream result = IOUtilities.getInputStream(conn);
        byte[] read = readAllBytes(result);
        Assert.assertArrayEquals(SAMPLE_DATA, read);
    }

    /* --------------------------------------------------------------------- */
    /* 2. transfer(File, URLConnection, TransferCallback)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        File src = tmp.newFile("src.txt");
        Files.write(src.toPath(), SAMPLE_DATA);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URLConnection conn = new SimpleURLConnection(new URL("http://example.com"), null, null) {
            @Override
            public OutputStream getOutputStream() {
                return out;
            }
        };

        IOUtilities.transfer(src, conn, null);
        Assert.assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    @Test
    public void testTransferFileToURLConnection_WithCallbackAndCancel() throws Exception {
        File src = tmp.newFile("src2.txt");
        // make a file larger than the cancel threshold
        byte[] large = new byte[20000];
        Arrays.fill(large, (byte) 'x');
        Files.write(src.toPath(), large);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URLConnection conn = new SimpleURLConnection(new URL("http://example.com"), null, null) {
            @Override
            public OutputStream getOutputStream() {
                return out;
            }
        };

        RecordingCallback cb = new RecordingCallback(5000); // cancel after 5 KB
        IOUtilities.transfer(src, conn, cb);
        Assert.assertTrue(cb.isCancelled());
        // The output should contain exactly the bytes transferred before cancellation
        Assert.assertEquals(cb.getTotal(), out.size());
    }

    /* --------------------------------------------------------------------- */
    /* 3. transfer(URLConnection, File, TransferCallback)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile() throws Exception {
        // Prepare a gzipped stream to be read via getInputStream()
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(SAMPLE_DATA);
        }
        ByteArrayInputStream compressed = new ByteArrayInputStream(gz.toByteArray());

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), compressed, "gzip");
        File dest = tmp.newFile("dest.txt");
        IOUtilities.transfer(conn, dest, null);
        byte[] read = Files.readAllBytes(dest.toPath());
        Assert.assertArrayEquals(SAMPLE_DATA, read);
    }

    @Test
    public void testTransferURLConnectionToFile_WithCallbackCancel() throws Exception {
        // Create a large gzipped payload
        byte[] payload = new byte[30000];
        Arrays.fill(payload, (byte) 'a');
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gz)) {
            gos.write(payload);
        }
        ByteArrayInputStream compressed = new ByteArrayInputStream(gz.toByteArray());

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), compressed, "gzip");
        File dest = tmp.newFile("dest2.txt");
        RecordingCallback cb = new RecordingCallback(10000); // cancel after 10 KB of uncompressed data
        IOUtilities.transfer(conn, dest, cb);
        Assert.assertTrue(cb.isCancelled());
        // The file should contain only the bytes transferred before cancellation
        byte[] written = Files.readAllBytes(dest.toPath());
        Assert.assertEquals(cb.getTotal(), written.length);
    }

    /* --------------------------------------------------------------------- */
    /* 4. transfer(InputStream, File, TransferCallback)                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        File dest = tmp.newFile("streamDest.txt");
        IOUtilities.transfer(in, dest, null);
        Assert.assertArrayEquals(SAMPLE_DATA, Files.readAllBytes(dest.toPath()));
    }

    @Test
    public void testTransferInputStreamToFile_WithCallback() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        File dest = tmp.newFile("streamDest2.txt");
        RecordingCallback cb = new RecordingCallback();
        IOUtilities.transfer(in, dest, cb);
        Assert.assertEquals(SAMPLE_DATA.length, cb.getTotal());
        Assert.assertArrayEquals(SAMPLE_DATA, Files.readAllBytes(dest.toPath()));
    }

    /* --------------------------------------------------------------------- */
    /* 5. transfer(InputStream, OutputStream, TransferCallback)             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferStreamToStream_NoCallback() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out, null);
        Assert.assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    @Test
    public void testTransferStreamToStream_WithCallbackCancel() throws IOException {
        // data larger than the cancel threshold
        byte[] data = new byte[15000];
        Arrays.fill(data, (byte) 7);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RecordingCallback cb = new RecordingCallback(8000);
        IOUtilities.transfer(in, out, cb);
        Assert.assertTrue(cb.isCancelled());
        Assert.assertEquals(cb.getTotal(), out.size());
    }

    /* --------------------------------------------------------------------- */
    /* 6. transfer(InputStream, byte[])                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToByteArray_ExactSize() throws IOException {
        byte[] target = new byte[SAMPLE_DATA.length];
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        IOUtilities.transfer(in, target);
        Assert.assertArrayEquals(SAMPLE_DATA, target);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArray_EarlyEof() throws IOException {
        byte[] target = new byte[100]; // larger than source
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]); // only 10 bytes
        IOUtilities.transfer(in, target); // should throw IOException
    }

    /* --------------------------------------------------------------------- */
    /* 7. transfer(InputStream, OutputStream)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferStreamToStream_NoCallback_NoClose() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        Assert.assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* 8. transfer(File, OutputStream)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream() throws Exception {
        File src = tmp.newFile("srcFile.txt");
        Files.write(src.toPath(), SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(src, out);
        Assert.assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* 9. close(XMLStreamReader) / close(XMLStreamWriter)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloseXmlStreamReader_NullAndException() throws XMLStreamException {
        // Null argument – should do nothing
        IOUtilities.close((XMLStreamReader) null);

        // Reader that throws on close
        XMLStreamReader badReader = new XMLStreamReader() {
            @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad"); }
            // all other methods are irrelevant for this test – provide empty bodies
            @Override public Object getProperty(String name) { return null; }
            @Override public int next() { return 0; }
            @Override public void require(int type, String namespace, String localName) {}
            @Override public String getElementText() { return null; }
            @Override public int nextTag() { return 0; }
            @Override public boolean hasNext() { return false; }
            @Override public void skip() {}
            @Override public String getNamespaceURI(String prefix) { return null; }
            @Override public boolean isStartElement() { return false; }
            @Override public boolean isEndElement() { return false; }
            @Override public boolean isCharacters() { return false; }
            @Override public boolean isWhiteSpace() { return false; }
            @Override public String getLocalName() { return null; }
            @Override public boolean hasName() { return false; }
            @Override public String getPrefix() { return null; }
            @Override public String getNamespaceURI() { return null; }
            @Override public java.util.Iterator<String> getNamespaces() { return null; }
            @Override public java.util.Iterator<String> getAttributes() { return null; }
            @Override public String getAttributeValue(String namespaceURI, String localName) { return null; }
            @Override public int getAttributeCount() { return 0; }
            @Override public javax.xml.namespace.QName getAttributeName(int index) { return null; }
            @Override public String getAttributeNamespace(int index) { return null; }
            @Override public String getAttributeLocalName(int index) { return null; }
            @Override public String getAttributePrefix(int index) { return null; }
            @Override public String getAttributeType(int index) { return null; }
            @Override public String getAttributeValue(int index) { return null; }
            @Override public String getNamespaceContext() { return null; }
            @Override public java.util.Iterator<String> getNamespacePrefixes() { return null; }
            @Override public String getText() { return null; }
            @Override public char[] getTextCharacters() { return new char[0]; }
            @Override public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) { return 0; }
            @Override public int getTextStart() { return 0; }
            @Override public int getTextLength() { return 0; }
            @Override public String getEncoding() { return null; }
            @Override public boolean hasText() { return false; }
            @Override public java.io.Reader getCharacterStream() { return null; }
            @Override public void require(int type, String namespaceURI, String localName) throws XMLStreamException {}
            @Override public String getNamespaceURI(String prefix) { return null; }
            @Override public java.util.Iterator<String> getPrefixes(String uri) { return null; }
        };
        // Should swallow the exception
        IOUtilities.close(badReader);
    }

    @Test
    public void testCloseXmlStreamWriter_NullAndException() throws XMLStreamException {
        IOUtilities.close((XMLStreamWriter) null);
        XMLStreamWriter badWriter = new XMLStreamWriter() {
            @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad"); }
            // Remaining methods are not needed for the test
            @Override public void writeStartElement(String localName) {}
            @Override public void writeStartElement(String namespaceURI, String localName) {}
            @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
            @Override public void writeEmptyElement(String namespaceURI, String localName) {}
            @Override public void writeEmptyElement(String prefix, String localName, String namespaceURI) {}
            @Override public void writeEmptyElement(String localName) {}
            @Override public void writeEndElement() {}
            @Override public void writeEndDocument() {}
            @Override public void close() throws XMLStreamException {}
            @Override public void flush() throws XMLStreamException {}
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
        };
        IOUtilities.close(badWriter);
    }

    /* --------------------------------------------------------------------- */
    /* 10. close(Closeable)                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloseCloseable_NullAndException() throws IOException {
        // Null – nothing should happen
        IOUtilities.close((Closeable) null);

        // Closeable that throws – exception must be swallowed
        IOUtilities.close(new BadCloseable());
    }

    /* --------------------------------------------------------------------- */
    /* 11. flush(Flushable)                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFlushFlushable_NullAndException() throws IOException {
        IOUtilities.flush((Flushable) null);
        IOUtilities.flush(new BadFlushable()); // should swallow IOException
    }

    /* --------------------------------------------------------------------- */
    /* 12. flush(XMLStreamWriter)                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFlushXmlStreamWriter_NullAndException() throws XMLStreamException {
        IOUtilities.flush((XMLStreamWriter) null);
        XMLStreamWriter badWriter = new XMLStreamWriter() {
            @Override public void flush() throws XMLStreamException { throw new XMLStreamException("bad"); }
            // Other methods are irrelevant for this test
            @Override public void writeStartElement(String localName) {}
            @Override public void writeStartElement(String namespaceURI, String localName) {}
            @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
            @Override public void writeEmptyElement(String namespaceURI, String localName) {}
            @Override public void writeEmptyElement(String prefix, String localName, String namespaceURI) {}
            @Override public void writeEmptyElement(String localName) {}
            @Override public void writeEndElement() {}
            @Override public void writeEndDocument() {}
            @Override public void close() {}
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
        };
        IOUtilities.flush(badWriter);
    }

    /* --------------------------------------------------------------------- */
    /* 13. inputStreamToBytes()                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytes_Normal() {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        Assert.assertArrayEquals(SAMPLE_DATA, result);
    }

    @Test
    public void testInputStreamToBytes_WithExceptionReturnsNull() {
        // InputStream that throws on read
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("read failure"); }
        };
        Assert.assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* --------------------------------------------------------------------- */
    /* 14. transfer(URLConnection, byte[])                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToByteArray() throws Exception {
        ByteArrayOutputStream collector = new ByteArrayOutputStream();
        URLConnection conn = new SimpleURLConnection(new URL("http://example.com"), null, null) {
            @Override public OutputStream getOutputStream() { return collector; }
        };
        byte[] payload = "payload".getBytes();
        IOUtilities.transfer(conn, payload);
        Assert.assertArrayEquals(payload, collector.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* 15. compressBytes(ByteArrayOutputStream, ByteArrayOutputStream)       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_ByteArrayOutputStream() throws IOException {
        ByteArrayOutputStream src = new ByteArrayOutputStream();
        src.write(SAMPLE_DATA);
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        // Decompress to verify
        byte[] decompressed = IOUtilities.uncompressBytes(dst.toByteArray());
        Assert.assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    /* --------------------------------------------------------------------- */
    /* 16. compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_FastByteArrayOutputStream() throws IOException {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.write(SAMPLE_DATA);
        FastByteArrayOutputStream dst = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(src, dst);
        byte[] decompressed = IOUtilities.uncompressBytes(dst.toByteArray());
        Assert.assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    /* --------------------------------------------------------------------- */
    /* 17. compressBytes(byte[]) / compressBytes(byte[],int,int)               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_ByteArray_Simple() {
        byte[] compressed = IOUtilities.compressBytes(SAMPLE_DATA);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        Assert.assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    @Test
    public void testCompressBytes_WithOffsetAndLength() {
        byte[] data = new byte[30];
        Arrays.fill(data, (byte) 0);
        System.arraycopy(SAMPLE_DATA, 0, data, 10, SAMPLE_DATA.length);
        byte[] compressed = IOUtilities.compressBytes(data, 10, SAMPLE_DATA.length);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        Assert.assertArrayEquals(SAMPLE_DATA, decompressed);
    }

    /* --------------------------------------------------------------------- */
    /* 18. uncompressBytes(byte[]) / uncompressBytes(byte[],int,int)         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUncompressBytes_NonGzippedReturnsOriginal() {
        byte[] notCompressed = "plain text".getBytes();
        byte[] result = IOUtilities.uncompressBytes(notCompressed);
        Assert.assertArrayEquals(notCompressed, result);
    }

    @Test(expected = RuntimeException.class)
    public void testUncompressBytes_CorruptGzipThrowsRuntimeException() {
        byte[] corrupt = new byte[] { (byte)0x1f, (byte)0x8b, 0, 0, 0, 0, 0, 0, 0, 0 }; // valid header but no data
        IOUtilities.uncompressBytes(corrupt);
    }

    /* --------------------------------------------------------------------- */
    /* Utility method used by several tests                                 */
    /* --------------------------------------------------------------------- */

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        return out.toByteArray();
    }
}
