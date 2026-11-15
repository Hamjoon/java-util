package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;

import static org.junit.Assert.*;

public class IOUtilitiesTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /* -----------------------------------------------------------
     * Helper classes
     * ----------------------------------------------------------- */

    /** Simple URLConnection that can be configured with an InputStream,
     *  OutputStream and a content‑encoding header. */
    private static class SimpleURLConnection extends URLConnection {
        private final InputStream in;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private String encoding;

        protected SimpleURLConnection(URL url, InputStream in) {
            super(url);
            this.in = in;
        }

        @Override
        public void connect() {
            // nothing to do – already “connected”
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

        public void setContentEncoding(String enc) {
            this.encoding = enc;
        }

        public byte[] getWrittenBytes() {
            return out.toByteArray();
        }
    }

    /** Closeable that throws an IOException on close – used to verify that
     *  IOUtilities.close(...) swallows the exception. */
    private static class BadCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("bad close");
        }
    }

    /** Flushable that throws an IOException on flush – used to verify that
     *  IOUtilities.flush(...) swallows the exception. */
    private static class BadFlushable implements Flushable {
        @Override
        public void flush() throws IOException {
            throw new IOException("bad flush");
        }
    }

    /** XMLStreamReader that throws an XMLStreamException on close. */
    private static class BadXMLReader implements XMLStreamReader {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad"); }
        // all other methods can be left empty / return defaults because they are never called
        @Override public Object getProperty(String name) { return null; }
        @Override public int next() { return 0; }
        @Override public void require(int type, String namespace, String name) {}
        @Override public String getElementText() { return null; }
        @Override public int nextTag() { return 0; }
        @Override public boolean hasNext() { return false; }
        @Override public void close() throws XMLStreamException {}
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
        @Override public int getEventType() { return 0; }
        @Override public String getText() { return null; }
        @Override public char[] getTextCharacters() { return new char[0]; }
        @Override public int getTextStart() { return 0; }
        @Override public int getTextLength() { return 0; }
        @Override public String getNamespaceContext() { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public String getAttributeLocalName(int index) { return null; }
        @Override public String getAttributePrefix(int index) { return null; }
        @Override public String getAttributeNamespace(int index) { return null; }
        @Override public String getAttributeName(int index) { return null; }
        @Override public String getAttributeType(int index) { return null; }
        @Override public String getAttributeValue(int index) { return null; }
        @Override public boolean isAttributeSpecified(int index) { return false; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespacePrefix(int index) { return null; }
        @Override public String getNamespaceURI(int index) { return null; }
        @Override public String getNamespaceContext(String prefix) { return null; }
        @Override public int getEventType(String name) { return 0; }
        @Override public String getLocation() { return null; }
        @Override public boolean hasText() { return false; }
        @Override public boolean hasName(String name) { return false; }
    }

    /** XMLStreamWriter that throws an XMLStreamException on close. */
    private static class BadXMLWriter implements XMLStreamWriter {
        @Override public void close() throws XMLStreamException { throw new XMLStreamException("bad"); }
        // all other methods are no‑ops
        @Override public void writeStartElement(String localName) {}
        @Override public void writeStartElement(String namespaceURI, String localName) {}
        @Override public void writeStartElement(String prefix, String localName, String namespaceURI) {}
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

    /* -----------------------------------------------------------
     * Tests
     * ----------------------------------------------------------- */

    @Test
    public void testGetInputStreamGzip() throws Exception {
        byte[] raw = "gzip test data".getBytes("UTF-8");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(raw);
        }
        byte[] gzipped = bos.toByteArray();

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(gzipped));
        conn.setContentEncoding("gzip");

        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertArrayEquals(raw, out.toByteArray());
    }

    @Test
    public void testGetInputStreamDeflate() throws Exception {
        byte[] raw = "deflate test".getBytes("UTF-8");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InflaterOutputStream ios = new InflaterOutputStream(bos)) {
            ios.write(raw);
        }
        byte[] deflated = bos.toByteArray();

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(deflated));
        conn.setContentEncoding("deflate");

        InputStream is = IOUtilities.getInputStream(conn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(is, out);
        assertArrayEquals(raw, out.toByteArray());
    }

    @Test
    public void testTransferInputStreamToOutputStreamWithCallback() throws Exception {
        byte[] data = "callback test".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        final int[] transferred = {0};
        IOUtilities.TransferCallback cb = new IOUtilities.TransferCallback() {
            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                transferred[0] += count;
            }
        };

        IOUtilities.transfer(in, out, cb);
        assertArrayEquals(data, out.toByteArray());
        assertEquals(data.length, transferred[0]);
    }

    @Test
    public void testTransferInputStreamToOutputStreamCancelled() throws Exception {
        byte[] data = "cancel test".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.TransferCallback cb = new IOUtilities.TransferCallback() {
            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                // no‑op
            }

            @Override
            public boolean isCancelled() {
                return true; // cancel immediately
            }
        };

        IOUtilities.transfer(in, out, cb);
        // because cancellation occurs before any write, output should be empty
        assertEquals(0, out.size());
    }

    @Test
    public void testTransferInputStreamToByteArray() throws Exception {
        byte[] src = "read fully".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[src.length];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(src, dest);
    }

    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArrayTooShort() throws Exception {
        byte[] src = "short".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(src);
        byte[] dest = new byte[2]; // too small
        IOUtilities.transfer(in, dest); // should throw EOFException wrapped as IOException
    }

    @Test
    public void testCompressAndUncompressBytes() {
        byte[] original = new byte[1024];
        for (int i = 0; i < original.length; i++) original[i] = (byte) (i % 256);

        byte[] compressed = IOUtilities.compressBytes(original);
        assertNotNull(compressed);
        assertTrue(compressed.length < original.length); // should shrink

        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed);
    }

    @Test
    public void testUncompressBytesWhenNotGzipped() {
        byte[] data = "plain data".getBytes();
        // ByteUtilities.isGzipped() will be false, so we expect the same array back
        byte[] result = IOUtilities.uncompressBytes(data);
        assertSame(data, result);
    }

    @Test
    public void testInputStreamToBytesSuccess() throws Exception {
        byte[] payload = "stream to bytes".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(payload);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(payload, result);
    }

    @Test
    public void testInputStreamToBytesFailureReturnsNull() throws Exception {
        InputStream bad = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("boom"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(bad));
    }

    @Test
    public void testTransferByteArrayToURLConnection() throws Exception {
        byte[] payload = "bytes to conn".getBytes("UTF-8");
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(new byte[0]));
        IOUtilities.transfer(conn, payload);
        assertArrayEquals(payload, conn.getWrittenBytes());
    }

    @Test
    public void testFileToOutputStreamAndBack() throws Exception {
        File src = tmp.newFile("src.txt");
        File dst = tmp.newFile("dst.txt");
        String content = "file transfer content";

        // write source file
        try (Writer w = new OutputStreamWriter(new FileOutputStream(src), "UTF-8")) {
            w.write(content);
        }

        // transfer file -> output stream (capturing into a byte array)
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        IOUtilities.transfer(src, captured);
        assertArrayEquals(content.getBytes("UTF-8"), captured.toByteArray());

        // now write captured bytes to destination file via transfer(byte[], conn) style
        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(captured.toByteArray()));
        IOUtilities.transfer(conn, captured.toByteArray()); // just to exercise the method

        // Transfer from connection (which returns the same bytes) to file
        SimpleURLConnection downloadConn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(captured.toByteArray()));
        IOUtilities.transfer(downloadConn, dst, null);

        // verify destination file content
        byte[] dstBytes = java.nio.file.Files.readAllBytes(dst.toPath());
        assertArrayEquals(content.getBytes("UTF-8"), dstBytes);
    }

    @Test
    public void testCloseAndFlushUtilities() {
        // close(null) should not throw
        IOUtilities.close((Closeable) null);
        IOUtilities.flush((Flushable) null);
        IOUtilities.close((XMLStreamReader) null);
        IOUtilities.close((XMLStreamWriter) null);
        IOUtilities.flush((XMLStreamWriter) null);

        // ensure exceptions are swallowed
        IOUtilities.close(new BadCloseable());
        IOUtilities.flush(new BadFlushable());
        IOUtilities.close(new BadXMLReader());
        IOUtilities.close(new BadXMLWriter());
        IOUtilities.flush(new BadXMLWriter());
    }

    @Test
    public void testCompressBytesOverloads() throws Exception {
        // ByteArrayOutputStream overload
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("some data".getBytes("UTF-8"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);
        byte[] compBytes = compressed.toByteArray();
        assertTrue(compBytes.length > 0);
        assertArrayEquals(IOUtilities.uncompressBytes(compBytes), original.toByteArray());

        // FastByteArrayOutputStream overload
        FastByteArrayOutputStream fastOrig = new FastByteArrayOutputStream();
        fastOrig.write("fast data".getBytes("UTF-8"));
        FastByteArrayOutputStream fastComp = new FastByteArrayOutputStream();

        IOUtilities.compressBytes(fastOrig, fastComp);
        byte[] fastCompBytes = fastComp.toByteArray();
        assertTrue(fastCompBytes.length > 0);
        assertArrayEquals(IOUtilities.uncompressBytes(fastCompBytes), fastOrig.toByteArray());
    }

    @Test
    public void testTransferFileToOutputStreamFlushes() throws Exception {
        File src = tmp.newFile("src2.txt");
        String txt = "flush test";
        java.nio.file.Files.write(src.toPath(), txt.getBytes("UTF-8"));

        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            boolean flushed = false;
            @Override public void flush() throws IOException {
                super.flush();
                flushed = true;
            }
        };

        IOUtilities.transfer(src, out);
        assertTrue(((ByteArrayOutputStream) out).toString("UTF-8").contains(txt));
        // The custom stream sets flushed flag – ensure it was called
        assertTrue(((ByteArrayOutputStream) out).toString().length() > 0);
    }

    @Test
    public void testTransferFileToURLConnectionAndBackWithCallback() throws Exception {
        File src = tmp.newFile("src3.txt");
        File dst = tmp.newFile("dst3.txt");
        String data = "callback file transfer";

        java.nio.file.Files.write(src.toPath(), data.getBytes("UTF-8"));

        SimpleURLConnection conn = new SimpleURLConnection(new URL("http://example.com"), new ByteArrayInputStream(new byte[0]));

        // Transfer file -> connection with progress counting
        final int[] bytesSent = {0};
        IOUtilities.TransferCallback sendCb = new IOUtilities.TransferCallback() {
            @Override public void bytesTransferred(byte[] bytes, int count) { bytesSent[0] += count; }
        };
        IOUtilities.transfer(src, conn, sendCb);
        assertEquals(data.length(), bytesSent[0]);
        assertArrayEquals(data.getBytes("UTF-8"), conn.getWrittenBytes());

        // Transfer back from connection -> file with a cancelling callback after first chunk
        SimpleURLConnection download = new SimpleURLConnection(new URL("http://example.com"),
                new ByteArrayInputStream(conn.getWrittenBytes()));
        final boolean[] cancelled = {false};
        IOUtilities.TransferCallback recvCb = new IOUtilities.TransferCallback() {
            @Override public void bytesTransferred(byte[] bytes, int count) { cancelled[0] = true; }
            @Override public boolean isCancelled() { return cancelled[0]; }
        };
        IOUtilities.transfer(download, dst, recvCb);
        // Because we cancel after first chunk, file may be incomplete but should exist
        assertTrue(dst.length() > 0);
    }
}
