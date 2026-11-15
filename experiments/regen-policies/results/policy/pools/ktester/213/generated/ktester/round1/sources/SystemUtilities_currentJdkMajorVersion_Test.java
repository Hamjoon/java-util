package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class SystemUtilities_currentJdkMajorVersion_Test {

    private static String originalSpecVersion;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original specification version for the whole test run
        originalSpecVersion = System.getProperty("java.specification.version");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with the original value (or null if it was not set)
        if (originalSpecVersion != null) {
            System.setProperty("java.specification.version", originalSpecVersion);
        } else {
            System.clearProperty("java.specification.version");
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the property after each test to avoid side‑effects
        if (originalSpecVersion != null) {
            System.setProperty("java.specification.version", originalSpecVersion);
        } else {
            System.clearProperty("java.specification.version");
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Final clean‑up – make sure the JVM property is exactly as it was before the suite
        if (originalSpecVersion != null) {
            System.setProperty("java.specification.version", originalSpecVersion);
        } else {
            System.clearProperty("java.specification.version");
        }
    }

    /**
     * Provides a stream of valid specification strings together with the
     * expected major version integer.
     *
     * Covers both legacy ("1.x") and modern version formats, including
     * edge cases such as very large version numbers.
     */
    private static Stream<Arguments> validSpecsProvider() {
        return Stream.of(// Legacy formats
        Arguments.of("1.8", 8), Arguments.of("1.7", 7), Arguments.of("1.9", 9), Arguments.of("1.10", 10), // Modern formats
        Arguments.of("9", 9), Arguments.of("11", 11), Arguments.of("17", 17), Arguments.of("24", 24), Arguments.of("1000", 1000));
    }

    /**
     * Provides a stream of specification strings that should cause the
     * method to throw an exception, together with the expected exception type.
     *
     * The {@code null} case is represented by an empty Optional – we clear the
     * property to simulate the property being undefined.
     */
    private static Stream<Arguments> exceptionSpecsProvider() {
        return Stream.of(// Property not defined → NullPointerException (spec is null)
        Arguments.of(null, NullPointerException.class), // Empty string
        Arguments.of("", NumberFormatException.class), // Non‑numeric value
        Arguments.of("abc", NumberFormatException.class), // Legacy format with only prefix
        Arguments.of("1.", NumberFormatException.class), // Legacy format with non‑numeric suffix
        Arguments.of("1.a", NumberFormatException.class), // Modern format that still contains a dot (treated as non‑numeric)
        Arguments.of("1.8.0", NumberFormatException.class), // Whitespace only
        Arguments.of(" ", NumberFormatException.class), // Legacy format with whitespace after the dot
        Arguments.of("1. 8", NumberFormatException.class));
    }

    @Test
    void returnsMajorVersionWhenSpecStartsWithOneDot() {
        // "1.8" should be interpreted as major version 8
        System.setProperty("java.specification.version", "1.8");
        int major = SystemUtilities.currentJdkMajorVersion();
        assertEquals(8, major, "Expected major version 8 for spec '1.8'");
    }

    @Test
    void returnsMajorVersionWhenSpecDoesNotStartWithOneDot() {
        // Modern JDKs report the version directly, e.g., "11", "17", "24"
        System.setProperty("java.specification.version", "17");
        int major = SystemUtilities.currentJdkMajorVersion();
        assertEquals(17, major, "Expected major version 17 for spec '17'");
        System.setProperty("java.specification.version", "24");
        major = SystemUtilities.currentJdkMajorVersion();
        assertEquals(24, major, "Expected major version 24 for spec '24'");
    }

    @Test
    void throwsNumberFormatExceptionForNonNumericSpec() {
        // If the property cannot be parsed, the method propagates NumberFormatException
        System.setProperty("java.specification.version", "invalid");
        assertThrows(NumberFormatException.class, SystemUtilities::currentJdkMajorVersion, "Expected NumberFormatException for non‑numeric spec value");
    }

    @ParameterizedTest(name = "Spec \"{0}\" should be parsed as major version {1}")
    @MethodSource("validSpecsProvider")
    void returnsMajorVersionForVariousSpecifications(String spec, int expected) {
        System.setProperty("java.specification.version", spec);
        int actual = SystemUtilities.currentJdkMajorVersion();
        assertEquals(expected, actual, () -> String.format("Expected major version %d for spec \"%s\"", expected, spec));
    }

    @ParameterizedTest(name = "Spec \"{0}\" should throw {1}")
    @MethodSource("exceptionSpecsProvider")
    void throwsExpectedExceptionForInvalidSpecifications(String spec, Class<? extends Throwable> expectedException) {
        if (spec == null) {
            // Simulate missing property
            System.clearProperty("java.specification.version");
        } else {
            System.setProperty("java.specification.version", spec);
        }
        assertThrows(expectedException, SystemUtilities::currentJdkMajorVersion, () -> String.format("Expected %s for spec \"%s\"", expectedException.getSimpleName(), spec));
    }
}
