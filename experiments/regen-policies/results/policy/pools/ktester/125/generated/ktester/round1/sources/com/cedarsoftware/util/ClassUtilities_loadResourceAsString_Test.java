package com.cedarsoftware.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.junit.*;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;

// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ClassUtilities_loadResourceAsString_Test {

    /**
     * Simple holder for a resource name and the expected content.
     */
    private static final class TestData {

        final String name;

        final String expected;

        TestData(String name, String expected) {
            this.name = name;
            this.expected = expected;
        }
    }

    /**
     * Verify that a known class‑path resource can be loaded as a UTF‑8 string.
     * The JAR manifest is guaranteed to be present on the test class‑path.
     */
    @Test
    @DisplayName("loadResourceAsString() returns content of an existing resource")
    void testLoadExistingResource() throws IOException {
        final String resourceName = "META-INF/MANIFEST.MF";
        // Load via the method under test.
        String result = ClassUtilities.loadResourceAsString(resourceName);
        // Load the same resource manually for an independent verification.
        String expected;
        try (var is = ClassUtilities.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Manual lookup of resource should succeed");
            byte[] bytes = is.readAllBytes();
            expected = new String(bytes, StandardCharsets.UTF_8);
        }
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals(expected, result, "String loaded by ClassUtilities must match manual loading");
        assertTrue(result.contains("Manifest-Version"), "Loaded manifest should contain the 'Manifest-Version' attribute");
    }

    /**
     * Verify that attempting to load a non‑existent resource results in an
     * {@link UncheckedIOException} (or {@link IllegalArgumentException}).
     */
    @Test
    @DisplayName("loadResourceAsString() throws UncheckedIOException for missing resource")
    void testLoadMissingResource() {
        final String missingResource = "this/resource/does/not/exist.txt";
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ClassUtilities.loadResourceAsString(missingResource));
        // Accept either IllegalArgumentException (fast‑fail) or UncheckedIOException wrapping an IOException.
        boolean acceptable = thrown instanceof IllegalArgumentException || (thrown instanceof UncheckedIOException && thrown.getCause() instanceof IOException);
        assertTrue(acceptable, "Exception should be IllegalArgumentException or UncheckedIOException wrapping IOException");
    }

    /**
     * Verify that passing {@code null} as the resource name results in a
     * {@link NullPointerException}. This mirrors the behaviour of the underlying
     * {@code ClassLoader#getResourceAsStream} call.
     */
    @Test
    @DisplayName("loadResourceAsString() throws NullPointerException for null argument")
    void testNullArgument() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsString(null));
    }

    /**
     * Verify that an empty string (which does not resolve to a valid resource)
     * results in an {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("loadResourceAsString() throws IllegalArgumentException for empty string")
    void testEmptyStringArgument() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsString(""));
    }
}
