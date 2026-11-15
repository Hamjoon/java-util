package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class TestUtil_fetchResource_Test {

    /**
     * Name of the temporary resource file placed on the test classpath
     */
    private static final String TEST_RESOURCE_NAME = "fetchResourceTest.txt";

    /**
     * Expected content of the temporary resource file
     */
    private static final String TEST_RESOURCE_CONTENT = "Hello, TestUtil fetchResource!";

    /**
     * Path to the temporary file that will be created in the test‑resources directory
     */
    private static Path tempResourcePath;

    /**
     * Directory on the classpath that holds test resources (usually src/test/resources).
     */
    private static Path resourcesDir;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Locate the test‑resources directory on the classpath.
        URL resourcesUrl = TestUtil_fetchResource_Test.class.getClassLoader().getResource("");
        assertNotNull(resourcesUrl, "Test resources directory could not be located");
        resourcesDir = Path.of(resourcesUrl.getPath());
        // ---- Successful resources -------------------------------------------------
        writeFile("sample.txt", "Hello World".getBytes());
        writeFile("empty.txt", new byte[0]);
        writeFile("spaced name.txt", "Space Content".getBytes());
        writeFile("ünïçødé.txt", "Unicode ✓".getBytes());
        // binary image (just some bytes, not a real PNG)
        writeFile("image.png", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
        // large text file (> 1 MiB)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200_000; i++) {
            // ~1.2 MiB of 'A's
            sb.append('A');
        }
        writeFile("large.txt", sb.toString().getBytes());
        // unreadable file – will be made unreadable after creation
        writeFile("unreadable.txt", "cannot read".getBytes());
        // malformed URI resource – name contains a pipe character which is illegal in a URI
        writeFile("invalid|uri.txt", "bad uri".getBytes());
        // ---- Make unreadable -------------------------------------------------------
        Path unreadable = resourcesDir.resolve("unreadable.txt");
        try {
            // POSIX file permissions are used when the underlying OS supports them.
            // If not supported (e.g., Windows), the test will be skipped.
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("---------");
            Files.setPosixFilePermissions(unreadable, perms);
        } catch (UnsupportedOperationException | IOException e) {
            // Skip unreadable‑file test on non‑POSIX systems.
        }
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Clean up all files we created.
        Files.deleteIfExists(resourcesDir.resolve("sample.txt"));
        Files.deleteIfExists(resourcesDir.resolve("empty.txt"));
        Files.deleteIfExists(resourcesDir.resolve("spaced name.txt"));
        Files.deleteIfExists(resourcesDir.resolve("ünïçødé.txt"));
        Files.deleteIfExists(resourcesDir.resolve("image.png"));
        Files.deleteIfExists(resourcesDir.resolve("large.txt"));
        Files.deleteIfExists(resourcesDir.resolve("unreadable.txt"));
        Files.deleteIfExists(resourcesDir.resolve("invalid|uri.txt"));
    }

    /**
     * Helper to write a file with the given content (creates parent directories if needed).
     */
    private static void writeFile(String name, byte[] content) throws IOException {
        Path p = resourcesDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Stream<Arguments> successfulResourcesProvider() {
        return Stream.of(Arguments.of("sample.txt", "Hello World"), Arguments.of("empty.txt", ""), Arguments.of("spaced name.txt", "Space Content"), Arguments.of("ünïçødé.txt", "Unicode ✓"), // For binary we only assert that the result is non‑empty.
        Arguments.of("image.png", "non-empty"), // Large file – we compare the exact content generated in @BeforeAll.
        Arguments.of("large.txt", "large-content"));
    }

    @Test
    void fetchResource_returnsFileContent() {
        // given – the resource name (without leading slash, as fetchResource adds it)
        String resourceName = TEST_RESOURCE_NAME;
        // when
        String result = TestUtil.fetchResource(resourceName);
        // then
        assertEquals(TEST_RESOURCE_CONTENT, result, "fetchResource should return the exact file content");
    }

    @Test
    void fetchResource_throwsRuntimeExceptionWhenResourceMissing() {
        // given – a name that does not exist on the classpath
        String missingResource = "non‑existent‑resource.txt";
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(missingResource), "fetchResource should wrap missing‑resource errors in a RuntimeException");
        // The cause should be a NoSuchFileException or similar; we only assert that a cause exists.
        assertNotNull(ex.getCause(), "The RuntimeException should have a cause");
    }

    @ParameterizedTest(name = "[{index}] fetchResource(''{0}'')")
    @MethodSource("successfulResourcesProvider")
    void fetchResource_successful(String resourceName, String expected) throws IOException {
        String result = TestUtil.fetchResource(resourceName);
        if ("non-empty".equals(expected)) {
            // Binary file – just ensure something was returned.
            assertFalse(result.isEmpty(), "Binary resource should yield a non‑empty string");
        } else if ("large-content".equals(expected)) {
            // Re‑create the expected large string.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 200_000; i++) {
                sb.append('A');
            }
            assertEquals(sb.toString(), result, "Large file content should be read completely");
        } else {
            assertEquals(expected, result, "Content of '" + resourceName + "' does not match");
        }
    }

    @Test
    void fetchResource_missingResource_throwsRuntimeException() {
        String missing = "nonexistent.txt";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(missing), "Missing resource should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }

    @Test
    void fetchResource_nullName_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(null), "Null name should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }

    @Test
    void fetchResource_emptyString_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(""), "Empty string name should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }

    @Test
    void fetchResource_nameWithLeadingSlash_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource("/sample.txt"), "Name starting with '/' should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }

    @Test
    void fetchResource_unreadableFile_throwsRuntimeException() throws IOException {
        Path unreadable = resourcesDir.resolve("unreadable.txt");
        // If the OS does not support POSIX permissions, skip this test.
        assumeTrue(Files.isReadable(unreadable) == false, "Skipping unreadable‑file test on non‑POSIX filesystem");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource("unreadable.txt"), "Unreadable file should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }

    @Test
    void fetchResource_invalidUri_throwsRuntimeException() {
        // The file name contains a pipe character which makes URL.toURI() throw URISyntaxException.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource("invalid|uri.txt"), "Resource with malformed URI should result in RuntimeException");
        assertNotNull(ex.getCause(), "Cause should be present");
    }
}
