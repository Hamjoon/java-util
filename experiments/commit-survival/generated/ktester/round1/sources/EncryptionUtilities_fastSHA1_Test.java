package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA1_Test {

    /**
     * Temporary directory supplied by JUnit5
     */
    @TempDir
    Path tempDir;

    /**
     * File that will be created for the successful‑hash test
     */
    private File tempFile;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file for each test case
        tempFile = Files.createTempFile(tempDir, "fastSHA1Test", ".txt").toFile();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up the temporary file if it still exists
        if (tempFile != null && tempFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    @Test
    void fastSHA1_returnsCorrectHash_forExistingFile() throws IOException {
        // GIVEN – a file with known content
        byte[] content = "Hello EncryptionUtilities!".getBytes(StandardCharsets.UTF_8);
        Files.write(tempFile.toPath(), content);
        // AND – the expected SHA‑1 hash computed by the library itself
        String expectedHash = EncryptionUtilities.calculateSHA1Hash(content);
        // WHEN – fastSHA1 is invoked
        String actualHash = EncryptionUtilities.fastSHA1(tempFile);
        // THEN – the hash matches the expected value
        assertEquals(expectedHash, actualHash, "fastSHA1 should return the correct SHA‑1 hash of the file contents");
    }

    @Test
    void fastSHA1_returnsNull_whenFileCannotBeRead() {
        // GIVEN – a File object that points to a non‑existent file
        File nonExistent = new File(tempDir.toFile(), "doesNotExist.txt");
        assertFalse(nonExistent.exists(), "Test pre‑condition: file must not exist");
        // WHEN – fastSHA1 is invoked
        String result = EncryptionUtilities.fastSHA1(nonExistent);
        // THEN – the method returns null because an IOException is caught
        assertNull(result, "fastSHA1 should return null when an IOException occurs");
    }

    /* --------------------------------------------------------------
     *  Null argument – expect NullPointerException (not caught)
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_throwsNullPointerException_whenFileIsNull() {
        // GIVEN – a null reference
        File nullFile = null;
        // WHEN / THEN – the constructor of FileInputStream throws NPE
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA1(nullFile), "fastSHA1 should propagate NullPointerException for a null File argument");
    }

    /* --------------------------------------------------------------
     *  Non‑existent file – IOException is caught, method returns null
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_returnsNull_whenFileDoesNotExist() {
        // GIVEN – a File object that points to a non‑existent file
        File nonExistent = new File(tempDir.toFile(), "doesNotExist.txt");
        assertFalse(nonExistent.exists(), "Pre‑condition: file must not exist");
        // WHEN – fastSHA1 is invoked
        String result = EncryptionUtilities.fastSHA1(nonExistent);
        // THEN – null is returned
        assertNull(result, "fastSHA1 should return null when an IOException occurs");
    }

    /* --------------------------------------------------------------
     *  Directory argument – opening a FileInputStream on a directory
     *  throws FileNotFoundException which is caught → null
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_returnsNull_whenFileIsDirectory() throws IOException {
        // GIVEN – a temporary directory
        File directory = Files.createDirectory(tempDir.resolve("subdir")).toFile();
        assertTrue(directory.isDirectory(), "Pre‑condition: should be a directory");
        // WHEN – fastSHA1 is invoked
        String result = EncryptionUtilities.fastSHA1(directory);
        // THEN – null is returned
        assertNull(result, "fastSHA1 should return null when a directory is supplied");
    }

    /* --------------------------------------------------------------
     *  Unreadable file – OS denies read permission → IOException → null
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_returnsNull_whenFileIsUnreadable() throws IOException {
        // GIVEN – a readable file that we then make unreadable
        Files.write(tempFile.toPath(), "unreadable".getBytes(StandardCharsets.UTF_8));
        assertTrue(tempFile.setReadable(false, false), "Failed to remove read permission");
        // WHEN – fastSHA1 is invoked
        String result = EncryptionUtilities.fastSHA1(tempFile);
        // THEN – null is returned (FileInputStream cannot open the file)
        assertNull(result, "fastSHA1 should return null when the file cannot be read");
        // Restore permission for cleanup (important on CI agents)
        tempFile.setReadable(true, false);
    }

    /* --------------------------------------------------------------
     *  Read error during hashing – mock calculateFileHash to throw IOException
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_returnsNull_whenCalculateFileHashThrowsIOException() throws IOException {
        // GIVEN – a normal file with some content
        Files.write(tempFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));
        // Mock the static method calculateFileHash to throw IOException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.calculateFileHash(any(), any(MessageDigest.class))).thenThrow(new IOException("Simulated read error"));
            // WHEN – fastSHA1 is invoked
            String result = EncryptionUtilities.fastSHA1(tempFile);
            // THEN – the method catches the IOException and returns null
            assertNull(result, "fastSHA1 should return null when calculateFileHash throws IOException");
        }
    }

    /* --------------------------------------------------------------
     *  SecurityManager restriction – SecurityException propagates
     * -------------------------------------------------------------- */
    @Test
    void fastSHA1_propagatesSecurityException_whenSecurityManagerDeniesRead() throws IOException {
        // GIVEN – a file that exists
        Files.write(tempFile.toPath(), "secure".getBytes(StandardCharsets.UTF_8));
        // Install a SecurityManager that denies read access to the temp file
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkRead(String file) {
                if (file != null && new File(file).equals(tempFile)) {
                    throw new SecurityException("Read access denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // allow everything else
            }
        });
        try {
            // WHEN / THEN – fastSHA1 should propagate the SecurityException
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA1(tempFile), "fastSHA1 should propagate SecurityException from the SecurityManager");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
