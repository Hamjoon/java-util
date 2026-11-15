package com.cedarsoftware.util;

import java.io.UncheckedIOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClassUtilities#loadResourceAsString(String)}.
 */
@ExtendWith(MockitoExtension.class)
class ClassUtilities_loadResourceAsString_Test {

    /**
     * Provides resource names for the parameterized test.
     * <p>
     * The {@code test-resource.txt} file should be placed under {@code src/test/resources}
     * and contain some non‑empty text. The {@code empty.txt} file should also exist
     * (it may be empty). Any other name is considered missing.
     * </p>
     *
     * @return a stream of resource names
     */
    static Stream<String> resourceNames() {
        return Stream.of("test-resource.txt", "nonexistent.txt");
    }

    @ParameterizedTest(name = "loadResourceAsString(\"{0}\")")
    @MethodSource("resourceNames")
    @DisplayName("loadResourceAsString should return content for existing resources and throw for missing ones")
    void testLoadResourceAsString(String resourceName) {
        if ("test-resource.txt".equals(resourceName)) {
            // Existing resource – should return its content.
            String content = ClassUtilities.loadResourceAsString(resourceName);
            assertNotNull(content, "Content should not be null for an existing resource");
            assertFalse(content.isEmpty(), "Content should not be empty for test-resource.txt");
        } else {
            // Missing resource – should throw an UncheckedIOException.
            assertThrows(UncheckedIOException.class,
                    () -> ClassUtilities.loadResourceAsString(resourceName),
                    "Missing resource should cause UncheckedIOException");
        }
    }

    @Test
    @DisplayName("loadResourceAsString returns empty string for an empty resource")
    void testLoadEmptyResource() {
        // The file empty.txt must exist in src/test/resources and be empty.
        String content = ClassUtilities.loadResourceAsString("empty.txt");
        assertNotNull(content, "Content should not be null even for an empty file");
        assertEquals("", content, "Content of an empty file should be an empty string");
    }
}