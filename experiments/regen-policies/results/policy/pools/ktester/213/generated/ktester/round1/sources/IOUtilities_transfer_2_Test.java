package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_2_Test {

    /**
     * Temporary file used by the tests
     */
    private static Path tempFile;

    /**
     * Expected content written to the temporary file
     */
    private static final String EXPECTED = "IOUtilities.transfer(File,OutputStream) test data";

    class FaultyFlushOutputStream extends ByteArrayOutputStream {

        @Override
        public void flush() throws IOException {
            throw new IOException("forced flush failure");
        }
    }

    class FaultyFlushOutputStream extends ByteArrayOutputStream {

        @Override
        public void flush() throws IOException {
            throw new IOException("forced flush failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary file that will be reused for all tests.
        tempFile = Files.createTempFile("IOUtilitiesTransferTest", ".txt");
        // Ensure the file is deleted on JVM exit in case a test forgets to clean it.
        tempFile.toFile().deleteOnExit();
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Populate the temporary file with known data before each test.
        Files.writeString(tempFile, EXPECTED);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Clear the file content after each test to avoid cross‑test contamination.
        Files.newBufferedWriter(tempFile).close();
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Delete the temporary file when all tests have finished.
        Files.deleteIfExists(tempFile);
    }

    /**
     * Verifies that {@link IOUtilities#transfer(File, OutputStream)} copies the exact bytes
     * from the source file to the provided {@link OutputStream} and that the stream is flushed.
     */
    @Test
    void testTransferFileToOutputStream() throws IOException {
        // GIVEN – a file containing known data and a fresh ByteArrayOutputStream
        File source = tempFile.toFile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // WHEN – the method under test is invoked
        IOUtilities.transfer(source, out);
        // THEN – the output stream contains the same data as the source file
        String actual = out.toString("UTF-8");
        assertEquals(EXPECTED, actual, "The transferred data should match the original file content");
    }

    /**
     * Verifies that the method correctly handles a {@code null} file argument
     * by throwing the expected {@link NullPointerException} (wrapped by Convention).
     */
    @Test
    void testTransferFileNullArgument() {
        OutputStream dummy = new ByteArrayOutputStream();
        Exception ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, dummy));
        assertTrue(ex.getMessage().contains("File cannot be null"));
    }

    /**
     * Verifies that the method correctly handles a {@code null} output stream argument.
     */
    @Test
    void testTransferOutputStreamNullArgument() throws IOException {
        File source = tempFile.toFile();
        Exception ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(source, null));
        assertTrue(ex.getMessage().contains("OutputStream cannot be null"));
    }

    /**
     * Normal case – a small text file is transferred to a {@link ByteArrayOutputStream}.
     */
    @Test
    void testTransferSmallFile() throws IOException {
        Path small = Files.createTempFile("small", ".txt");
        try {
            Files.writeString(small, "Hello World");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtilities.transfer(small.toFile(), out);
            assertEquals("Hello World", out.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(small);
        }
    }

    /**
     * Edge case – an empty file should result in an empty output stream.
     */
    @Test
    void testTransferEmptyFile() throws IOException {
        Path empty = Files.createTempFile("empty", ".txt");
        try {
            // ensure file is truly empty
            Files.write(empty, new byte[0]);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtilities.transfer(empty.toFile(), out);
            assertEquals(0, out.size(), "Output stream must be empty for an empty source file");
        } finally {
            Files.deleteIfExists(empty);
        }
    }

    /**
     * Error handling – attempting to transfer a file that does not exist should
     * propagate the underlying {@link java.nio.file.NoSuchFileException}.
     */
    @Test
    void testTransferNonExistentFile() {
        Path missing = Paths.get("nonExistentFile.txt");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Exception ex = assertThrows(NoSuchFileException.class, () -> IOUtilities.transfer(missing.toFile(), out));
        assertTrue(ex.getMessage().contains("nonExistentFile.txt"));
    }

    /**
     * Propagation – if the provided {@link OutputStream} throws an {@link IOException}
     * during {@code write}, the method should propagate that exception.
     */
    @Test
    void testTransferWithFaultyOutputStreamWrite() throws IOException {
        Path src = Files.createTempFile("faultyWrite", ".txt");
        try {
            Files.writeString(src, "Hello World");
            OutputStream faulty = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    throw new IOException("forced write failure");
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    throw new IOException("forced write failure");
                }
            };
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(src.toFile(), faulty));
            assertEquals("forced write failure", ex.getMessage());
        } finally {
            Files.deleteIfExists(src);
        }
    }

    /**
     * Buffering test – a file larger than the internal {@code TRANSFER_BUFFER}
     * (32 768 bytes) forces multiple read/write cycles; verifies complete transfer.
     */
    @Test
    void testTransferLargeFile() throws IOException {
        // 100 KB of random data
        byte[] data = new byte[100 * 1024];
        new Random().nextBytes(data);
        Path large = Files.createTempFile("large", ".bin");
        try {
            Files.write(large, data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtilities.transfer(large.toFile(), out);
            assertArrayEquals(data, out.toByteArray(), "The transferred bytes must be identical to the source file");
        } finally {
            Files.deleteIfExists(large);
        }
    }

    /**
     * Security validation – the file exists but the process lacks read permission.
     * Expect an {@link AccessDeniedException} (or a generic {@link IOException}
     * depending on the platform).
     */
    @Test
    void testTransferFileNoReadPermission() throws IOException {
        Path secret = Files.createTempFile("noRead", ".txt");
        try {
            Files.writeString(secret, "secret data");
            // Attempt to remove read permission (POSIX only). On Windows the test is skipped.
            if (Files.getFileStore(secret).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(secret);
                perms.remove(PosixFilePermission.OWNER_READ);
                Files.setPosixFilePermissions(secret, perms);
            } else {
                // On non‑POSIX file systems we cannot reliably enforce the permission,
                // so we skip the test rather than produce a false failure.
                Assumptions.assumeTrue(false, "POSIX file permissions not supported on this platform");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(secret.toFile(), out));
            // The exact subclass may vary (AccessDeniedException, FileSystemException, …)
            assertTrue(ex instanceof AccessDeniedException || ex instanceof FileSystemException, "Expected an access‑denied related IOException");
        } finally {
            // Restore permissions so the temporary file can be deleted.
            if (Files.exists(secret)) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(secret);
                perms.add(PosixFilePermission.OWNER_READ);
                Files.setPosixFilePermissions(secret, perms);
                Files.deleteIfExists(secret);
            }
        }
    }

    /**
     * Type validation – attempting to open a directory as a file should cause a
     * {@link FileSystemException} (e.g., “Is a directory”).
     */
    @Test
    void testTransferDirectoryAsFile() throws IOException {
        Path dir = Files.createTempDirectory("dirAsFile");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FileSystemException ex = assertThrows(FileSystemException.class, () -> IOUtilities.transfer(dir.toFile(), out));
            assertTrue(ex.getMessage().toLowerCase().contains("directory"), "Exception message should indicate that the source is a directory");
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    /**
     * Resource cleanup validation – the transfer succeeds, but {@code flush(out)}
     * throws an {@link IOException}. The method should propagate this exception.
     */
    @Test
    void testTransferWithFaultyFlush() throws IOException {
        Path src = Files.createTempFile("faultyFlush", ".txt");
        try {
            Files.writeString(src, "Hello World");
            // OutputStream that works for write but fails on flush
            class FaultyFlushOutputStream extends ByteArrayOutputStream {

                @Override
                public void flush() throws IOException {
                    throw new IOException("forced flush failure");
                }
            }
            FaultyFlushOutputStream out = new FaultyFlushOutputStream();
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(src.toFile(), out));
            assertEquals("forced flush failure", ex.getMessage());
        } finally {
            Files.deleteIfExists(src);
        }
    }
}
