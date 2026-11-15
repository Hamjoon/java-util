package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_fetchResource_Test {

    /**
     * Path to the temporary resource created for the successful‑fetch test.
     */
    private static Path tempResourcePath;

    /**
     * Name of the temporary resource (used when calling {@code fetchResource}).
     */
    private static final String TEMP_RESOURCE_NAME = "tempResource.txt";

    /**
     * Content written to the temporary resource.
     */
    private static final String TEMP_RESOURCE_CONTENT = "Hello, TestUtil!";

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Locate the root of the class‑path (the directory that backs TestUtil.class.getResource("/"))
        URL rootUrl = TestUtil.class.getResource("/");
        assertNotNull(rootUrl, "Unable to locate class‑path root for TestUtil");
        Path rootPath = Paths.get(rootUrl.toURI());
        // Create a temporary file inside that root so that TestUtil.getResource can find it.
        tempResourcePath = rootPath.resolve(TEMP_RESOURCE_NAME);
        Files.writeString(tempResourcePath, TEMP_RESOURCE_CONTENT);
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Clean up the temporary file if it was created.
        if (tempResourcePath != null && Files.exists(tempResourcePath)) {
            Files.delete(tempResourcePath);
        }
    }

    /**
     * Parameter source for successful‑read scenarios.
     *
     * Each {@code Arguments} entry contains:
     * <ul>
     *   <li>resource name</li>
     *   <li>expected file content</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successfulReadProvider() {
        return Stream.of(// JSON case: name = "test.txt", expected = "Hello World"
        org.junit.jupiter.params.provider.Arguments.of("test.txt", "Hello World"));
    }

    /**
     * Parameter source for null‑argument scenarios.
     *
     * The expected result is the type of exception we anticipate.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> nullNameProvider() {
        return Stream.of(// JSON case: name = null, expected = RuntimeException
        org.junit.jupiter.params.provider.Arguments.of((String) null, RuntimeException.class));
    }

    @Test
    void fetchResource_returnsFileContent() {
        // GIVEN – the temporary resource has been created in @BeforeAll
        // WHEN
        String result = TestUtil.fetchResource(TEMP_RESOURCE_NAME);
        // THEN
        assertEquals(TEMP_RESOURCE_CONTENT, result, "fetchResource should return the exact contents of the resource file");
    }

    @Test
    void fetchResource_throwsRuntimeExceptionWhenResourceMissing() {
        // GIVEN – a name that does not correspond to any resource on the class‑path
        String missingName = "does-not-exist-12345.txt";
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(missingName), "fetchResource should wrap missing‑resource errors in a RuntimeException");
        // The cause should be either a NullPointerException (url == null) or an URISyntaxException.
        assertNotNull(ex.getCause(), "The RuntimeException should have a non‑null cause indicating the underlying problem");
    }

    @ParameterizedTest(name = "[{index}] fetchResource(''{0}'') returns expected content")
    @MethodSource("successfulReadProvider")
    void fetchResource_successfulRead(String resourceName, String expectedContent) throws Exception {
        // GIVEN – create the resource file in the same class‑path root used by TestUtil
        URL rootUrl = TestUtil.class.getResource("/");
        assertNotNull(rootUrl, "Unable to locate class‑path root for TestUtil");
        Path rootPath = Paths.get(rootUrl.toURI());
        Path resourcePath = rootPath.resolve(resourceName);
        Files.writeString(resourcePath, expectedContent);
        try {
            // WHEN
            String actual = TestUtil.fetchResource(resourceName);
            // THEN
            assertEquals(expectedContent, actual, () -> "fetchResource should return the exact contents of '" + resourceName + "'");
        } finally {
            // Clean up the ad‑hoc resource regardless of test outcome.
            Files.deleteIfExists(resourcePath);
        }
    }

    @ParameterizedTest(name = "[{index}] fetchResource(null) throws {1}")
    @MethodSource("nullNameProvider")
    void fetchResource_nullName(String name, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> TestUtil.fetchResource(name));
        // The underlying cause should be a NullPointerException (from getResource) wrapped inside.
        assertNotNull(thrown.getCause(), "The thrown exception should wrap the original cause (e.g., NullPointerException)");
    }
}
