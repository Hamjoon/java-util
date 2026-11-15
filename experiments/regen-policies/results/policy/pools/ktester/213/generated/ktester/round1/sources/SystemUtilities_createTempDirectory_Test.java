package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_createTempDirectory_Test {

    /**
     * Holds the directory created by a test so it can be cleaned up afterwards.
     */
    private File createdTempDir;

    /**
     * Original value of {@code java.io.tmpdir} – restored after each test that changes it.
     */
    private static String originalTmpDir;

    /**
     * Original security manager – restored after the security‑manager test.
     */
    private static SecurityManager originalSecurityManager;

    @BeforeAll
    static void setupBeforeAll() {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        originalSecurityManager = System.getSecurityManager();
    }

    @BeforeEach
    void setupBeforeEach() {
        // reset before each test
        createdTempDir = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // The method under test registers deleteOnExit(), but we also delete it now
        // to keep the file‑system clean when the test suite runs repeatedly.
        if (createdTempDir != null && createdTempDir.exists()) {
            // Recursively delete the directory (it should be empty)
            try {
                Files.deleteIfExists(createdTempDir.toPath());
            } catch (IOException ignored) {
                // If deletion fails we rely on deleteOnExit() as a fallback.
            }
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore any global changes made by the tests.
        System.setProperty("java.io.tmpdir", originalTmpDir);
        System.setSecurityManager(originalSecurityManager);
    }

    private static Stream<Arguments> normalPrefixProvider() {
        return Stream.of(// empty string prefix – Files.createTempDirectory treats it as "" and still creates a dir
        Arguments.of("", "tmp"), // Unicode characters
        Arguments.of("spécial_ß_!@#", "spécial_ß_!@#"), // Leading whitespace
        Arguments.of("   leadingSpace_", "   leadingSpace_"), // Single dot prefix – should create a hidden dir on *nix
        Arguments.of(".", "."), // Long but acceptable prefix (≈240 chars)
        Arguments.of("maxLen_" + "x".repeat(240), "maxLen_"));
    }

    @Test
    void testCreateTempDirectory_WithValidPrefix() throws IOException {
        // GIVEN
        String prefix = "testPrefix_";
        // WHEN
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        // remember for cleanup
        createdTempDir = tempDir;
        // THEN
        assertNotNull(tempDir, "Returned File must not be null");
        assertTrue(tempDir.isDirectory(), "Returned File must be a directory");
        assertTrue(tempDir.getName().startsWith(prefix), () -> "Directory name should start with the supplied prefix. Expected prefix: " + prefix);
        // Verify that the directory resides in the system temporary directory
        String systemTmp = System.getProperty("java.io.tmpdir");
        assertTrue(tempDir.getAbsolutePath().startsWith(systemTmp), () -> "Directory should be created under the system temp directory: " + systemTmp);
        // The file should be scheduled for deletion on JVM exit
        // (We cannot directly assert the flag, but the call to deleteOnExit() should not throw)
        assertDoesNotThrow(tempDir::deleteOnExit);
    }

    @Test
    void testCreateTempDirectory_WithNullPrefix() {
        // GIVEN
        String prefix = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> SystemUtilities.createTempDirectory(prefix), "Passing a null prefix should result in NullPointerException");
    }

    @Test
    void testCreateTempDirectory_UniqueDirectories() throws IOException {
        // GIVEN
        String prefix = "uniqueTest_";
        // WHEN
        File first = SystemUtilities.createTempDirectory(prefix);
        File second = SystemUtilities.createTempDirectory(prefix);
        // remember at least one for cleanup; the other will be cleaned by deleteOnExit
        createdTempDir = first;
        // THEN
        assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath(), "Two calls with the same prefix must produce distinct directories");
        assertTrue(first.isDirectory() && second.isDirectory(), "Both returned Files must be directories");
    }

    @ParameterizedTest(name = "prefix=\"{0}\"")
    @MethodSource("normalPrefixProvider")
    void testCreateTempDirectory_NormalPrefixes(String prefix, String expectedStart) throws IOException {
        // WHEN
        File dir = SystemUtilities.createTempDirectory(prefix);
        // remember for cleanup
        createdTempDir = dir;
        // THEN
        assertNotNull(dir);
        assertTrue(dir.isDirectory(), "Should be a directory");
        assertTrue(dir.getName().startsWith(expectedStart), () -> "Directory name should start with expected prefix '" + expectedStart + "'");
        // Verify location under system temp dir
        String sysTmp = System.getProperty("java.io.tmpdir");
        assertTrue(dir.getAbsolutePath().startsWith(sysTmp), () -> "Directory should be under the system temp dir");
    }

    @Test
    void testCreateTempDirectory_TooLongPrefix_ThrowsIOException() {
        // Build a prefix that is deliberately longer than typical filesystem limits.
        String longPrefix = "veryLongPrefix_" + "x".repeat(200);
        // WHEN / THEN
        assertThrows(IOException.class, () -> SystemUtilities.createTempDirectory(longPrefix), "Extremely long prefix should cause IOException");
    }

    @Test
    void testCreateTempDirectory_PrefixWithSeparator_ThrowsIOException() {
        String prefix = "subdir" + File.separator + "inner_";
        // The JDK throws InvalidPathException wrapped in IOException
        assertThrows(IOException.class, () -> SystemUtilities.createTempDirectory(prefix), "Prefix containing a file separator should cause IOException/InvalidPathException");
    }

    @Test
    void testCreateTempDirectory_NullCharacterPrefix_ThrowsInvalidPathException() {
        String prefix = "\u0000invalid";
        // Files.createTempDirectory throws InvalidPathException (unchecked) wrapped in IOException
        assertThrows(InvalidPathException.class, () -> SystemUtilities.createTempDirectory(prefix), "Prefix containing a null character should cause InvalidPathException");
    }

    @Test
    void testCreateTempDirectory_WithCustomTmpDirProperty() throws IOException {
        // Create a temporary directory that will act as the new tmpdir
        Path customTmp = Files.createTempDirectory("customTmpRoot");
        try {
            System.setProperty("java.io.tmpdir", customTmp.toString());
            String prefix = "customTmp_";
            File dir = SystemUtilities.createTempDirectory(prefix);
            // remember for cleanup
            createdTempDir = dir;
            assertTrue(dir.getAbsolutePath().startsWith(customTmp.toString()), "Directory should be created inside the overridden java.io.tmpdir");
            assertTrue(dir.getName().startsWith(prefix));
        } finally {
            // Cleanup the custom tmp root (the directory created by the method under test
            // will be deleted by its own deleteOnExit or by @AfterEach)
            Files.walk(customTmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            System.setProperty("java.io.tmpdir", originalTmpDir);
        }
    }

    @Test
    void testCreateTempDirectory_WithSecurityManagerThrowing() {
        // Install a SecurityManager that forbids write access to the temp directory.
        SecurityManager restrictiveSM = new SecurityManager() {

            @Override
            public void checkWrite(String file) {
                throw new SecurityException("Write access denied by test SecurityManager");
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }
        };
        System.setSecurityManager(restrictiveSM);
        try {
            assertThrows(SecurityException.class, () -> SystemUtilities.createTempDirectory("anyPrefix"), "SecurityException should propagate when write is denied");
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    @Test
    void testCreateTempDirectory_NoWritePermission_ThrowsIOException() throws IOException {
        // Skip on Windows where POSIX permissions are not supported.
        Assumptions.assumeTrue(!System.getProperty("os.name").toLowerCase().contains("win"), "POSIX permission test is not applicable on Windows");
        // Create a writable temporary directory and then remove write permission.
        Path noWriteDir = Files.createTempDirectory("noWriteTmp");
        try {
            // Remove write permission for owner, group, others.
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("r-xr-xr-x");
            Files.setPosixFilePermissions(noWriteDir, perms);
            System.setProperty("java.io.tmpdir", noWriteDir.toString());
            // The creation should now fail with an AccessDeniedException (subclass of IOException)
            assertThrows(IOException.class, () -> SystemUtilities.createTempDirectory("noWritePermission"), "Creating a temp directory in a non‑writable location should throw IOException");
        } finally {
            // Restore permissions so we can delete the directory
            Files.setPosixFilePermissions(noWriteDir, PosixFilePermissions.fromString("rwxrwxrwx"));
            Files.walk(noWriteDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            System.setProperty("java.io.tmpdir", originalTmpDir);
        }
    }
}
