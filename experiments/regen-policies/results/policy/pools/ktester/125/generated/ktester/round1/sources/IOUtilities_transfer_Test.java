package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URL;
import java.net.URLConnection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_Test {

    /**
     * Temporary directory provided by JUnit 5.
     */
    @TempDir
    static Path tempDir;

    /**
     * Holds the temporary file used by a test – cleared after each test.
     */
    private File tempFile;

    /**
     * Mocked URLConnection that supplies a controllable OutputStream.
     */
    @Mock
    private URLConnection mockConnection;

    /**
     * Captures what the method writes to the connection.
     */
    private ByteArrayOutputStream capturedOutput;

    class SimpleCallback implements IOUtilities.TransferCallback {

        long reported = 0L;

        @Override
        public boolean progress(long bytes) {
            reported += bytes;
            // Return true to indicate “continue”.
            return true;
        }
    }

    private static class NormalCase {

        final String payload;

        final long expectedBytes;

        final String description;

        NormalCase(String payload, long expectedBytes, String description) {
            this.payload = payload;
            this.expectedBytes = expectedBytes;
            this.description = description;
        }
    }

    class RecordingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        @Override
        public boolean progress(long bytes) {
            total += bytes;
            // continue
            return true;
        }
    }

    class CountingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        @Override
        public boolean progress(long bytes) {
            total += bytes;
            return true;
        }
    }

    class RecordingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        @Override
        public boolean progress(long bytes) {
            total += bytes;
            // continue
            return true;
        }
    }

    class CountingCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        @Override
        public boolean progress(long bytes) {
            total += bytes;
            return true;
        }
    }

    class SimpleCallback implements IOUtilities.TransferCallback {

        long total = 0L;

        @Override
        public boolean progress(long bytes) {
            total += bytes;
            return true;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file for each test.
        Path filePath = Files.createTempFile(tempDir, "ioTest", ".txt");
        tempFile = filePath.toFile();
        // Write a known payload to the file.
        String payload = "This is for an IO test!";
        Files.write(filePath, payload.getBytes(), StandardOpenOption.WRITE);
        // Prepare the mocked connection.
        capturedOutput = new ByteArrayOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(capturedOutput);
    }

    @AfterEach
    void teardownAfterEach() {
        // Delete the temporary file if it still exists.
        if (tempFile != null && tempFile.exists()) {
            assertTrue(tempFile.delete(), "Failed to delete temporary file");
        }
        // Reset the mock for the next test.
        reset(mockConnection);
    }

    /* -----------------------------------------------------------
     *  Normal operation – parameterised tests
     * ----------------------------------------------------------- */
    private static Stream<NormalCase> normalCases() {
        return Stream.of(new NormalCase("hello world", "hello world".getBytes().length, "normal content"), new NormalCase("", 0, "empty file"));
    }

    /* -----------------------------------------------------------
     *  Helper method to create a temporary file with given bytes.
     * ----------------------------------------------------------- */
    private void writeBytesToTempFile(byte[] bytes) throws IOException {
        Files.write(tempFile.toPath(), bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    void testTransferFileToOutputStream() throws Exception {
        // Act – invoke the method under test.
        IOUtilities.transfer(tempFile, mockConnection, null);
        // Assert – the data written to the mocked connection matches the file content.
        String expected = "This is for an IO test!";
        String actual = capturedOutput.toString("UTF-8");
        assertEquals(expected, actual, "The transferred bytes should match the file content");
    }

    @Test
    void testTransferFileToOutputStreamWithCallback() throws Exception {
        // Arrange – create a simple callback that records the total number of bytes reported.
        class SimpleCallback implements IOUtilities.TransferCallback {

            long reported = 0L;

            @Override
            public boolean progress(long bytes) {
                reported += bytes;
                // Return true to indicate “continue”.
                return true;
            }
        }
        SimpleCallback callback = new SimpleCallback();
        // Act.
        IOUtilities.transfer(tempFile, mockConnection, callback);
        // Assert – verify that the callback was invoked and that the total reported bytes equal the file size.
        long fileSize = tempFile.length();
        assertEquals(fileSize, callback.reported, "Callback should have been notified of the exact number of bytes transferred");
        assertEquals("This is for an IO test!", capturedOutput.toString("UTF-8"));
    }

    @Test
    void testTransferFileWhenConnectionThrowsIOException() throws Exception {
        // Arrange – make the connection throw when getOutputStream() is called.
        when(mockConnection.getOutputStream()).thenThrow(new IOException("simulated failure"));
        // Act & Assert – the method should propagate the exception.
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, mockConnection, null));
        assertEquals("simulated failure", ex.getMessage());
        // Verify that the input stream was still closed (no exception should escape the finally block).
        // Since close() is a static utility method, we rely on the fact that no additional
        // exception is thrown after the original one.
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalCases")
    void testTransferFile_NormalOperation(NormalCase tc) throws Exception {
        // Write the specific payload for this iteration.
        Files.write(tempFile.toPath(), tc.payload.getBytes());
        // Simple callback that records the total bytes reported.
        class RecordingCallback implements IOUtilities.TransferCallback {

            long total = 0L;

            @Override
            public boolean progress(long bytes) {
                total += bytes;
                // continue
                return true;
            }
        }
        RecordingCallback callback = new RecordingCallback();
        // Act
        IOUtilities.transfer(tempFile, mockConnection, callback);
        // Assert output matches file content.
        assertArrayEquals(tc.payload.getBytes(), capturedOutput.toByteArray(), "Output stream should contain the exact file bytes");
        // Assert callback received the correct total.
        assertEquals(tc.expectedBytes, callback.total, "Callback should be notified of the exact number of transferred bytes");
    }

    /* -----------------------------------------------------------
     *  Large file (multiple buffer cycles)
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_LargeFileMultipleBuffers() throws Exception {
        // 200 KB of repeating pattern "0123456789"
        // 200 KB
        int size = 200 * 1024;
        byte[] pattern = "0123456789".getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        while (baos.size() < size) {
            baos.write(pattern, 0, Math.min(pattern.length, size - baos.size()));
        }
        byte[] data = baos.toByteArray();
        Files.write(tempFile.toPath(), data);
        class CountingCallback implements IOUtilities.TransferCallback {

            long total = 0L;

            @Override
            public boolean progress(long bytes) {
                total += bytes;
                return true;
            }
        }
        CountingCallback cb = new CountingCallback();
        IOUtilities.transfer(tempFile, mockConnection, cb);
        assertArrayEquals(data, capturedOutput.toByteArray(), "All 200 KB should be transferred unchanged");
        assertEquals(size, cb.total, "Callback must report the full byte count");
    }

    /* -----------------------------------------------------------
     *  Binary data containing zero bytes
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_BinaryDataWithZeroBytes() throws Exception {
        byte[] binary = new byte[] { 0x00, (byte) 0xFF, 0x7F, 0x00, 0x01 };
        Files.write(tempFile.toPath(), binary);
        class SimpleCallback implements IOUtilities.TransferCallback {

            long total = 0L;

            @Override
            public boolean progress(long bytes) {
                total += bytes;
                return true;
            }
        }
        SimpleCallback cb = new SimpleCallback();
        IOUtilities.transfer(tempFile, mockConnection, cb);
        assertArrayEquals(binary, capturedOutput.toByteArray(), "Binary sequence must be transferred unchanged");
        assertEquals(binary.length, cb.total, "Callback should report 5 bytes");
    }

    /* -----------------------------------------------------------
     *  Null arguments
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_NullFileArgument() {
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, mockConnection, bytes -> true));
    }

    @Test
    void testTransferFile_NullURLConnectionArgument() throws IOException {
        // Use a valid temporary file.
        Files.write(tempFile.toPath(), "test".getBytes());
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(tempFile, null, bytes -> true));
    }

    @Test
    void testTransferFile_NullCallbackArgument() throws IOException {
        Files.write(tempFile.toPath(), "data".getBytes());
        // According to the supplied test cases, a null callback should cause NPE.
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(tempFile, mockConnection, null));
    }

    /* -----------------------------------------------------------
     *  Callback throws exception
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_CallbackThrowsException() throws IOException {
        Files.write(tempFile.toPath(), "abc".getBytes());
        IOUtilities.TransferCallback badCallback = new IOUtilities.TransferCallback() {

            @Override
            public boolean progress(long bytes) {
                throw new RuntimeException("callback failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(tempFile, mockConnection, badCallback));
        assertEquals("callback failure", ex.getMessage());
        // Ensure streams are closed – the capturedOutput should be closed, attempting
        // another write should fail.
        assertThrows(IOException.class, () -> capturedOutput.write(1));
    }

    /* -----------------------------------------------------------
     *  URLConnection output stream throws IOException
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_OutputStreamThrowsIOException() throws IOException {
        Files.write(tempFile.toPath(), "xyz".getBytes());
        when(mockConnection.getOutputStream()).thenThrow(new IOException("simulated failure"));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
        assertEquals("simulated failure", ex.getMessage());
    }

    /* -----------------------------------------------------------
     *  Non‑existent file
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_NonExistentFile() {
        File nonExistent = tempDir.resolve("doesNotExist.txt").toFile();
        assertFalse(nonExistent.exists());
        assertThrows(NoSuchFileException.class, () -> IOUtilities.transfer(nonExistent, mockConnection, bytes -> true));
    }

    /* -----------------------------------------------------------
     *  File is a directory
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_DirectoryArgument() throws IOException {
        // Use the temporary directory itself as the "file".
        File dir = tempDir.toFile();
        assertTrue(dir.isDirectory());
        assertThrows(IOException.class, () -> IOUtilities.transfer(dir, mockConnection, bytes -> true));
    }

    /* -----------------------------------------------------------
     *  Unreadable file (AccessDeniedException)
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_UnreadableFile() throws IOException {
        Files.write(tempFile.toPath(), "secret".getBytes());
        // Remove read permission.
        tempFile.setReadable(false, false);
        try {
            assertThrows(AccessDeniedException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
        } finally {
            // Restore permission for cleanup.
            tempFile.setReadable(true, false);
        }
    }

    /* -----------------------------------------------------------
     *  getOutputStream returns null
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_OutputStreamReturnsNull() throws IOException {
        Files.write(tempFile.toPath(), "test".getBytes());
        when(mockConnection.getOutputStream()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
    }

    /* -----------------------------------------------------------
     *  InputStream read throws IOException (using static mock)
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_InputStreamReadThrowsIOException() throws IOException {
        // Custom InputStream that throws after delivering 3 bytes.
        byte[] data = "abcdef".getBytes();
        InputStream flaky = new InputStream() {

            int pos = 0;

            @Override
            public int read() throws IOException {
                if (pos < 3) {
                    return data[pos++];
                }
                throw new IOException("simulated read failure");
            }
        };
        try (MockedStatic<Files> mocked = mockStatic(Files.class)) {
            mocked.when(() -> Files.newInputStream(any(Path.class))).thenReturn(flaky);
            // The file itself is irrelevant because the static mock intercepts the call.
            assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
        }
    }

    /* -----------------------------------------------------------
     *  OutputStream write throws IOException (custom stream)
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_OutputStreamWriteThrowsIOException() throws IOException {
        Files.write(tempFile.toPath(), "1234567890".getBytes());
        OutputStream badOut = new OutputStream() {

            int count = 0;

            @Override
            public void write(int b) throws IOException {
                if (count++ < 5) {
                    // allow first 5 bytes
                    return;
                }
                throw new IOException("simulated write failure");
            }
        };
        when(mockConnection.getOutputStream()).thenReturn(badOut);
        assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
    }

    /* -----------------------------------------------------------
     *  close method throws IOException (static mock of IOUtilities.close)
     * ----------------------------------------------------------- */
    @Test
    void testTransferFile_CloseThrowsIOException() throws IOException {
        Files.write(tempFile.toPath(), "data".getBytes());
        // Mock the static close methods to throw when called.
        try (MockedStatic<IOUtilities> mocked = mockStatic(IOUtilities.class)) {
            // For any Closeable, throw IOException.
            mocked.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> {
                throw new IOException("close failure");
            });
            // The transfer method will attempt to close both streams in finally.
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile, mockConnection, bytes -> true));
            assertEquals("close failure", ex.getMessage());
        }
    }
}
