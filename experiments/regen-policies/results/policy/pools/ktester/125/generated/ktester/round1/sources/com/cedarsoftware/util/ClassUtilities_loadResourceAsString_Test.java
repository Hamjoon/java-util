package com.cedarsoftware.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_loadResourceAsString_Test {

    private static final String VALID_RESOURCE = "com/cedarsoftware/util/sample.txt";

    private static final String INVALID_RESOURCE = "nonexistent/resource.txt";

    /**
     * Simple holder for method‑source arguments.
     */
    private static final class Arguments extends java.util.ArrayList<Object> {

        private Arguments(Object... values) {
            for (Object v : values) add(v);
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }
        //         @SuppressWarnings("unchecked")
        //         <T> T get(int index) {
        //             return (T) super.get(index);
        //         }
    }

    /**
     * Supplies valid resource names together with the expected content (or null for binary data).
     */
    private static Stream<Arguments> validResources() {
        return Stream.of(Arguments.of("testdata/simple.txt", "Hello World"), Arguments.of("testdata/empty.txt", ""), Arguments.of("testdata/multibyte.txt", "こんにちは世界"), Arguments.of("testdata/large.txt", "abc".repeat(10_000)), Arguments.of("testdata/bom.txt", "\uFEFFBOM test"), Arguments.of("testdata/space in name.txt", "File name with spaces"), Arguments.of("testdata/linebreaks.txt", "line1\r\nline2\nline3\rline4"), Arguments.of("testdata/invalid_utf8.bin", null));
    }

    /**
     * Supplies invalid inputs together with the exception type we expect.
     */
    private static Stream<Arguments> errorResources() {
        return Stream.of(Arguments.of(null, NullPointerException.class), Arguments.of("nonexistent/resource.txt", IllegalArgumentException.class), Arguments.of("", IllegalArgumentException.class), Arguments.of("   ", IllegalArgumentException.class), Arguments.of(" testdata/simple.txt", IllegalArgumentException.class), Arguments.of("/testdata/simple.txt", IllegalArgumentException.class), Arguments.of("testdata/", IllegalArgumentException.class), Arguments.of("testdata/ioerror.txt", UncheckedIOException.class), Arguments.of("testdata/secure.txt", SecurityException.class));
    }

    @Test
    @DisplayName("loadResourceAsString returns the file content for a valid resource")
    void testLoadResourceAsString_ValidResource() {
        String result = ClassUtilities.loadResourceAsString(VALID_RESOURCE);
        assertNotNull(result, "Result should not be null for an existing resource");
        assertEquals("Hello World", result.trim(), "The content of the resource should match the expected string");
    }

    @Test
    @DisplayName("loadResourceAsString throws when the resource cannot be found")
    void testLoadResourceAsString_InvalidResource() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ClassUtilities.loadResourceAsString(INVALID_RESOURCE));
        assertTrue(thrown.getMessage().contains(INVALID_RESOURCE), "Exception message should contain the missing resource name");
    }

    @Test
    @DisplayName("loadResourceAsString and loadResourceAsBytes produce consistent results")
    void testLoadResourceAsString_ConsistencyWithBytes() {
        String resource = "testdata/simple.txt";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resource);
        String fromString = ClassUtilities.loadResourceAsString(resource);
        assertArrayEquals(bytes, fromString.getBytes(StandardCharsets.UTF_8), "String representation should be equivalent to the raw bytes interpreted as UTF‑8");
    }

    @ParameterizedTest(name = "[{index}] loadResourceAsString(\"{0}\") → expected content")
    @MethodSource("validResources")
    @DisplayName("loadResourceAsString returns correct content for valid resources")
    void testLoadResourceAsString_ValidResources(String resourceName, String expectedContent) {
        String result = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(result, "Result must never be null for an existing resource");
        if (expectedContent != null) {
            assertEquals(expectedContent, result, "Content of resource '" + resourceName + "' does not match expected");
        } else {
            // binary file with malformed UTF‑8 – expect the replacement character
            assertTrue(result.contains("\uFFFD"), "Result for '" + resourceName + "' should contain the Unicode replacement character");
        }
    }

    @ParameterizedTest(name = "[{index}] loadResourceAsString(\"{0}\") → throws {1}")
    @MethodSource("errorResources")
    @DisplayName("loadResourceAsString propagates expected exceptions for invalid inputs")
    void testLoadResourceAsString_ErrorScenarios(String resourceName, Class<? extends Throwable> expectedException) {
        if ("testdata/ioerror.txt".equals(resourceName) || "testdata/secure.txt".equals(resourceName)) {
            try (MockedStatic<ClassUtilities> mocked = mockStatic(ClassUtilities.class)) {
                mocked.when(() -> ClassUtilities.loadResourceAsBytes(resourceName)).thenThrow(expectedException);
                Throwable thrown = assertThrows(expectedException, () -> ClassUtilities.loadResourceAsString(resourceName));
                if (expectedException == UncheckedIOException.class) {
                    assertTrue(thrown.getCause() instanceof IOException, "UncheckedIOException should wrap an IOException");
                }
            }
        } else {
            assertThrows(expectedException, () -> ClassUtilities.loadResourceAsString(resourceName), "Expected " + expectedException.getSimpleName() + " for resource: " + resourceName);
        }
    }
}
