package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link IOUtilities}.
 * <p>
 * The tests cover every public static method, exercising normal operation,
 * edge conditions and the silent‑failure behaviour of the close/flush helpers.
 * </p>
 */
public class IOUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /** Simple {@link URLConnection} implementation that allows us to inject
     *  an {@link InputStream}, an {@link OutputStream} and a content‑encoding
     *  string. */
    private static class DummyURLConnection extends URLConnection {
        private final InputStream in;
        private final OutputStream out;
        private final String encoding;

        protected DummyURLConnection(InputStream in, OutputStream out, String encoding) {
            super(null);
            this.in = in;
            this.out = out;
            this.encoding = encoding;
        }

        @Override
        public void connect() throws IOException {
            // No real connection needed.
        }

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public String getContentEncoding() {
            return encoding;
        }
    }

    /** Records bytes transferred and optionally cancels after a given threshold. */
    private static class RecordingCallback implements IOUtilities.TransferCallback {
        private int totalBytes = 0;
        private int callCount = 0;
        private final int cancelAfter; // cancel after this many bytes, 0 = never cancel

        RecordingCallback(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count) {
            totalBytes += count;
            callCount++;
        }

        @Override
        public boolean isCancelled() {
            return cancelAfter > 0 && totalBytes >= cancelAfter;
        }

        int getTotalBytes() { return totalBytes; }
        int getCallCount() { return callCount; }
    }

    /** Closeable that always throws an IOException when closed. */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("forced close failure");
        }
    }

    /** Flushable that always throws an IOException when flushed. */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("forced flush failure");
        }
    }

    /** Minimal XMLStreamReader that throws an XMLStreamException on close. */
    private static class BadXMLStreamReader implements XMLStreamReader {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        // The rest of the methods are not used in the test; they return defaults.
        @Override public Object getProperty(String name) { return null; }
        @Override public int next() { return 0; }
        @Override public void require(int type, String namespaceURI, String localName) {}
        @Override public String getElementText() { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public String getAttributeLocalName(int index) { return null; }
        @Override public QName getAttributeName(int index) { return null; }
        @Override public String getAttributeNamespace(int index) { return null; }
        @Override public String getAttributePrefix(int index) { return null; }
        @Override public String getAttributeType(int index) { return null; }
        @Override public String getAttributeValue(int index) { return null; }
        @Override public String getAttributeValue(String namespaceURI, String localName) { return null; }
        @Override public boolean isAttributeSpecified(int index) { return false; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespacePrefix(int index) { return null; }
        @Override public String getNamespaceURI(int index) { return null; }
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public int getEventType() { return 0; }
        @Override public String getText() { return null; }
        @Override public char[] getTextCharacters() { return new char[0]; }
        @Override public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) { return 0; }
        @Override public int getTextStart() { return 0; }
        @Override public int getTextLength() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public boolean hasName() { return false; }
        @Override public QName getName() { return null; }
        @Override public String getNamespaceURI() { return null; }
        @Override public String getPrefix() { return null; }
        @Override public String getVersion() { return null; }
        @Override public boolean isStandalone() { return false; }
        @Override public boolean standaloneSet() { return false; }
        @Override public String getCharacterEncodingScheme() { return null; }
        @Override public String getPITarget() { return null; }
        @Override public boolean hasNext() { return false; }
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
    }

    /** Minimal XMLStreamWriter that throws an XMLStreamException on close. */
    private static class BadXMLStreamWriter implements XMLStreamWriter {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad close"); }
        @Override public void flush() throws XMLStreamException {}
        // All other methods are no‑ops.
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
    }

    /* --------------------------------------------------------------------- */
    /* Tests for getInputStream                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetInputStream_NoEncoding() throws IOException {
        byte[] data = "plain text".getBytes("UTF-8");
        InputStream src = new ByteArrayInputStream(data);
        DummyURLConnection conn = new DummyURLConnection(src, null, null);

        InputStream result = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(result, out);
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testGetInputStream_GzipEncoding() throws IOException {
        byte[] original = "gzip compressed".getBytes("UTF-8");
        // compress using IOUtilities.compressBytes to guarantee compatibility
        byte[] gzipped = IOUtilities.compressBytes(original);

        InputStream src = new ByteArrayInputStream(gzipped);
        DummyURLConnection conn = new DummyURLConnection(src, null, "gzip");

        InputStream result = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(result, out);
        assertArrayEquals(original, out.toByteArray());
    }

    @Test
    public void testGetInputStream_DeflateEncoding() throws IOException {
        byte[] original = "deflate compressed".getBytes("UTF-8");
        // produce a deflate stream manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.util.zip.Deflater def = new java.util.zip.Deflater();
        def.setInput(original);
        def.finish();
        byte[] buffer = new byte[1024];
        while (!def.finished()) {
            int cnt = def.deflate(buffer);
            baos.write(buffer, 0, cnt);
        }
        def.end();
        byte[] deflated = baos.toByteArray();

        InputStream src = new ByteArrayInputStream(deflated);
        DummyURLConnection conn = new DummyURLConnection(src, null, "deflate");

        InputStream result = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(result, out);
        assertArrayEquals(original, out.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(File, URLConnection, TransferCallback)             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferFileToURLConnection() throws Exception {
        // Prepare source file
        File srcFile = File.createTempFile("ioUtilSrc", ".txt");
        srcFile.deleteOnExit();
        byte[] data = "file‑to‑connection".getBytes("UTF-8");
        Files.write(srcFile.toPath(), data);

        // Destination that captures written bytes
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        DummyURLConnection conn = new DummyURLConnection(null, dest, null);

        RecordingCallback cb = new RecordingCallback(0);
        IOUtilities.transfer(srcFile, conn, cb);

        assertArrayEquals(data, dest.toByteArray());
        assertEquals(data.length, cb.getTotalBytes());
        assertTrue(cb.getCallCount() > 0);
    }

    @Test
    public void testTransferFileToURLConnection_Cancelled() throws Exception {
        File srcFile = File.createTempFile("ioUtilCancel", ".txt");
        srcFile.deleteOnExit();
        byte[] data = new byte[5000]; // larger than buffer to guarantee multiple callbacks
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        Files.write(srcFile.toPath(), data);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        DummyURLConnection conn = new DummyURLConnection(null, dest, null);

        // Cancel after 2000 bytes
        RecordingCallback cb = new RecordingCallback(2000);
        IOUtilities.transfer(srcFile, conn, cb);

        assertTrue(dest.toByteArray().length < data.length);
        assertTrue(cb.isCancelled());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, File, TransferCallback)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionToFile() throws Exception {
        byte[] payload = "connection‑to‑file".getBytes("UTF-8");
        InputStream src = new ByteArrayInputStream(payload);
        DummyURLConnection conn = new DummyURLConnection(src, null, null);

        File destFile = File.createTempFile("ioUtilDest", ".txt");
        destFile.deleteOnExit();

        RecordingCallback cb = new RecordingCallback(0);
        IOUtilities.transfer(conn, destFile, cb);

        byte[] readBack = Files.readAllBytes(destFile.toPath());
        assertArrayEquals(payload, readBack);
        assertEquals(payload.length, cb.getTotalBytes());
    }

    @Test
    public void testTransferURLConnectionToFile_WithGzip() throws Exception {
        byte[] original = "gzip‑payload".getBytes("UTF-8");
        byte[] gzipped = IOUtilities.compressBytes(original);
        InputStream src = new ByteArrayInputStream(gzipped);
        DummyURLConnection conn = new DummyURLConnection(src, null, "gzip");

        File destFile = File.createTempFile("ioUtilGzip", ".txt");
        destFile.deleteOnExit();

        IOUtilities.transfer(conn, destFile, null);
        byte[] readBack = Files.readAllBytes(destFile.toPath());
        assertArrayEquals(original, readBack);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, File, TransferCallback)               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToFile() throws Exception {
        byte[] data = "stream‑to‑file".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        File dest = File.createTempFile("ioUtilStreamFile", ".txt");
        dest.deleteOnExit();

        IOUtilities.transfer(in, dest, null);
        assertArrayEquals(data, Files.readAllBytes(dest.toPath()));
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream, TransferCallback)      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToOutputStream_WithCallback() throws Exception {
        byte[] data = new byte[10000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 256);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RecordingCallback cb = new RecordingCallback(0);
        IOUtilities.transfer(in, out, cb);

        assertArrayEquals(data, out.toByteArray());
        assertEquals(data.length, cb.getTotalBytes());
        assertTrue(cb.getCallCount() > 0);
    }

    @Test
    public void testTransferInputStreamToOutputStream_Cancelled() throws Exception {
        byte[] data = new byte[8000];
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Cancel after 3000 bytes
        RecordingCallback cb = new RecordingCallback(3000);
        IOUtilities.transfer(in, out, cb);

        assertTrue(out.size() < data.length);
        assertTrue(cb.isCancelled());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, byte[])                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToByteArray_FullRead() throws Exception {
        byte[] source = "full‑read".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(source);
        byte[] target = new byte[source.length];
        IOUtilities.transfer(in, target);
        assertArrayEquals(source, target);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArray_PrematureEOF() throws Exception {
        byte[] source = "short".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(source);
        byte[] target = new byte[source.length + 5]; // larger than source
        IOUtilities.transfer(in, target); // should throw IOException
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream) (no callback)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferInputStreamToOutputStream_NoCallback() throws Exception {
        byte[] data = "no‑callback".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(File, OutputStream)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferFileToOutputStream_FlushInvoked() throws Exception {
        byte[] data = "file‑to‑stream".getBytes("UTF-8");
        File src = File.createTempFile("ioUtilFileOut", ".txt");
        src.deleteOnExit();
        Files.write(src.toPath(), data);

        // Custom OutputStream that also implements Flushable and records flush calls
        class FlushRecordingOutputStream extends OutputStream implements Flushable {
            private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
            boolean flushed = false;

            @Override public void write(int b) { delegate.write(b); }
            @Override public void write(byte[] b, int off, int len) { delegate.write(b, off, len); }
            @Override public void flush() { flushed = true; }
            byte[] toByteArray() { return delegate.toByteArray(); }
        }

        FlushRecordingOutputStream out = new FlushRecordingOutputStream();
        IOUtilities.transfer(src, out);
        assertArrayEquals(data, out.toByteArray());
        assertTrue("flush should have been called", out.flushed);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for close methods (Closeable, XMLStreamReader, XMLStreamWriter)*/
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloseCloseable_SuppressesException() {
        // Should not propagate the IOException
        IOUtilities.close(new BadCloseable());
    }

    @Test
    public void testCloseXMLStreamReader_SuppressesException() {
        IOUtilities.close(new BadXMLStreamReader());
    }

    @Test
    public void testCloseXMLStreamWriter_SuppressesException() {
        IOUtilities.close(new BadXMLStreamWriter());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for flush methods (Flushable, XMLStreamWriter)                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFlushFlushable_SuppressesException() {
        IOUtilities.flush(new BadFlushable());
    }

    @Test
    public void testFlushXMLStreamWriter_SuppressesException() {
        // BadXMLStreamWriter does not throw on flush, but we verify that no exception propagates.
        IOUtilities.flush(new BadXMLStreamWriter());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for inputStreamToBytes                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInputStreamToBytes_Normal() {
        byte[] payload = "bytes‑to‑array".getBytes();
        InputStream in = new ByteArrayInputStream(payload);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(payload, result);
    }

    @Test
    public void testInputStreamToBytes_WithExceptionReturnsNull() {
        // InputStream that throws IOException on read
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("forced"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(URLConnection, byte[])                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransferURLConnectionWithBytes() throws Exception {
        byte[] payload = "conn‑bytes".getBytes("UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DummyURLConnection conn = new DummyURLConnection(null, baos, null);
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, baos.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for compressBytes / uncompressBytes (byte[] overloads)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressAndUncompressBytes_RoundTrip() {
        byte[] original = new byte[1024];
        for (int i = 0; i < original.length; i++) original[i] = (byte) (i % 256);
        byte[] compressed = IOUtilities.compressBytes(original);
        assertNotNull(compressed);
        assertTrue(compressed.length < original.length); // should be smaller

        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testUncompressBytes_NonGzippedReturnsOriginal() {
        byte[] data = "plain‑data".getBytes();
        assertSame(data, IOUtilities.uncompressBytes(data));
    }

    /* --------------------------------------------------------------------- */
    /* Tests for compressBytes(ByteArrayOutputStream, ByteArrayOutputStream)*/
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_ByteArrayOutputStreams() throws IOException {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("stream‑data".getBytes("UTF-8"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);
        byte[] gzipped = compressed.toByteArray();

        // Decompress using the public API and verify round‑trip
        byte[] roundTrip = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original.toByteArray(), roundTrip);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_FastByteArrayOutputStreams() throws IOException {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("fast‑stream".getBytes("UTF-8"));
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);
        byte[] gzipped = compressed.toByteArray();

        // Verify that uncompressing yields the original content
        byte[] roundTrip = IOUtilities.uncompressBytes(gzipped);
        assertArrayEquals(original.toByteArray(), roundTrip);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for compressBytes(byte[], offset, len)                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompressBytes_WithOffsetAndLength() {
        byte[] data = "0123456789ABCDEF".getBytes(); // 16 bytes
        // compress bytes 4..11 (8 bytes: "456789AB")
        byte[] compressed = IOUtilities.compressBytes(data, 4, 8);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals("456789AB".getBytes(), uncompressed);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for transfer(InputStream, OutputStream) (internal buffer)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransfer_InputStreamToOutputStream_UsesBuffer() throws Exception {
        // Large payload to guarantee multiple buffer reads
        byte[] data = new byte[200_000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 0xFF);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.transfer(in, out);
        assertArrayEquals(data, out.toByteArray());
    }
}
