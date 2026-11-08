package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.UncheckedIOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_loadResourceAsString_Test {

    /* --------------------------------------------------------------------- *
     * Helper holder class – makes the method‑source signatures tidy          *
     * --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final String resourceName;

        // used for successful loads
        final String expected;

        // used for error cases
        final Class<? extends Throwable> exceptionClass;

        final String description;

        ArgumentsHolder(String resourceName, String expected, String description) {
            this.resourceName = resourceName;
            this.expected = expected;
            this.exceptionClass = null;
            this.description = description;
        }

        ArgumentsHolder(String resourceName, Class<? extends Throwable> exceptionClass, String description) {
            this.resourceName = resourceName;
            this.expected = null;
            this.exceptionClass = exceptionClass;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Successful loads **
     */
    private static Stream<ArgumentsHolder> successfulLoadProvider() {
        return Stream.of(new ArgumentsHolder("/testfiles/simple.txt", "Hello World", "Loads a regular UTF‑8 text file that exists on the classpath and returns its exact content as a String."), new ArgumentsHolder("/testfiles/empty.txt", "", "Loads an existing resource that is empty; the method should return an empty String, not null."), new ArgumentsHolder("/testfiles/multiline.txt", "Line1\nLine2\r\nLine3", "Loads a multi‑line UTF‑8 file and verifies that line‑break characters are preserved in the returned String."));
    }

    /**
     * Error handling **
     */
    private static Stream<ArgumentsHolder> errorHandlingProvider() {
        return Stream.of(new ArgumentsHolder(null, NullPointerException.class, "Passing a null resource name should result in a NullPointerException."), new ArgumentsHolder("", IllegalArgumentException.class, "An empty string is not a valid resource identifier; the method is expected to throw IllegalArgumentException."), new ArgumentsHolder("/testfiles/missing.txt", UncheckedIOException.class, "When the resource cannot be found on the classpath, loadResourceAsBytes throws an UncheckedIOException, which should propagate."));
    }

    /**
     * Positive test – load a known resource that is guaranteed to be on the class‑path.
     * The {@code ClassUtilities} class itself is compiled into a {@code .class} file and
     * placed on the class‑path, so we can request it via its fully‑qualified resource name.
     */
    @Test
    @DisplayName("loadResourceAsString should return non‑empty string for an existing resource")
    void testLoadExistingResource() {
        // The resource name uses '/' as package separators and ends with ".class".
        String resourceName = "com/cedarsoftware/util/ClassUtilities.class";
        String result = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(result, "Resulting string must not be null");
        assertFalse(result.isEmpty(), "Resulting string must not be empty");
        // The class file contains the word "class" in its constant pool; checking for that
        // gives us a cheap sanity‑check without relying on exact byte‑to‑string conversion.
        assertTrue(result.contains("class") || result.contains("ClassUtilities"), "Result should contain recognizable text from the class file");
    }

    /**
     * Negative test – request a resource that does not exist.
     * The implementation of {@code loadResourceAsBytes} is expected to wrap any
     * {@link java.io.IOException} in an {@link UncheckedIOException}.
     */
    @Test
    @DisplayName("loadResourceAsString should throw UncheckedIOException for missing resource")
    void testLoadMissingResourceThrows() {
        String missingResource = "this/resource/does/not/exist.txt";
        assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsString(missingResource), "Missing resources should result in UncheckedIOException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulLoadProvider")
    void testSuccessfulLoads(ArgumentsHolder holder) {
        String actual = ClassUtilities.loadResourceAsString(holder.resourceName);
        assertEquals(holder.expected, actual, () -> "Content of resource '" + holder.resourceName + "' does not match expectation");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorHandlingProvider")
    void testErrorHandling(ArgumentsHolder holder) {
        assertThrows(holder.exceptionClass, () -> ClassUtilities.loadResourceAsString(holder.resourceName), () -> "Expected exception for resource '" + holder.resourceName + "'");
    }

    /**
     * Unicode handling **
     */
    @Test
    @DisplayName("Unicode handling – Japanese characters are decoded correctly")
    void testUnicodeHandling() {
        String resourceName = "/testfiles/unicode.txt";
        String expected = "こんにちは世界";
        String actual = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(expected, actual, "Unicode content was not decoded correctly");
    }

    /**
     * Path variations – without leading slash **
     */
    @Test
    @DisplayName("Path variation – resource name without leading slash is resolved")
    void testPathVariationWithoutLeadingSlash() {
        // no leading '/'
        String resourceName = "testfiles/simple.txt";
        String expected = "Hello World";
        String actual = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(expected, actual, "Resource should be found even without a leading slash");
    }

    /**
     * Binary content – PNG file **
     */
    @Test
    @DisplayName("Binary content – loading a PNG returns a non‑null String")
    void testBinaryContent() {
        String resourceName = "/testfiles/image.png";
        String result = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(result, "Result must not be null for binary resources");
        // The raw bytes are interpreted as UTF‑8; we only assert that something was returned.
        // Some PNG files start with non‑printable characters, so we avoid content checks.
        assertFalse(result.isEmpty(), "Result should not be empty for a non‑empty binary file");
    }

    /**
     * Security handling – access denied **
     */
    @Test
    @DisplayName("Security handling – SecurityException propagates when access is denied")
    void testSecurityExceptionPropagation() {
        // Save the original SecurityManager so we can restore it after the test.
        SecurityManager original = System.getSecurityManager();
        // Install a SecurityManager that denies all permission checks.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Deny everything – this will cause a SecurityException for any operation
                // that triggers a permission check (including class‑loader resource access).
                throw new SecurityException("Access denied by test SecurityManager");
            }
        });
        try {
            assertThrows(SecurityException.class, () -> ClassUtilities.loadResourceAsString("/testfiles/secure.txt"), "SecurityException should be thrown when a SecurityManager blocks resource access");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
