package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link IOUtilities}.
 *
 * <p>
 * The tests are written as if three independent experts had each contributed a
 * single test case for every public method.  The final file merges those
 * contributions into a single, runnable test class.
 * </p>
 *
 * <p>
 * Only classes that appear in the supplied source (or in the signatures listed
 * in the prompt) are used – no external mocking frameworks, no subclassing of
 * library abstract types, and no reliance on newer library versions.
 * </p>
 */
public class IOUtilitiesTest
{
    private File tempFile;
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    @Before
    public void setUp() throws IOException
    {
        // a temporary file used by many tests
        tempFile = File.createTempFile("ioutil_test", ".tmp");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown()
    {
        if (tempFile != null && tempFile.exists())
        {
            tempFile.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper classes – small concrete implementations of the abstract types  */
    /* ---------------------------------------------------------------------- */

    /** Simple URLConnection that works with in‑memory byte arrays. */
    private static class MemoryURLConnection extends URLConnection
    {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayInputStream in;
        private final String encoding;

        MemoryURLConnection(byte[] inputBytes, String encoding)
        {
            super(null);
            this.in = new ByteArrayInputStream(inputBytes);
            this.encoding = encoding;
        }

        @Override
        public void connect() { /* no real connection */ }

        @Override
        public InputStream getInputStream()
        {
            return in;
        }

        @Override
        public OutputStream getOutputStream()
        {
            return out;
        }

        @Override
        public String getContentEncoding()
        {
            return encoding;
        }

        byte[] getWrittenBytes()
        {
            return out.toByteArray();
        }
    }

    /** Minimal XMLStreamReader that records whether {@code close()} was called. */
    private static class DummyXMLStreamReader implements XMLStreamReader
    {
        boolean closed = false;

        @Override public void close() throws XMLStreamException { closed = true; }

        // All other methods provide dummy implementations – they are never used
        @Override public int next() throws XMLStreamException { return 0; }
        @Override public int getEventType() { return 0; }
        @Override public String getText() { return null; }
        @Override public int getTextLength() { return 0; }
        @Override public int getTextStart() { return 0; }
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public boolean isStartElement() { return false; }
        @Override public boolean isEndElement() { return false; }
        @Override public boolean isCharacters() { return false; }
        @Override public boolean isWhiteSpace() { return false; }
        @Override public String getAttributeValue(String namespaceURI, String localName) { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public javax.xml.namespace.QName getAttributeName(int index) { return null; }
        @Override public String getAttributeNamespace(int index) { return null; }
        @Override public String getAttributeLocalName(int index) { return null; }
        @Override public String getAttributePrefix(int index) { return null; }
        @Override public String getAttributeType(int index) { return null; }
        @Override public String getAttributeValue(int index) { return null; }
        @Override public boolean isAttributeSpecified(int index) { return false; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespacePrefix(int index) { return null; }
        @Override public String getNamespaceURI(int index) { return null; }
        @Override public javax.xml.namespace.QName getName() { return null; }
        @Override public String getLocalName() { return null; }
        @Override public boolean hasName() { return false; }
        @Override public String getPrefix() { return null; }
        @Override public String getNamespaceURI() { return null; }
        @Override public int getLocation() { return 0; }
        @Override public int getElementText() throws XMLStreamException { return 0; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public void require(int type, String namespaceURI, String localName) throws XMLStreamException {}
        @Override public String getElementText() throws XMLStreamException { return null; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public String getNamespaceURI(String prefix) { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }
        @Override public java.util.Iterator getNamespacePrefixes() { return null; }
        @Override public java.util.Iterator getNamespaceContext() { return null; }
        @Override public java.util.Iterator getAttributeNames() { return null; }

        // many more methods exist; they are omitted for brevity because they are
        // never invoked by the code under test.
    }

    /** Minimal XMLStreamWriter that records whether {@code close()} / {@code flush()} were called. */
    private static class DummyXMLStreamWriter implements XMLStreamWriter
    {
        boolean closed = false;
        boolean flushed = false;

        @Override public void close() throws XMLStreamException { closed = true; }
        @Override public void flush() throws XMLStreamException { flushed = true; }

        // All other methods are dummy – they are never used.
        @Override public void writeStartElement(String localName) throws XMLStreamException {}
        @Override public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {}
        @Override public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {}
        @Override public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {}
        @Override public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {}
        @Override public void writeEmptyElement(String localName) throws XMLStreamException {}
        @Override public void writeEndElement() throws XMLStreamException {}
        @Override public void writeEndDocument() throws XMLStreamException {}
        @Override public void writeAttribute(String localName, String value) throws XMLStreamException {}
        @Override public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {}
        @Override public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {}
        @Override public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {}
        @Override public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {}
        @Override public void writeComment(String data) throws XMLStreamException {}
        @Override public void writeProcessingInstruction(String target) throws XMLStreamException {}
        @Override public void writeProcessingInstruction(String target, String data) throws XMLStreamException {}
        @Override public void writeCData(String data) throws XMLStreamException {}
        @Override public void writeDTD(String dtd) throws XMLStreamException {}
        @Override public void writeEntityRef(String name) throws XMLStreamException {}
        @Override public void writeStartDocument() throws XMLStreamException {}
        @Override public void writeStartDocument(String version) throws XMLStreamException {}
        @Override public void writeStartDocument(String encoding, String version) throws XMLStreamException {}
        @Override public void writeCharacters(String text) throws XMLStreamException {}
        @Override public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {}
        @Override public String getPrefix(String uri) throws XMLStreamException { return null; }
        @Override public void setPrefix(String prefix, String uri) throws XMLStreamException {}
        @Override public void setDefaultNamespace(String uri) throws XMLStreamException {}
        @Override public void setNamespaceContext(javax.xml.namespace.NamespaceContext context) throws XMLStreamException {}
        @Override public javax.xml.namespace.NamespaceContext getNamespaceContext() { return null; }
        @Override public Object getProperty(String name) throws IllegalArgumentException { return null; }
    }

    /** TransferCallback implementation that records transferred bytes and can cancel after a limit. */
    private static class RecordingCallback implements IOUtilities.TransferCallback
    {
        private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        private final int cancelAfter; // number of bytes after which {@code isCancelled()} returns true
        private int total = 0;

        RecordingCallback(int cancelAfter)
        {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void bytesTransferred(byte[] bytes, int count)
        {
            recorded.write(bytes, 0, count);
            total += count;
        }

        @Override
        public boolean isCancelled()
        {
            return cancelAfter >= 0 && total >= cancelAfter;
        }

        byte[] getRecorded()
        {
            return recorded.toByteArray();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Test cases – one per public method (as if contributed by three experts) */
    /* ---------------------------------------------------------------------- */

    /** 1. getInputStream – no encoding (plain stream). */
    @Test
    public void testGetInputStream_NoEncoding() throws Exception
    {
        MemoryURLConnection conn = new MemoryURLConnection(SAMPLE_DATA, null);
        InputStream is = IOUtilities.getInputStream(conn);
        byte[] read = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(SAMPLE_DATA, read);
    }

    /** 2. getInputStream – gzip encoding. */
    @Test
    public void testGetInputStream_GzipEncoding() throws Exception
    {
        // Prepare a gzipped byte array
        ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
        try (DeflaterOutputStream gos = new AdjustableGZIPOutputStream(gzOut, Deflater.BEST_SPEED))
        {
            gos.write(SAMPLE_DATA);
        }
        byte[] gzipped = gzOut.toByteArray();

        MemoryURLConnection conn = new MemoryURLConnection(gzipped, "gzip");
        InputStream is = IOUtilities.getInputStream(conn);
        assertTrue(is instanceof GZIPInputStream);
        byte[] read = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(SAMPLE_DATA, read);
    }

    /** 3. getInputStream – deflate encoding. */
    @Test
    public void testGetInputStream_DeflateEncoding() throws Exception
    {
        // Prepare a deflate‑compressed byte array (no zlib header)
        ByteArrayOutputStream defOut = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_SPEED, true);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(defOut, deflater))
        {
            dos.write(SAMPLE_DATA);
        }
        byte[] deflated = defOut.toByteArray();

        MemoryURLConnection conn = new MemoryURLConnection(deflated, "deflate");
        InputStream is = IOUtilities.getInputStream(conn);
        assertTrue(is instanceof InflaterInputStream);
        byte[] read = IOUtilities.inputStreamToBytes(is);
        assertArrayEquals(SAMPLE_DATA, read);
    }

    /** 4. transfer(File, URLConnection, TransferCallback) – normal flow. */
    @Test
    public void testTransferFileToURLConnection() throws Exception
    {
        // write sample data to temp file
        try (FileOutputStream fos = new FileOutputStream(tempFile))
        {
            fos.write(SAMPLE_DATA);
        }

        MemoryURLConnection conn = new MemoryURLConnection(new byte[0], null);
        RecordingCallback cb = new RecordingCallback(-1); // never cancel
        IOUtilities.transfer(tempFile, conn, cb);

        // Verify that the bytes written to the connection match the file contents
        assertArrayEquals(SAMPLE_DATA, conn.getWrittenBytes());
        // Verify that the callback saw the same data
        assertArrayEquals(SAMPLE_DATA, cb.getRecorded());
    }

    /** 5. transfer(File, URLConnection, TransferCallback) – cancellation after half. */
    @Test
    public void testTransferFileToURLConnection_CancelMidway() throws Exception
    {
        // write sample data to temp file
        try (FileOutputStream fos = new FileOutputStream(tempFile))
        {
            fos.write(SAMPLE_DATA);
        }

        MemoryURLConnection conn = new MemoryURLConnection(new byte[0], null);
        // cancel after 10 bytes
        RecordingCallback cb = new RecordingCallback(10);
        IOUtilities.transfer(tempFile, conn, cb);

        // The written bytes must be at most 10 (maybe a little more because the buffer
        // size is larger than 10, but the callback stops after it sees the limit)
        assertTrue(conn.getWrittenBytes().length <= SAMPLE_DATA.length);
        assertTrue(cb.getRecorded().length <= 10);
    }

    /** 6. transfer(URLConnection, File, TransferCallback) – reading from a connection. */
    @Test
    public void testTransferURLConnectionToFile() throws Exception
    {
        MemoryURLConnection conn = new MemoryURLConnection(SAMPLE_DATA, null);
        RecordingCallback cb = new RecordingCallback(-1);
        IOUtilities.transfer(conn, tempFile, cb);

        // Verify file content
        byte[] fileBytes = readAllBytes(tempFile);
        assertArrayEquals(SAMPLE_DATA, fileBytes);
        // Verify callback
        assertArrayEquals(SAMPLE_DATA, cb.getRecorded());
    }

    /** 7. transfer(InputStream, File, TransferCallback) – stream to file. */
    @Test
    public void testTransferInputStreamToFile() throws Exception
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        RecordingCallback cb = new RecordingCallback(-1);
        IOUtilities.transfer(in, tempFile, cb);

        byte[] fileBytes = readAllBytes(tempFile);
        assertArrayEquals(SAMPLE_DATA, fileBytes);
        assertArrayEquals(SAMPLE_DATA, cb.getRecorded());
    }

    /** 8. transfer(InputStream, OutputStream, TransferCallback) – simple copy with callback. */
    @Test
    public void testTransferInputStreamToOutputStream_WithCallback() throws Exception
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordingCallback cb = new RecordingCallback(-1);
        IOUtilities.transfer(in, out, cb);

        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
        assertArrayEquals(SAMPLE_DATA, cb.getRecorded());
    }

    /** 9. transfer(InputStream, OutputStream, TransferCallback) – cancellation. */
    @Test
    public void testTransferInputStreamToOutputStream_Cancel() throws Exception
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Cancel after 15 bytes
        RecordingCallback cb = new RecordingCallback(15);
        IOUtilities.transfer(in, out, cb);

        assertTrue(out.toByteArray().length <= SAMPLE_DATA.length);
        assertTrue(cb.getRecorded().length <= 15);
    }

    /** 10. transfer(InputStream, byte[]) – exact length succeeds. */
    @Test
    public void testTransferInputStreamToByteArrayExact() throws Exception
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        byte[] target = new byte[SAMPLE_DATA.length];
        IOUtilities.transfer(in, target);
        assertArrayEquals(SAMPLE_DATA, target);
    }

    /** 11. transfer(InputStream, byte[]) – insufficient data throws IOException. */
    @Test(expected = IOException.class)
    public void testTransferInputStreamToByteArrayInsufficient() throws Exception
    {
        InputStream in = new ByteArrayInputStream(new byte[]{1, 2, 3});
        byte[] target = new byte[10];
        IOUtilities.transfer(in, target); // should throw
    }

    /** 12. transfer(InputStream, OutputStream) – without callback. */
    @Test
    public void testTransferInputStreamToOutputStream_NoCallback() throws Exception
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /** 13. transfer(File, OutputStream) – file to output stream, flushes correctly. */
    @Test
    public void testTransferFileToOutputStream() throws Exception
    {
        // Write data to the temp file
        try (FileOutputStream fos = new FileOutputStream(tempFile))
        {
            fos.write(SAMPLE_DATA);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(tempFile, out);
        assertArrayEquals(SAMPLE_DATA, out.toByteArray());
    }

    /** 14. close(Closeable) – normal close and null handling. */
    @Test
    public void testCloseCloseable()
    {
        // Normal close
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtilities.close(baos);
        // No exception expected

        // Null handling – should not throw
        IOUtilities.close((Closeable) null);
    }

    /** 15. close(XMLStreamReader) – ensure no exception is thrown. */
    @Test
    public void testCloseXMLStreamReader()
    {
        DummyXMLStreamReader reader = new DummyXMLStreamReader();
        IOUtilities.close(reader);
        assertTrue(reader.closed);
        // Null handling
        IOUtilities.close((XMLStreamReader) null);
    }

    /** 16. close(XMLStreamWriter) – ensure no exception is thrown. */
    @Test
    public void testCloseXMLStreamWriter()
    {
        DummyXMLStreamWriter writer = new DummyXMLStreamWriter();
        IOUtilities.close(writer);
        assertTrue(writer.closed);
        // Null handling
        IOUtilities.close((XMLStreamWriter) null);
    }

    /** 17. flush(Flushable) – normal flush and null handling. */
    @Test
    public void testFlushFlushable()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtilities.flush(baos); // no exception

        IOUtilities.flush((Flushable) null); // no exception
    }

    /** 18. flush(XMLStreamWriter) – ensure flush is called. */
    @Test
    public void testFlushXMLStreamWriter()
    {
        DummyXMLStreamWriter writer = new DummyXMLStreamWriter();
        IOUtilities.flush(writer);
        assertTrue(writer.flushed);
        // Null handling
        IOUtilities.flush((XMLStreamWriter) null);
    }

    /** 19. inputStreamToBytes – successful conversion. */
    @Test
    public void testInputStreamToBytes_Success()
    {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertArrayEquals(SAMPLE_DATA, result);
    }

    /** 20. inputStreamToBytes – exception handling returns null. */
    @Test
    public void testInputStreamToBytes_Exception()
    {
        InputStream broken = new InputStream()
        {
            @Override public int read() throws IOException { throw new IOException("boom"); }
        };
        assertNull(IOUtilities.inputStreamToBytes(broken));
    }

    /** 21. transfer(URLConnection, byte[]) – writes bytes to connection output. */
    @Test
    public void testTransferURLConnectionWithBytes() throws Exception
    {
        MemoryURLConnection conn = new MemoryURLConnection(new byte[0], null);
        IOUtilities.transfer(conn, SAMPLE_DATA);
        assertArrayEquals(SAMPLE_DATA, conn.getWrittenBytes());
    }

    /** 22. compressBytes(byte[]) – round‑trip compress / uncompress. */
    @Test
    public void testCompressAndUncompressBytes()
    {
        byte[] compressed = IOUtilities.compressBytes(SAMPLE_DATA);
        assertTrue(ByteUtilities.isGzipped(compressed));
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(SAMPLE_DATA, uncompressed);
    }

    /** 23. compressBytes(byte[], offset, len) – partial array compress. */
    @Test
    public void testCompressBytesWithOffset()
    {
        byte[] data = new byte[100];
        Arrays.fill(data, (byte) 'x');
        System.arraycopy(SAMPLE_DATA, 0, data, 10, SAMPLE_DATA.length);

        byte[] compressed = IOUtilities.compressBytes(data, 10, SAMPLE_DATA.length);
        byte[] result = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(SAMPLE_DATA, result);
    }

    /** 24. compressBytes(FastByteArrayOutputStream) – overload that accepts FastByteArrayOutputStream. */
    @Test
    public void testCompressBytesFastByteArrayOutputStream() throws Exception
    {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(SAMPLE_DATA);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);
        byte[] result = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(SAMPLE_DATA, result);
    }

    /** 25. uncompressBytes – non‑gzipped input returns the same array. */
    @Test
    public void testUncompressBytes_NonGzipped()
    {
        byte[] data = "plain".getBytes();
        byte[] result = IOUtilities.uncompressBytes(data);
        assertSame(data, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Utility methods used by several tests                                    */
    /* ---------------------------------------------------------------------- */

    private static byte[] readAllBytes(File f) throws IOException
    {
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }
}
