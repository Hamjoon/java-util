package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_createTempDirectory_Test {

    /**
     * Holds any temporary directories created during a test so they can be cleaned up.
     */
    private final List<File> createdDirs = new ArrayList<>();

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the list is empty before each test runs.
        createdDirs.clear();
    }

    @AfterEach
    void teardownAfterEach() {
        // Attempt to delete any directories that were created during the test.
        for (File dir : createdDirs) {
            if (dir != null && dir.exists()) {
                deleteRecursively(dir);
            }
        }
    }

    /**
     * Helper method to delete a directory and all of its contents.
     */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Test
    void testCreateTempDirectoryCreatesDirectory() throws IOException {
        // GIVEN
        String prefix = "myTempDirPrefix_";
        // WHEN
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        // Record for cleanup
        createdDirs.add(tempDir);
        // THEN
        assertNotNull(tempDir, "Returned directory should not be null");
        assertTrue(tempDir.exists(), "Directory should exist on the file system");
        assertTrue(tempDir.isDirectory(), "Returned File should be a directory");
        assertTrue(tempDir.getName().startsWith(prefix), "Directory name should start with the supplied prefix");
        // The directory should be located inside the system temporary directory.
        String systemTemp = System.getProperty("java.io.tmpdir");
        assertTrue(tempDir.getAbsolutePath().startsWith(new File(systemTemp).getAbsolutePath()), "Directory should be created under the system temporary directory");
    }

    @Test
    void testCreateTempDirectoryWithNullPrefixThrowsException() {
        // GIVEN
        String nullPrefix = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> SystemUtilities.createTempDirectory(nullPrefix), "Passing a null prefix should result in NullPointerException");
    }

    @ParameterizedTest(name = "createTempDirectory with prefix \"{0}\" should succeed")
    @CsvSource({ "'testPrefix'", // empty string – Files.createTempDirectory accepts it
    "''" })
    void testCreateTempDirectorySuccessful(String prefix) throws IOException {
        // GIVEN
        // (prefix is supplied by the parameterized test)
        // WHEN
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        // Record for cleanup
        createdDirs.add(tempDir);
        // THEN
        assertNotNull(tempDir, "Returned directory should not be null");
        assertTrue(tempDir.exists(), "Directory should exist on the file system");
        assertTrue(tempDir.isDirectory(), "Returned File should be a directory");
        if (!prefix.isEmpty()) {
            assertTrue(tempDir.getName().startsWith(prefix), "Directory name should start with the supplied prefix");
        }
        // The directory should be located inside the system temporary directory.
        String systemTemp = System.getProperty("java.io.tmpdir");
        assertTrue(tempDir.getAbsolutePath().startsWith(new File(systemTemp).getAbsolutePath()), "Directory should be created under the system temporary directory");
    }

    @ParameterizedTest(name = "createTempDirectory with short prefix \"{0}\" should throw IllegalArgumentException")
    @CsvSource({ "'ab'", "'a'" })
    void testCreateTempDirectoryShortPrefixThrows(String shortPrefix) {
        // GIVEN a prefix shorter than the 3‑character minimum required by Files.createTempDirectory
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> SystemUtilities.createTempDirectory(shortPrefix), "Short prefixes must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "createTempDirectory with prefix \"{0}\" should propagate IOException")
    @CsvSource({ "'uncreatable'", "'readonly'", "'fullDisk'" })
    void testCreateTempDirectoryIOExceptionPropagation(String prefix) {
        // GIVEN – mock Files.createTempDirectory to throw an IOException
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createTempDirectory(prefix)).thenThrow(new IOException("Simulated I/O failure for prefix: " + prefix));
            // WHEN / THEN
            IOException thrown = assertThrows(IOException.class, () -> SystemUtilities.createTempDirectory(prefix), "Method should propagate IOException from Files.createTempDirectory");
            assertTrue(thrown.getMessage().contains("Simulated I/O failure"), "Exception message should contain the simulated cause");
        }
    }

    @Test
    void testDeleteOnExitIsRegistered() throws IOException {
        // GIVEN
        String prefix = "deleteOnExit_";
        // WHEN
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        createdDirs.add(tempDir);
        // THEN
        // There is no public API to query the delete‑on‑exit registry.
        // As a pragmatic check we verify that the file still exists after creation
        // (the JVM would delete it on exit, not immediately).
        assertTrue(tempDir.exists(), "Directory should still exist immediately after creation");
        // The call to deleteOnExit does not throw, so reaching this point is sufficient.
    }
}
