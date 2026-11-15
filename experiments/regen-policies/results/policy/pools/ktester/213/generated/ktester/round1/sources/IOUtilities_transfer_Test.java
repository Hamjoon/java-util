package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URLConnection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_Test {

    /**
     * Content used for the temporary file in the happy‑path test.
     */
    private static final String EXPECTED_CONTENT = "This is for an IO test!";

    /**
     * Temporary file that will be created before each test and deleted afterwards.
     */
    private File tempFile;

    /**
     * Mocked URLConnection that supplies a controllable OutputStream.
     */
    private URLConnection mockConnection;

    /**
     * The OutputStream returned by the mocked connection – we can later inspect its bytes.
     */
    private ByteArrayOutputStream connectionOut;

    class RecordingCallback implements IOUtilities.TransferCallback {

        long bytesSeen = 0L;

        @Override
        public void transferred(long bytes) {
            bytesSeen += bytes;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class ZeroCallback implements IOUtilities.TransferCallback {

        long bytesSeen = 0L;

        int calls = 0;

        @Override
        public void transferred(long bytes) {
            bytesSeen += bytes;
            calls++;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class CountingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        int calls = 0;

        @Override
        public void transferred(long bytes) {
            total += bytes;
            calls++;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class BadCallback implements IOUtilities.TransferCallback {

        @Override
        public void transferred(long bytes) {
            throw new RuntimeException("callback failure");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class FaultyCloseOutputStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            delegate.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close failed");
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }

    class ZeroCallback implements IOUtilities.TransferCallback {

        long bytesSeen = 0L;

        int calls = 0;

        @Override
        public void transferred(long bytes) {
            bytesSeen += bytes;
            calls++;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class CountingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        int calls = 0;

        @Override
        public void transferred(long bytes) {
            total += bytes;
            calls++;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class RecordingCallback implements IOUtilities.TransferCallback {

        long bytesSeen = 0L;

        @Override
        public void transferred(long bytes) {
            bytesSeen += bytes;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class BadCallback implements IOUtilities.TransferCallback {

        @Override
        public void transferred(long bytes) {
            throw new RuntimeException("callback failure");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    class FaultyCloseOutputStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            delegate.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close failed");
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file and write the known test data into it.
        tempFile = File.createTempFile("IOUtilities_transfer_Test", ".txt");
        Files.write(tempFile.toPath(), EXPECTED_CONTENT.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        // Prepare a mock URLConnection that returns a fresh ByteArrayOutputStream.
        connectionOut = new ByteArrayOutputStream();
        mockConnection = mock(URLConnection.class);
        when(mockConnection.getOutputStream()).thenReturn(connectionOut);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up the temporary file – ignore any failure to delete.
        if (tempFile != null && tempFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    // ----------------------------------------------------------------------
    private static Stream<Arguments> nullCheckProvider() {
        return Stream.of(Arguments.of(null, mock(URLConnection.class), "File cannot be null"), Arguments.of(File.createTempFile("dummy", ".tmp"), null, "URLConnection cannot be null"));
    }

    @Test
    void testTransferFileToURLConnection_success() throws Exception {
        // GIVEN – a real file (tempFile) and a mocked connection (mockConnection)
        // WHEN
        IOUtilities.transfer(tempFile, mockConnection, null);
        // THEN – the bytes written to the connection's OutputStream must match the file's contents.
        assertEquals(EXPECTED_CONTENT, connectionOut.toString("UTF-8"));
    }

    @Test
    void testTransferFile_nullFile_throwsException() {
        // GIVEN a null file reference
        File nullFile = null;
        // WHEN / THEN
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(nullFile, mockConnection, null));
        // The exact exception type is defined by Convention.throwIfNull; we only assert that something is thrown.
        assertNotNull(ex.getMessage());
    }

    @Test
    void testTransferFile_nullConnection_throwsException() {
        // GIVEN a null URLConnection
        URLConnection nullConn = null;
        // WHEN / THEN
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(tempFile, nullConn, null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void testTransferFile_withCallback_invokesCallback() throws Exception {
        // GIVEN a simple callback that records the number of bytes transferred.
        class RecordingCallback implements IOUtilities.TransferCallback {

            long bytesSeen = 0L;

            @Override
            public void transferred(long bytes) {
                bytesSeen += bytes;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        }
        RecordingCallback callback = new RecordingCallback();
        // WHEN
        IOUtilities.transfer(tempFile, mockConnection, callback);
        // THEN – the callback must have been invoked at least once and the total bytes must equal the file size.
        assertTrue(callback.bytesSeen > 0, "Callback should have been invoked");
        assertEquals(tempFile.length(), callback.bytesSeen, "Callback should have seen exactly the file length");
        assertEquals(EXPECTED_CONTENT, connectionOut.toString("UTF-8"));
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_nullFile_throwsIllegalArgumentException() {
        // GIVEN a null file reference
        File nullFile = null;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(nullFile, mockConnection, null));
        assertEquals("File cannot be null", ex.getMessage());
    }

    @Test
    void testTransfer_nullConnection_throwsIllegalArgumentException() {
        // GIVEN a null URLConnection
        URLConnection nullConn = null;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(tempFile, nullConn, null));
        assertEquals("URLConnection cannot be null", ex.getMessage());
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_emptyFile_callsCallbackWithZeroBytes() throws Exception {
        // Create an empty temporary file.
        File emptyFile = File.createTempFile("empty", ".txt");
        emptyFile.deleteOnExit();
        class ZeroCallback implements IOUtilities.TransferCallback {

            long bytesSeen = 0L;

            int calls = 0;

            @Override
            public void transferred(long bytes) {
                bytesSeen += bytes;
                calls++;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        }
        ZeroCallback cb = new ZeroCallback();
        IOUtilities.transfer(emptyFile, mockConnection, cb);
        // For an empty file the callback may be called zero or one time with 0 bytes.
        assertTrue(cb.calls <= 1, "Callback should be called at most once for empty file");
        assertEquals(0L, cb.bytesSeen, "Callback must see zero bytes transferred");
        assertEquals(0, connectionOut.size(), "No data should be written to the connection");
    }

    @Test
    void testTransfer_largeFile_multipleCallbackInvocations() throws Exception {
        // Create a file >10 MiB (11 MiB) filled with deterministic data.
        // 11 MiB
        final int size = 11 * 1024 * 1024;
        Path largePath = Files.createTempFile("large", ".bin");
        largePath.toFile().deleteOnExit();
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(largePath, StandardOpenOption.WRITE)) {
            int written = 0;
            while (written < size) {
                int toWrite = Math.min(buffer.length, size - written);
                out.write(buffer, 0, toWrite);
                written += toWrite;
            }
        }
        File largeFile = largePath.toFile();
        class CountingCallback implements IOUtilities.TransferCallback {

            long total = 0L;

            int calls = 0;

            @Override
            public void transferred(long bytes) {
                total += bytes;
                calls++;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        }
        CountingCallback cb = new CountingCallback();
        IOUtilities.transfer(largeFile, mockConnection, cb);
        assertEquals(largeFile.length(), cb.total, "Callback must see total bytes equal to file size");
        assertTrue(cb.calls > 1, "Callback should be invoked multiple times for a large file");
        assertEquals(largeFile.length(), connectionOut.size(), "All bytes must be written to the connection output stream");
    }

    @Test
    void testTransfer_outputStreamThrowsIOException_propagates() throws Exception {
        // Mock an OutputStream that throws IOException on write after a few bytes.
        OutputStream flakyOut = mock(OutputStream.class);
        // First call succeeds, second call throws.
        doNothing().doThrow(new IOException("simulated write failure")).when(flakyOut).write(any(byte[].class), anyInt(), anyInt());
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenReturn(flakyOut);
        // Use a small file to keep the test fast.
        File smallFile = File.createTempFile("flaky", ".txt");
        smallFile.deleteOnExit();
        Files.write(smallFile.toPath(), "1234567890".getBytes());
        class RecordingCallback implements IOUtilities.TransferCallback {

            long bytesSeen = 0L;

            @Override
            public void transferred(long bytes) {
                bytesSeen += bytes;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        }
        RecordingCallback cb = new RecordingCallback();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(smallFile, conn, cb));
        assertEquals("simulated write failure", ex.getMessage());
        // The callback may have received progress for the bytes written before the failure.
        assertTrue(cb.bytesSeen > 0 && cb.bytesSeen < smallFile.length(), "Callback should have seen a partial byte count");
    }

    @Test
    void testTransfer_connectionOutputStreamClosedBeforeTransfer_throwsIOException() throws Exception {
        // Create a ByteArrayOutputStream and close it before the transfer starts.
        ByteArrayOutputStream closedOut = new ByteArrayOutputStream();
        // now any write will cause IOException
        closedOut.close();
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenReturn(closedOut);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, conn, null));
        // The exact message depends on the JDK, we only assert that an IOException is thrown.
        assertNotNull(ex.getMessage());
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_nonexistentFile_propagatesNoSuchFileException() {
        File nonexistent = new File("this_file_does_not_exist_12345.tmp");
        assertFalse(nonexistent.exists(), "File must not exist for this test");
        Exception ex = assertThrows(NoSuchFileException.class, () -> IOUtilities.transfer(nonexistent, mockConnection, null));
        assertTrue(ex.getMessage().contains("this_file_does_not_exist_12345.tmp"));
    }

    @Test
    void testTransfer_directoryAsFile_propagatesAccessDeniedException() throws IOException {
        // Create a temporary directory and pass it as the source file.
        Path dir = Files.createTempDirectory("io_util_dir");
        dir.toFile().deleteOnExit();
        Exception ex = assertThrows(IOException.class, () -> IOUtilities.transfer(dir.toFile(), mockConnection, null));
        // JDK may throw AccessDeniedException or FileSystemException – both are subclasses of IOException.
        assertTrue(ex instanceof AccessDeniedException || ex instanceof FileSystemException, "Expected an access‑related IOException");
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_urlConnectionGetOutputStreamThrowsIOException() throws IOException {
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenThrow(new IOException("getOutputStream failed"));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, conn, null));
        assertEquals("getOutputStream failed", ex.getMessage());
    }

    @Test
    void testTransfer_urlConnectionGetOutputStreamReturnsNull_throwsNullPointerException() throws IOException {
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenReturn(null);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(tempFile, conn, null));
        // The NPE originates from BufferedOutputStream's constructor.
        assertNotNull(ex.getMessage());
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_callbackThrowsRuntimeException_propagates() {
        class BadCallback implements IOUtilities.TransferCallback {

            @Override
            public void transferred(long bytes) {
                throw new RuntimeException("callback failure");
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        }
        BadCallback badCb = new BadCallback();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(tempFile, mockConnection, badCb));
        assertEquals("callback failure", ex.getMessage());
    }

    // ----------------------------------------------------------------------
    @Test
    void testTransfer_outputStreamCloseThrowsIOException_propagates() throws Exception {
        // Custom OutputStream whose close() throws IOException after a successful write.
        class FaultyCloseOutputStream extends OutputStream {

            private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                delegate.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                delegate.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }

            byte[] toByteArray() {
                return delegate.toByteArray();
            }
        }
        FaultyCloseOutputStream faultyOut = new FaultyCloseOutputStream();
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenReturn(faultyOut);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, conn, null));
        assertEquals("close failed", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] null check: {2}")
    @MethodSource("nullCheckProvider")
    void testTransfer_parameterizedNullChecks(File f, URLConnection c, String expectedMessage) throws IOException {
        // The method under test validates arguments before any I/O occurs,
        // therefore we can safely invoke it without additional setup.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(f, c, null));
        assertEquals(expectedMessage, ex.getMessage());
    }
}
