package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Random;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_6_Test {

    /**
     * Temporary file used as the destination of the transfer.
     */
    private Path tempFilePath;

    /**
     * The {@link File} view of {@link #tempFilePath}.
     */
    private File tempFile;

    /**
     * Mocked callback that will receive the number of transferred bytes.
     */
    @Mock
    private IOUtilities.TransferCallback callback;

    /**
     * InputStream that throws an IOException after a configurable number of successful reads.
     */
    private static class FaultyInputStream extends InputStream {

        private final byte[] data;

        // number of bytes after which IOException is thrown
        private final int failAfter;

        private int pos = 0;

        FaultyInputStream(byte[] data, int failAfter) {
            this.data = data;
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (pos >= failAfter) {
                throw new IOException("Simulated read failure after " + failAfter + " bytes");
            }
            if (pos >= data.length) {
                return -1;
            }
            return data[pos++] & 0xFF;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file for each test.
        tempFilePath = Files.createTempFile("ioUtilitiesTransferTest", ".tmp");
        tempFile = tempFilePath.toFile();
        // Ensure the file is empty before each test.
        try (OutputStream out = new FileOutputStream(tempFile)) {
            out.write(new byte[0]);
        }
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Delete the temporary file after each test.
        Files.deleteIfExists(tempFilePath);
    }

    @Test
    void transfer_copiesAllBytesAndInvokesCallback() throws Exception {
        // GIVEN
        byte[] sourceData = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(sourceData)) {
            // WHEN
            IOUtilities.transfer(source, tempFile, callback);
        }
        // THEN – the file now contains exactly the source data.
        byte[] fileData = Files.readAllBytes(tempFilePath);
        assertArrayEquals(sourceData, fileData, "File content should match the source InputStream");
        // AND – the callback was notified with the exact number of transferred bytes.
        // The TransferCallback interface in the original library defines a single method:
        //   void transferred(long bytes);
        // If the method name changes, adjust the verification accordingly.
        verify(callback, times(1)).transferred(sourceData.length);
        verifyNoMoreInteractions(callback);
    }

    @Test
    void transfer_withEmptyInputWritesEmptyFile() throws Exception {
        // GIVEN
        byte[] empty = new byte[0];
        try (InputStream source = new ByteArrayInputStream(empty)) {
            // WHEN
            IOUtilities.transfer(source, tempFile, callback);
        }
        // THEN – the destination file is empty.
        byte[] fileData = Files.readAllBytes(tempFilePath);
        assertEquals(0, fileData.length, "File should be empty when source InputStream is empty");
        // AND – the callback is still invoked (with 0 bytes transferred).
        verify(callback, times(1)).transferred(0L);
        verifyNoMoreInteractions(callback);
    }

    @Test
    void transfer_nullInputStream_throwsNullPointerException() {
        // GIVEN
        InputStream nullStream = null;
        // WHEN / THEN – a NullPointerException is expected because the method tries to read from the stream.
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(nullStream, tempFile, callback));
    }

    @Test
    void transfer_smallData_verifiesFileContentAndCallback() throws Exception {
        // GIVEN – a small ByteArrayInputStream containing "Hello World"
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(data)) {
            // WHEN
            IOUtilities.transfer(source, tempFile, callback);
        }
        // THEN – file contains the exact bytes
        assertArrayEquals(data, Files.readAllBytes(tempFilePath));
        // AND – callback received 11 (length of "Hello World")
        verify(callback, times(1)).transferred(data.length);
        verifyNoMoreInteractions(callback);
    }

    @Test
    void transfer_nullFile_throwsNullPointerException() {
        // GIVEN
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(data)) {
            // WHEN / THEN – passing a null File should cause NPE before any I/O occurs
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(source, null, callback));
        }
    }

    @Test
    void transfer_nullCallback_throwsNullPointerException() {
        // GIVEN
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(data)) {
            // WHEN / THEN – passing a null callback should cause NPE
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(source, tempFile, null));
        }
    }

    @Test
    void transfer_largeData_handlesOneMiBCorrectly() throws Exception {
        // GIVEN – 1 MiB of random data
        byte[] largeData = new byte[1_048_576];
        new Random(0).nextBytes(largeData);
        try (InputStream source = new ByteArrayInputStream(largeData)) {
            // WHEN
            IOUtilities.transfer(source, tempFile, callback);
        }
        // THEN – file size matches and content is identical
        byte[] fileData = Files.readAllBytes(tempFilePath);
        assertEquals(largeData.length, fileData.length);
        assertArrayEquals(largeData, fileData);
        // AND – callback received the full byte count
        verify(callback, times(1)).transferred(largeData.length);
        verifyNoMoreInteractions(callback);
    }

    @Test
    void transfer_inputStreamThrowsIOException_propagatesAndPartialFileWritten() {
        // GIVEN – 10 bytes total, but the stream fails after 5 bytes
        byte[] data = "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new FaultyInputStream(data, 5)) {
            // WHEN / THEN – IOException should propagate
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(source, tempFile, callback));
            assertTrue(ex.getMessage().contains("Simulated read failure"));
        }
        // THEN – file should contain the first 5 bytes that were successfully written
        try {
            byte[] fileData = Files.readAllBytes(tempFilePath);
            assertArrayEquals("ABCDE".getBytes(StandardCharsets.UTF_8), fileData, "File should contain the bytes written before the failure");
        } catch (IOException e) {
            fail("Unable to read temporary file", e);
        }
        // AND – callback should have been invoked with the count of bytes transferred before failure
        verify(callback, times(1)).transferred(5L);
        verifyNoMoreInteractions(callback);
    }

    @Test
    void transfer_fileNotWritable_throwsIOException() throws IOException {
        // GIVEN – a read‑only temporary directory
        Path readOnlyDir = Files.createTempDirectory("readonlyDir");
        try {
            // Make directory read‑only
            readOnlyDir.toFile().setWritable(false, false);
            File unwritableFile = new File(readOnlyDir.toFile(), "noWrite.txt");
            byte[] data = "data".getBytes(StandardCharsets.UTF_8);
            try (InputStream source = new ByteArrayInputStream(data)) {
                // WHEN / THEN – attempting to write should throw an IOException (or AccessDeniedException)
                assertThrows(IOException.class, () -> IOUtilities.transfer(source, unwritableFile, callback));
            }
        } finally {
            // Cleanup: make the directory writable again so it can be deleted
            readOnlyDir.toFile().setWritable(true, false);
            Files.walk(readOnlyDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void transfer_callbackThrowsRuntimeException_propagates() {
        // GIVEN – a callback that throws on the first transferred() call
        doThrow(new RuntimeException("Callback failure")).when(callback).transferred(anyLong());
        byte[] data = "callback".getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(data)) {
            // WHEN / THEN – the RuntimeException should propagate out of the method
            RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(source, tempFile, callback));
            assertEquals("Callback failure", ex.getMessage());
        }
        // The file may be empty or partially written; the contract does not guarantee atomicity.
        // We only assert that the exception was not swallowed.
        verify(callback, times(1)).transferred(data.length);
    }
}
