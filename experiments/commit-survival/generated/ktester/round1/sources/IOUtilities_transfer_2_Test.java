package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_2_Test {

    /**
     * Temporary directory provided by JUnit 5
     */
    @TempDir
    Path tempDir;

    /**
     * An OutputStream that throws an {@link IOException} on the first write operation.
     */
    private static class ThrowOnWriteOutputStream extends OutputStream {

        private boolean firstWrite = true;

        @Override
        public void write(int b) throws IOException {
            if (firstWrite) {
                firstWrite = false;
                throw new IOException("Simulated write failure");
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (firstWrite) {
                firstWrite = false;
                throw new IOException("Simulated write failure");
            }
        }
    }

    /**
     * An OutputStream that records data but throws an {@link IOException} when {@code flush()} is called.
     */
    private static class ThrowOnFlushOutputStream extends ByteArrayOutputStream {

        @Override
        public void flush() throws IOException {
            throw new IOException("Simulated flush failure");
        }
    }

    /**
     * Verifies that {@code IOUtilities.transfer(File, OutputStream)} copies the exact
     * bytes from the source file to the destination stream and that the stream is
     * flushed after the transfer.
     */
    @Test
    void testTransferCopiesFileToOutputAndFlushes() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        byte[] original = "The quick brown fox jumps over the lazy dog".getBytes();
        // create a temporary file with known content
        Path filePath = Files.createFile(tempDir.resolve("source.txt"));
        Files.write(filePath, original);
        File sourceFile = filePath.toFile();
        // use a spy on a real ByteArrayOutputStream so we can verify flush()
        ByteArrayOutputStream realOut = new ByteArrayOutputStream();
        OutputStream outSpy = spy(realOut);
        // ----- WHEN ------------------------------------------------------------
        IOUtilities.transfer(sourceFile, outSpy);
        // ----- THEN ------------------------------------------------------------
        // content must be identical
        assertArrayEquals(original, realOut.toByteArray(), "The bytes written to the OutputStream must match the file content");
        // flush must have been called exactly once (the method calls flush in finally)
        verify(outSpy, times(1)).flush();
    }

    /**
     * Ensures that a non‑existent source file results in an {@link IOException}
     * (specifically a {@link FileNotFoundException}) being thrown.
     */
    @Test
    void testTransferWithMissingFileThrowsException() {
        // ----- GIVEN -----------------------------------------------------------
        File missingFile = tempDir.resolve("does_not_exist.txt").toFile();
        OutputStream dummyOut = spy(new ByteArrayOutputStream());
        // ----- WHEN / THEN ----------------------------------------------------
        FileNotFoundException ex = assertThrows(FileNotFoundException.class, () -> IOUtilities.transfer(missingFile, dummyOut), "Transferring from a non‑existent file should throw FileNotFoundException");
        // even though the file cannot be opened, the finally block should still flush
        verify(dummyOut, times(1)).flush();
        assertNotNull(ex.getMessage());
    }

    @Test
    void testTransferEmptyFileWritesNothingButFlushes() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        Path emptyPath = Files.createFile(tempDir.resolve("empty.txt"));
        File emptyFile = emptyPath.toFile();
        ByteArrayOutputStream realOut = new ByteArrayOutputStream();
        OutputStream outSpy = spy(realOut);
        // ----- WHEN ------------------------------------------------------------
        IOUtilities.transfer(emptyFile, outSpy);
        // ----- THEN ------------------------------------------------------------
        assertEquals(0, realOut.size(), "Empty file should result in no data written");
        verify(outSpy, times(1)).flush();
    }

    @Test
    void testTransferNullFileThrowsNullPointerExceptionAndFlushes() {
        // ----- GIVEN -----------------------------------------------------------
        File nullFile = null;
        OutputStream outSpy = spy(new ByteArrayOutputStream());
        // ----- WHEN / THEN ----------------------------------------------------
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(nullFile, outSpy), "Passing a null File should result in NullPointerException");
        // flush is still attempted in finally
        verify(outSpy, times(1)).flush();
        assertNotNull(npe.getMessage());
    }

    @Test
    void testTransferNullOutputStreamThrowsNullPointerException() {
        // ----- GIVEN -----------------------------------------------------------
        Path filePath = Files.createFile(tempDir.resolve("some.txt"));
        Files.write(filePath, new byte[] { 1, 2, 3 });
        File sourceFile = filePath.toFile();
        OutputStream nullOut = null;
        // ----- WHEN / THEN ----------------------------------------------------
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(sourceFile, nullOut), "Passing a null OutputStream should result in NullPointerException");
        // No flush can be performed because the reference itself is null
        assertNotNull(npe.getMessage());
    }

    @Test
    void testTransferUnreadableFileThrowsIOExceptionAndFlushes() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        Path unreadablePath = Files.createFile(tempDir.resolve("unreadable.txt"));
        Files.write(unreadablePath, new byte[] { 9, 9, 9 });
        // Remove all permissions (POSIX only; on Windows the test may be skipped)
        try {
            Set<PosixFilePermission> noPerms = EnumSet.noneOf(PosixFilePermission.class);
            Files.setPosixFilePermissions(unreadablePath, noPerms);
        } catch (UnsupportedOperationException | IOException ignored) {
            // If the platform does not support POSIX permissions, we cannot reliably test this case.
            // In that situation we simply return, letting the test pass trivially.
            return;
        }
        File unreadableFile = unreadablePath.toFile();
        OutputStream outSpy = spy(new ByteArrayOutputStream());
        // ----- WHEN / THEN ----------------------------------------------------
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(unreadableFile, outSpy), "Opening a file without read permission should throw IOException");
        // flush still executed
        verify(outSpy, times(1)).flush();
        assertNotNull(ex.getMessage());
    }

    @Test
    void testTransferDirectoryAsFileThrowsIOExceptionAndFlushes() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        Path dirPath = Files.createDirectory(tempDir.resolve("someDir"));
        File dirAsFile = dirPath.toFile();
        OutputStream outSpy = spy(new ByteArrayOutputStream());
        // ----- WHEN / THEN ----------------------------------------------------
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(dirAsFile, outSpy), "Passing a directory where a regular file is expected should throw IOException");
        verify(outSpy, times(1)).flush();
        assertNotNull(ex.getMessage());
    }

    @Test
    void testTransferOutputStreamThrowsOnWritePropagatesException() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        byte[] data = { 10, 20, 30 };
        Path filePath = Files.createFile(tempDir.resolve("writeFail.txt"));
        Files.write(filePath, data);
        File sourceFile = filePath.toFile();
        ThrowOnWriteOutputStream badOut = new ThrowOnWriteOutputStream();
        // ----- WHEN / THEN ----------------------------------------------------
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(sourceFile, badOut), "IOException from OutputStream.write should be propagated");
        assertEquals("Simulated write failure", ex.getMessage());
        // The finally block still attempts to flush; our badOut does not override flush,
        // so flush succeeds – we just ensure no additional exception masks the original.
    }

    @Test
    void testTransferOutputStreamThrowsOnFlushPropagatesException() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        byte[] data = { 7, 8, 9 };
        Path filePath = Files.createFile(tempDir.resolve("flushFail.txt"));
        Files.write(filePath, data);
        File sourceFile = filePath.toFile();
        ThrowOnFlushOutputStream badFlushOut = new ThrowOnFlushOutputStream();
        // ----- WHEN / THEN ----------------------------------------------------
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(sourceFile, badFlushOut), "IOException from OutputStream.flush should be propagated");
        assertEquals("Simulated flush failure", ex.getMessage());
        // Verify that the data was indeed written before the flush failure
        assertArrayEquals(data, badFlushOut.toByteArray(), "Data should be transferred before the flush exception occurs");
    }

    @Test
    void testTransferFileExactlyBufferSize() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        final int bufferSize = 32768;
        byte[] payload = new byte[bufferSize];
        Arrays.fill(payload, (byte) 0x55);
        Path filePath = Files.createFile(tempDir.resolve("exactBuffer.bin"));
        Files.write(filePath, payload);
        File sourceFile = filePath.toFile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // ----- WHEN ------------------------------------------------------------
        IOUtilities.transfer(sourceFile, out);
        // ----- THEN ------------------------------------------------------------
        assertArrayEquals(payload, out.toByteArray(), "File whose length equals the internal buffer should be transferred correctly");
    }

    @Test
    void testTransferFileJustOverBufferSize() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        final int bufferSize = 32768;
        byte[] payload = new byte[bufferSize + 1];
        Arrays.fill(payload, (byte) 0xAA);
        Path filePath = Files.createFile(tempDir.resolve("justOverBuffer.bin"));
        Files.write(filePath, payload);
        File sourceFile = filePath.toFile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // ----- WHEN ------------------------------------------------------------
        IOUtilities.transfer(sourceFile, out);
        // ----- THEN ------------------------------------------------------------
        assertArrayEquals(payload, out.toByteArray(), "File slightly larger than the buffer should be transferred in two reads");
    }

    @Test
    void testTransferSecurityExceptionIsPropagatedAndFlushes() throws IOException {
        // ----- GIVEN -----------------------------------------------------------
        Path securePath = Files.createFile(tempDir.resolve("secure.txt"));
        Files.write(securePath, new byte[] { 1, 2, 3 });
        File secureFile = securePath.toFile();
        OutputStream outSpy = spy(new ByteArrayOutputStream());
        // Install a SecurityManager that denies read access for this specific file
        SecurityManager originalSM = System.getSecurityManager();
        SecurityManager denyingSM = new SecurityManager() {

            @Override
            public void checkRead(String file) {
                if (file.equals(secureFile.getAbsolutePath())) {
                    throw new SecurityException("Read access denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }
        };
        System.setSecurityManager(denyingSM);
        try {
            // ----- WHEN / THEN ----------------------------------------------------
            SecurityException se = assertThrows(SecurityException.class, () -> IOUtilities.transfer(secureFile, outSpy), "SecurityException should be propagated when read permission is denied");
            assertEquals("Read access denied by test SecurityManager", se.getMessage());
            // flush should still be attempted
            verify(outSpy, times(1)).flush();
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(originalSM);
        }
    }
}
