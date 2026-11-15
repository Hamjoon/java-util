package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toTimeZone_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    /**
     * Provides inputs together with the expected TimeZone ID or {@code null}.
     *
     * <p>For the GMT offset format we use a sentinel ID {@code "GMT_OFFSET"} and also
     * provide the expected raw offset in milliseconds.</p>
     */
    private static Stream<Arguments> provideTimeZoneResults() {
        return Stream.of(// Null and empty inputs
        arguments(null, null, null), arguments("   ", null, null), arguments("", null, null), // Valid IDs
        arguments("UTC", "UTC", null), arguments("America/New_York", "America/New_York", null), arguments("GMT+02:00", "GMT_OFFSET", 7_200_000), // Invalid IDs – fallback to GMT
        arguments("Invalid/Zone", "GMT", null), arguments("XYZ", "GMT", null), // Whitespace handling
        arguments("  UTC  ", "UTC", null), arguments("\tAmerica/Los_Angeles\n", "America/Los_Angeles", null), // Case sensitivity – lower‑case identifiers are not recognised
        arguments("utc", "GMT", null), arguments("UsEr", "GMT", null));
    }

    /**
     * Provides non‑String inputs that should trigger a {@link ClassCastException}.
     */
    private static Stream<Arguments> provideClassCastExceptionCases() {
        return Stream.of(arguments(123), arguments(true));
    }

    @Test
    void toTimeZone_NullInput_ReturnsNull() {
        TimeZone tz = StringConversions.toTimeZone(null, converter);
        assertNull(tz, "Null source should result in null TimeZone");
    }

    @Test
    void toTimeZone_BlankString_ReturnsNull() {
        TimeZone tz = StringConversions.toTimeZone("   \t\n", converter);
        assertNull(tz, "Blank/whitespace‑only string should be trimmed to null");
    }

    @Test
    void toTimeZone_ValidId_ReturnsCorrespondingTimeZone() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertNotNull(tz, "TimeZone for \"UTC\" should not be null");
        assertEquals("UTC", tz.getID(), "TimeZone ID should match the input string");
    }

    @Test
    void toTimeZone_StringWithWhitespace_IsTrimmedBeforeLookup() {
        // Input contains leading/trailing spaces; method should trim them.
        TimeZone tz = StringConversions.toTimeZone("  PST  ", converter);
        assertNotNull(tz, "Trimmed string should produce a TimeZone");
        assertEquals("PST", tz.getID(), "Whitespace should be removed before TimeZone lookup");
    }

    @Test
    void toTimeZone_UnknownId_ReturnsGMT() {
        // TimeZone.getTimeZone returns a GMT zone for unknown IDs.
        TimeZone tz = StringConversions.toTimeZone("some-nonexistent-zone", converter);
        assertNotNull(tz, "Even unknown IDs return a TimeZone instance");
        assertEquals("GMT", tz.getID(), "Unknown IDs should default to GMT");
    }

    /**
     * Test cases that are expected to return a {@link TimeZone} instance.
     *
     * The {@code expectedId} can be {@code null} when the method should return {@code null}
     * (e.g., blank or empty strings).  When {@code expectedId} is {@code "GMT_OFFSET"},
     * the test asserts the raw offset of the returned zone.
     */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected ID=''{1}''")
    @MethodSource("provideTimeZoneResults")
    void toTimeZone_ReturnsExpectedTimeZone(Object from, String expectedId, Integer expectedRawOffset) {
        // Act
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        // Assert based on expectations
        if (expectedId == null) {
            assertNull(tz, "Expected null TimeZone for input: " + from);
        } else {
            assertNotNull(tz, "Expected non‑null TimeZone for input: " + from);
            if ("GMT_OFFSET".equals(expectedId)) {
                // Only used for the GMT+02:00 test case
                assertEquals(expectedRawOffset.intValue(), tz.getRawOffset(), "Raw offset should match expected milliseconds");
            } else {
                assertEquals(expectedId, tz.getID(), "TimeZone ID should match expected value");
            }
        }
    }

    /**
     * Test cases that are expected to throw {@link ClassCastException}
     * because the source object cannot be cast to {@link String}.
     */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expects ClassCastException")
    @MethodSource("provideClassCastExceptionCases")
    void toTimeZone_NonStringInput_ThrowsClassCastException(Object from) {
        assertThrows(ClassCastException.class, () -> StringConversions.toTimeZone(from, converter), "Non‑String input should cause ClassCastException");
    }
}
