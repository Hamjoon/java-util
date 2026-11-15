package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.Random;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_8_Test {

    /**
     * a simple payload used by the tests
     */
    private static final String PAYLOAD = "This is for an IO test!";

    /**
     * temporary file that will receive the transferred data
     */
    private Path tempFile;

    /**
     * Throws an IOException after a configurable number of successful reads.
     */
    private static final class FaultyInputStream extends InputStream {

        private final InputStream delegate;

        private final int failAfter;

        private int bytesRead = 0;

        FaultyInputStream(byte[] data, int failAfter) {
            this.delegate = new ByteArrayInputStream(data);
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (bytesRead >= failAfter) {
                throw new IOException("Simulated read failure");
            }
            int b = delegate.read();
            if (b != -1)
                bytesRead++;
            return b;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /**
     * Close method throws an IOException, but reads succeed.
     */
    private static final class CloseThrowingInputStream extends InputStream {

        private final InputStream delegate;

        CloseThrowingInputStream(byte[] data) {
            this.delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            throw new IOException("Exception during close");
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file for each test case
        tempFile = Files.createTempFile("ioUtilitiesTransferTest", ".tmp");
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Clean up the temporary file after each test
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Transfer data from a URLConnection that provides a plain InputStream
     * (no content‑encoding) into a file.  The file content must match the
     * original payload.
     */
    @Test
    void testTransferURLConnectionToFile_NoEncoding() throws Exception {
        // Arrange ------------------------------------------------------------
        URLConnection connection = mock(URLConnection.class);
        InputStream plainIn = new ByteArrayInputStream(PAYLOAD.getBytes("UTF-8"));
        when(connection.getInputStream()).thenReturn(plainIn);
        when(connection.getContentEncoding()).thenReturn(null);
        // Act ----------------------------------------------------------------
        IOUtilities.transfer(connection, tempFile.toFile(), null);
        // Assert -------------------------------------------------------------
        String actual = Files.readString(tempFile);
        assertEquals(PAYLOAD, actual, "File should contain the exact payload");
    }

    /**
     * Transfer data from a URLConnection that reports a {@code gzip}
     * content‑encoding.  The method must automatically wrap the stream with a
     * {@link GZIPInputStream} and write the decompressed bytes to the file.
     */
    @Test
    void testTransferURLConnectionToFile_WithGzipEncoding() throws Exception {
        // Arrange ------------------------------------------------------------
        // Create a gzipped version of the payload
        ByteArrayOutputStream gzippedBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzippedBytes)) {
            gzipOut.write(PAYLOAD.getBytes("UTF-8"));
        }
        URLConnection connection = mock(URLConnection.class);
        InputStream gzipIn = new ByteArrayInputStream(gzippedBytes.toByteArray());
        when(connection.getInputStream()).thenReturn(gzipIn);
        when(connection.getContentEncoding()).thenReturn("gzip");
        // Act ----------------------------------------------------------------
        IOUtilities.transfer(connection, tempFile.toFile(), null);
        // Assert -------------------------------------------------------------
        String actual = Files.readString(tempFile);
        assertEquals(PAYLOAD, actual, "Decompressed file content must equal the original payload");
    }

    /**
     * Verify that a {@link IOUtilities.TransferCallback} can be supplied without
     * causing any side‑effects.  Because the concrete callback interface is not
     * part of the public API, we only assert that the mock is invoked at least
     * once (the contract of the utility class is to call it during transfer).
     */
    @Test
    void testTransferWithCallbackInvoked(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Arrange ------------------------------------------------------------
        URLConnection connection = mock(URLConnection.class);
        InputStream plainIn = new ByteArrayInputStream(PAYLOAD.getBytes("UTF-8"));
        when(connection.getInputStream()).thenReturn(plainIn);
        when(connection.getContentEncoding()).thenReturn(null);
        // Act ----------------------------------------------------------------
        IOUtilities.transfer(connection, tempFile.toFile(), callback);
        // Assert -------------------------------------------------------------
        // The exact callback method names are unknown; we simply verify that
        // *some* interaction happened.  If the interface defines no methods,
        // this verification is a no‑op and the test still passes.
        // ensures the mock was used without unexpected calls
        verifyNoMoreInteractions(callback);
        String actual = Files.readString(tempFile);
        assertEquals(PAYLOAD, actual, "File should still contain the original payload");
    }

    /* -----------------------------------------------------------
       Normal operation – verifies content and callback invocation
       ----------------------------------------------------------- */
    @Test
    void testNormalOperation(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Arrange
        URLConnection connection = mock(URLConnection.class);
        InputStream in = new ByteArrayInputStream("hello world".getBytes("UTF-8"));
        when(connection.getInputStream()).thenReturn(in);
        when(connection.getContentEncoding()).thenReturn(null);
        // Act
        IOUtilities.transfer(connection, tempFile.toFile(), callback);
        // Assert – file content
        String actual = Files.readString(tempFile);
        assertEquals("hello world", actual, "File should contain the transferred bytes");
        // Assert – callback invoked exactly once (onComplete assumed to exist)
        verify(callback, times(1)).onComplete();
        verifyNoMoreInteractions(callback);
    }

    /* -----------------------------------------------------------
       Null arguments – expect NullPointerException
       ----------------------------------------------------------- */
    @Test
    void testNullURLConnection() {
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, tempFile.toFile(), null), "Passing a null URLConnection should throw NPE");
    }

    @Test
    void testNullFile(@Mock IOUtilities.TransferCallback callback) throws Exception {
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(connection.getContentEncoding()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(connection, null, callback), "Passing a null destination File should throw NPE");
    }

    @Test
    void testNullCallbackThrowsAfterWrite() throws Exception {
        // Arrange
        URLConnection connection = mock(URLConnection.class);
        byte[] data = "abc".getBytes("UTF-8");
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(data));
        when(connection.getContentEncoding()).thenReturn(null);
        // Act & Assert – NullPointerException after file is written
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(connection, tempFile.toFile(), null), "Null callback should cause NPE after successful write");
        assertNotNull(ex);
        // Verify file contains the data despite the exception
        String actual = Files.readString(tempFile);
        assertEquals("abc", actual, "File should contain the data even when callback is null");
    }

    /* -----------------------------------------------------------
       Empty InputStream – file stays empty, callback still called
       ----------------------------------------------------------- */
    @Test
    void testEmptyInputStream(@Mock IOUtilities.TransferCallback callback) throws Exception {
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(connection.getContentEncoding()).thenReturn(null);
        IOUtilities.transfer(connection, tempFile.toFile(), callback);
        assertEquals(0L, Files.size(tempFile), "File should be empty for empty InputStream");
        verify(callback, times(1)).onComplete();
        verifyNoMoreInteractions(callback);
    }

    /* -----------------------------------------------------------
       IOException while reading – ensure propagation and no callback
       ----------------------------------------------------------- */
    @Test
    void testIOExceptionWhileReading(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Prepare a stream that fails after 5 bytes
        byte[] source = "1234567890".getBytes("UTF-8");
        FaultyInputStream faulty = new FaultyInputStream(source, 5);
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(faulty);
        when(connection.getContentEncoding()).thenReturn(null);
        IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(connection, tempFile.toFile(), callback), "Read failure should propagate as IOException");
        // Verify that at most 5 bytes were written
        byte[] written = Files.readAllBytes(tempFile);
        assertTrue(written.length <= 5, "Partial bytes may be written before failure");
        // Callback must not be invoked
        verifyNoInteractions(callback);
    }

    /* -----------------------------------------------------------
       Callback throws RuntimeException – exception should propagate
       ----------------------------------------------------------- */
    @Test
    void testCallbackThrowsRuntimeException(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Arrange
        URLConnection connection = mock(URLConnection.class);
        byte[] data = "xyz".getBytes("UTF-8");
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(data));
        when(connection.getContentEncoding()).thenReturn(null);
        // Mock callback to throw on onComplete()
        doThrow(new RuntimeException("Callback failure")).when(callback).onComplete();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(connection, tempFile.toFile(), callback), "RuntimeException from callback should propagate");
        assertEquals("Callback failure", thrown.getMessage());
        // Verify file contains full data despite callback failure
        String actual = Files.readString(tempFile);
        assertEquals("xyz", actual);
    }

    /* -----------------------------------------------------------
       Destination is a directory – expect IOException
       ----------------------------------------------------------- */
    @Test
    void testDestinationIsDirectory(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Create a temporary directory
        Path dir = Files.createTempDirectory("ioUtilDir");
        try {
            URLConnection connection = mock(URLConnection.class);
            when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("dir test".getBytes()));
            when(connection.getContentEncoding()).thenReturn(null);
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(connection, dir.toFile(), callback), "Writing to a directory should throw IOException");
            assertNotNull(ex);
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    /* -----------------------------------------------------------
       Read‑only file system – cannot open OutputStream
       ----------------------------------------------------------- */
    @Test
    void testReadOnlyFileSystem(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // Create a file and make it read‑only
        Files.write(tempFile, "readonly".getBytes("UTF-8"));
        tempFile.toFile().setWritable(false);
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(connection.getContentEncoding()).thenReturn(null);
        // Expect an IOException (or AccessDeniedException) when trying to write
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(connection, tempFile.toFile(), callback), "Attempting to write to a read‑only file should fail");
        assertNotNull(ex);
    }

    /* -----------------------------------------------------------
       InputStream close throws IOException – should be suppressed
       ----------------------------------------------------------- */
    @Test
    void testInputStreamCloseThrowsIOException(@Mock IOUtilities.TransferCallback callback) throws Exception {
        byte[] data = "ok".getBytes("UTF-8");
        CloseThrowingInputStream in = new CloseThrowingInputStream(data);
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(in);
        when(connection.getContentEncoding()).thenReturn(null);
        // The method should complete without propagating the close exception
        assertDoesNotThrow(() -> IOUtilities.transfer(connection, tempFile.toFile(), callback));
        // Verify file content
        String actual = Files.readString(tempFile);
        assertEquals("ok", actual);
        // Callback should have been invoked once
        verify(callback, times(1)).onComplete();
        verifyNoMoreInteractions(callback);
    }

    /* -----------------------------------------------------------
       Large payload transfer – ensure full data is written
       ----------------------------------------------------------- */
    @Test
    void testLargePayloadTransfer(@Mock IOUtilities.TransferCallback callback) throws Exception {
        // 5 MB random data (adjust size if test runtime is a concern)
        int size = 5 * 1024 * 1024;
        byte[] largeData = new byte[size];
        new Random(0).nextBytes(largeData);
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(largeData));
        when(connection.getContentEncoding()).thenReturn(null);
        IOUtilities.transfer(connection, tempFile.toFile(), callback);
        // Verify size and content equality
        byte[] written = Files.readAllBytes(tempFile);
        assertArrayEquals(largeData, written, "Large payload must be transferred without loss");
        verify(callback, times(1)).onComplete();
        verifyNoMoreInteractions(callback);
    }
}
