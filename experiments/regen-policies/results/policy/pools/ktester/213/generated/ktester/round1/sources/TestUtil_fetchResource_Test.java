package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_fetchResource_Test {

    /**
     * Name of the resource used for the successful fetch test.
     */
    private static final String SAMPLE_RESOURCE = "sample.txt";

    /**
     * Expected content of {@code sample.txt}.
     */
    private static final String SAMPLE_CONTENT = "Hello, TestUtil!";

    /**
     * Expected content of {@code empty.txt}.
     */
    private static final String EMPTY_CONTENT = "";

    /**
     * Expected content of {@code configs/app.properties}.
     */
    private static final String APP_PROPERTIES_CONTENT = "app.name=TestApp\napp.version=1.0";

    @BeforeEach
    void setupBeforeEach() {
        // Verify that the test resource is actually available on the classpath.
        // If it is missing the test will fail with a clear message.
        URL url = TestUtil.class.getResource("/" + SAMPLE_RESOURCE);
        assertNotNull(url, "Test resource '" + SAMPLE_RESOURCE + "' must be present on the classpath");
        // Ensure that the resources referenced in the successful test cases exist.
        assertNotNull(TestUtil.class.getResource("/" + SAMPLE_RESOURCE), "Resource '" + SAMPLE_RESOURCE + "' must be on the classpath");
        assertNotNull(TestUtil.class.getResource("/empty.txt"), "Resource 'empty.txt' must be on the classpath");
        assertNotNull(TestUtil.class.getResource("/configs/app.properties"), "Resource 'configs/app.properties' must be on the classpath");
    }

    /**
     * Supplies resource names and their expected contents for the success group.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideSuccessfulResources() {
        return Stream.of(// sample.txt – normal non‑empty file
        org.junit.jupiter.params.provider.Arguments.of("sample.txt", SAMPLE_CONTENT), // empty.txt – empty file should yield empty string
        org.junit.jupiter.params.provider.Arguments.of("empty.txt", EMPTY_CONTENT), // configs/app.properties – file located in a sub‑directory
        org.junit.jupiter.params.provider.Arguments.of("configs/app.properties", APP_PROPERTIES_CONTENT));
    }

    /**
     * Supplies the names that trigger error handling together with a short description.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideErrorScenarios() {
        return Stream.of(// non‑existent resource
        org.junit.jupiter.params.provider.Arguments.of("nonexistent.txt", "Resource does not exist on the classpath"), // null name – getResource will throw NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(null, "Null resource name"), // leading slash – method adds another slash resulting in '//sample.txt'
        org.junit.jupiter.params.provider.Arguments.of("/sample.txt", "Name starts with a leading slash, causing double slash"), // directory path – attempting to read a directory as a file
        org.junit.jupiter.params.provider.Arguments.of("configs", "Name points to a directory rather than a file"), // JAR resource – e.g., a class file from the JDK
        org.junit.jupiter.params.provider.Arguments.of("java/lang/String.class", "Resource located inside a JAR/module, unsupported by Paths.get(URI)"));
    }

    @Test
    void testFetchResourceSuccess() {
        // given – the name of an existing resource (handled in @BeforeEach)
        // when
        String result = TestUtil.fetchResource(SAMPLE_RESOURCE);
        // then
        assertEquals(SAMPLE_CONTENT, result, "fetchResource should return the exact contents of the resource");
    }

    @Test
    void testFetchResourceNotFound() {
        // given
        String missingResource = "non‑existent‑resource.txt";
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(missingResource), "fetchResource should wrap missing‑resource errors in a RuntimeException");
        // The cause should be a NullPointerException or a URISyntaxException depending on the JDK.
        assertNotNull(thrown.getCause(), "The RuntimeException should have a non‑null cause indicating the underlying problem");
    }

    /**
     * Parameterized test for cases that must result in a {@link RuntimeException}.
     *
     * @param name        the resource name passed to {@code fetchResource}
     * @param description description of the scenario (used only for readability)
     */
    @ParameterizedTest(name = "[{index}] fetchResource_error: {1}")
    @MethodSource("provideErrorScenarios")
    void testFetchResourceError(String name, String description) {
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(name), () -> "Expected RuntimeException for scenario: " + description);
        // The underlying cause should not be null – it indicates why the operation failed.
        assertNotNull(thrown.getCause(), "RuntimeException must wrap the original exception for: " + description);
    }

    /* -------------------------------------------------------------
     *  Additional explicit test for null name (covers JSON group explicitly)
     * ------------------------------------------------------------- */
    @Test
    void testFetchResourceNullNameExplicit() {
        // given
        String nullName = null;
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> TestUtil.fetchResource(nullName), "fetchResource should wrap NullPointerException when name is null");
        assertNotNull(thrown.getCause(), "Cause of RuntimeException should be the original NullPointerException");
    }
}
