package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_2_Test {

    /**
     * Holds temporary files that need to be cleaned up after the test run.
     */
    private static final List<Path> TEMP_FILES = new ArrayList<>();

    /**
     * Simple test data that will be written to a temporary file.
     */
    private static final byte[] TEST_DATA = "IOUtilities transfer test payload".getBytes();

    /**
     * Records every byte written and counts how many times {@code flush()} is invoked.
     */
    private static class RecordingOutputStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        private int flushCount = 0;

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() {
            flushCount++;
        }

        byte[] getBytes() {
            return delegate.toByteArray();
        }

        int getFlushCount() {
            return flushCount;
        }
    }

    /**
     * Configurable stream that can throw an {@link IOException} on write and/or flush.
     */
    private static class FailingOutputStream extends OutputStream {

        private final boolean throwOnWrite;

        private final boolean throwOnFlush;

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        private int flushCount = 0;

        FailingOutputStream(boolean throwOnWrite, boolean throwOnFlush) {
            this.throwOnWrite = throwOnWrite;
            this.throwOnFlush = throwOnFlush;
        }

        @Override
        public void write(int b) throws IOException {
            if (throwOnWrite) {
                throw new IOException("simulated write failure");
            }
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (throwOnWrite) {
                throw new IOException("simulated write failure");
            }
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            flushCount++;
            if (throwOnFlush) {
                throw new IOException("simulated flush failure");
            }
        }

        byte[] getBytes() {
            return delegate.toByteArray();
        }

        int getFlushCount() {
            return flushCount;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Remove any temporary files created by a single test.
        for (Path p : TEMP_FILES) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        }
        TEMP_FILES.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure any stray temporary files are removed.
        for (Path p : TEMP_FILES) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Verifies that {@code IOUtilities.transfer(File, OutputStream)} copies the full
     * contents of the source file to the destination {@link OutputStream}.
     */
    @Test
    void testTransferFileToOutputStream_success() throws Exception {
        // Arrange – create a temporary file containing known data
        Path tempFile = Files.createTempFile("ioUtilTransfer", ".tmp");
        Files.write(tempFile, TEST_DATA);
        // remember for cleanup
        TEMP_FILES.add(tempFile);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Act
        IOUtilities.transfer(tempFile.toFile(), out);
        // Assert – the output stream must contain exactly the bytes we wrote
        assertArrayEquals(TEST_DATA, out.toByteArray(), "The transferred bytes should match the source file contents");
    }

    /**
     * Ensures that the {@code flush} operation is performed even when the underlying
     * {@link OutputStream} throws an {@link IOException} during the transfer.
     */
    @Test
    void testTransferFileToOutputStream_flushOnException(@Mock OutputStream mockOut) throws Exception {
        // Arrange – a temporary file with some data
        Path tempFile = Files.createTempFile("ioUtilTransferExc", ".tmp");
        Files.write(tempFile, TEST_DATA);
        TEMP_FILES.add(tempFile);
        // Simulate a failure on the first write call
        doThrow(new IOException("simulated write failure")).when(mockOut).write(any(byte[].class), anyInt(), anyInt());
        // Act & Assert – the method should propagate the IOException
        IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile.toFile(), mockOut), "Expected transfer to re‑throw the IOException from the OutputStream");
        assertEquals("simulated write failure", thrown.getMessage());
        // Verify that flush() was still invoked in the finally block
        verify(mockOut, times(1)).flush();
    }

    /**
     * Verifies that a non‑existent source file results in a {@link NoSuchFileException}.
     */
    @Test
    void testTransferFileToOutputStream_fileNotFound() {
        // Arrange – a path that does not exist
        Path nonExistent = Paths.get("this_file_does_not_exist_12345.tmp");
        OutputStream dummyOut = new ByteArrayOutputStream();
        // Act & Assert
        assertThrows(NoSuchFileException.class, () -> IOUtilities.transfer(nonExistent.toFile(), dummyOut), "Transferring a missing file should throw NoSuchFileException");
    }

    /**
     * Successful transfer – verifies that data is fully written and flush is called once.
     */
    @Test
    void testSuccessfulTransfer_RecordingOutputStream() throws Exception {
        Path tempFile = Files.createTempFile("success", ".tmp");
        Files.write(tempFile, "Hello World".getBytes());
        TEMP_FILES.add(tempFile);
        RecordingOutputStream out = new RecordingOutputStream();
        IOUtilities.transfer(tempFile.toFile(), out);
        assertArrayEquals("Hello World".getBytes(), out.getBytes(), "Output should contain the exact file contents");
        assertEquals(1, out.getFlushCount(), "flush() must be invoked exactly once");
    }

    /**
     * When the source file cannot be opened (e.g., does not exist), an IOException is
     * propagated but the OutputStream is still flushed.
     */
    @Test
    void testFileNotFound_flushInvoked() throws Exception {
        Path nonExistent = Files.createTempFile("dir", ".tmp");
        // ensure it does not exist
        Files.deleteIfExists(nonExistent);
        RecordingOutputStream out = new RecordingOutputStream();
        assertThrows(IOException.class, () -> IOUtilities.transfer(nonExistent.toFile(), out), "Expected IOException for missing file");
        assertEquals(1, out.getFlushCount(), "flush() must be invoked even after failure");
        assertEquals(0, out.getBytes().length, "No data should be written on failure");
    }

    /**
     * OutputStream throws IOException during write – exception propagates and flush still runs.
     */
    @Test
    void testOutputStreamWriteFails_flushInvoked() throws Exception {
        Path tempFile = Files.createTempFile("writeFail", ".tmp");
        Files.write(tempFile, "Data".getBytes());
        TEMP_FILES.add(tempFile);
        FailingOutputStream out = new FailingOutputStream(true, false);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile.toFile(), out), "Expected IOException from failing write");
        assertEquals("simulated write failure", ex.getMessage());
        assertEquals(1, out.getFlushCount(), "flush() must be invoked exactly once");
        // partial data may have been written before failure – we do not assert its length
    }

    /**
     * OutputStream.flush() itself throws IOException – the exception from finally is propagated.
     */
    @Test
    void testOutputStreamFlushFails_exceptionPropagated() throws Exception {
        Path tempFile = Files.createTempFile("flushFail", ".tmp");
        Files.write(tempFile, "12345".getBytes());
        TEMP_FILES.add(tempFile);
        FailingOutputStream out = new FailingOutputStream(false, true);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(tempFile.toFile(), out), "Expected IOException from failing flush");
        assertEquals("simulated flush failure", ex.getMessage(), "Exception from flush should be propagated");
        assertEquals(1, out.getFlushCount(), "flush() attempted exactly once");
        assertArrayEquals("12345".getBytes(), out.getBytes(), "Data should have been transferred successfully before flush failure");
    }

    /**
     * Transfer of an empty file – no data written but flush still occurs once.
     */
    @Test
    void testEmptyFileTransfer() throws Exception {
        Path emptyFile = Files.createTempFile("empty", ".tmp");
        // write nothing, keep file empty
        TEMP_FILES.add(emptyFile);
        RecordingOutputStream out = new RecordingOutputStream();
        IOUtilities.transfer(emptyFile.toFile(), out);
        assertEquals(0, out.getBytes().length, "Empty file should result in no bytes written");
        assertEquals(1, out.getFlushCount(), "flush() must be invoked exactly once");
    }

    /**
     * Transfer where file size equals the internal buffer size (32768 bytes).
     */
    @Test
    void testExactBufferSizeTransfer() throws Exception {
        int size = 32768;
        byte[] payload = new byte[size];
        Arrays.fill(payload, (byte) 'x');
        Path file = Files.createTempFile("exactBuffer", ".tmp");
        Files.write(file, payload);
        TEMP_FILES.add(file);
        RecordingOutputStream out = new RecordingOutputStream();
        IOUtilities.transfer(file.toFile(), out);
        assertArrayEquals(payload, out.getBytes(), "Output should contain exactly the buffer‑sized payload");
        assertEquals(1, out.getFlushCount(), "flush() must be called once");
    }

    /**
     * Transfer where file size is larger than the buffer (2 * 32768 bytes).
     */
    @Test
    void testMultipleBufferTransfer() throws Exception {
        // 2 * TRANSFER_BUFFER
        int size = 65536;
        byte[] payload = new byte[size];
        Arrays.fill(payload, (byte) 'y');
        Path file = Files.createTempFile("multiBuffer", ".tmp");
        Files.write(file, payload);
        TEMP_FILES.add(file);
        RecordingOutputStream out = new RecordingOutputStream();
        IOUtilities.transfer(file.toFile(), out);
        assertArrayEquals(payload, out.getBytes(), "All bytes must be transferred even when spanning multiple buffers");
        assertEquals(1, out.getFlushCount(), "flush() must be called exactly once");
    }

    /**
     * Attempt to transfer a directory – should result in IOException and still flush.
     */
    @Test
    void testDirectoryAsFile() throws Exception {
        Path dir = Files.createTempDirectory("asFileDir");
        // will be removed in teardown
        TEMP_FILES.add(dir);
        RecordingOutputStream out = new RecordingOutputStream();
        assertThrows(IOException.class, () -> IOUtilities.transfer(dir.toFile(), out), "Transferring a directory should throw IOException");
        assertEquals(1, out.getFlushCount(), "flush() must be invoked even after directory error");
        assertEquals(0, out.getBytes().length, "No data should be written when source is a directory");
    }

    /**
     * Null file argument – expect NullPointerException and no flush on the output stream.
     */
    @Test
    void testNullFileArgument() throws Exception {
        RecordingOutputStream out = new RecordingOutputStream();
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, out), "Passing null as file should throw NullPointerException");
        // According to the specification, flush should NOT be called when the file argument is null.
        assertEquals(0, out.getFlushCount(), "flush() must not be invoked when file argument is null");
    }

    /**
     * Null OutputStream argument – expect NullPointerException during write/flush.
     */
    @Test
    void testNullOutputStreamArgument() throws Exception {
        Path tempFile = Files.createTempFile("nullOut", ".tmp");
        Files.write(tempFile, "data".getBytes());
        TEMP_FILES.add(tempFile);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(tempFile.toFile(), null), "Passing null as OutputStream should result in NullPointerException");
        // The NPE is thrown from inside the try block; the finally block attempts to flush null,
        // which also throws NPE, but the first thrown exception is sufficient for the test.
        // No further verification is needed.
    }
}
