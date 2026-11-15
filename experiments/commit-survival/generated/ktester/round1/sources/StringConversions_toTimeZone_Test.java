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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StringConversions_toTimeZone_Test {

    /**
     * The Converter is not used by the implementation of {@code toTimeZone},
     *  but it is required by the method signature.  A mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple stub implementation to act as a non‑null {@code Converter}.
     */
    private static class MockConverter implements Converter {
        // No implementation needed – the test only checks that it is not used.
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(""), Arguments.of("   \t\n"));
    }

    private static Stream<Arguments> validIdsProvider() {
        return Stream.of(Arguments.of("UTC", "UTC"), Arguments.of("America/New_York", "America/New_York"), Arguments.of("GMT+02:00", "GMT+02:00"));
    }

    private static Stream<Arguments> unknownIdProvider() {
        return Stream.of(Arguments.of("NotARealZone"));
    }

    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(Arguments.of("  UTC  ", "UTC"), Arguments.of("\tAmerica/Los_Angeles\n", "America/Los_Angeles"));
    }

    private static Stream<Arguments> converterIgnoredProvider() {
        // The same mockConverter is reused; Mockito guarantees a fresh mock per test class instance.
        return Stream.of(Arguments.of("UTC", new MockConverter(), "UTC"));
    }

    private static Stream<Arguments> nonStringProvider() {
        return Stream.of(Arguments.of(12345), Arguments.of(Boolean.TRUE));
    }

    @Test
    void toTimeZone_NullInput_ReturnsNull() {
        // given
        Object input = null;
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNull(result, "Null input should produce a null TimeZone");
    }

    @Test
    void toTimeZone_EmptyString_ReturnsNull() {
        // given
        Object input = "";
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNull(result, "Empty string should be trimmed to null and return null");
    }

    @Test
    void toTimeZone_BlankString_ReturnsNull() {
        // given
        Object input = "   \t\n";
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNull(result, "Blank string should be trimmed to null and return null");
    }

    @Test
    void toTimeZone_ValidTimeZoneId_ReturnsCorrespondingTimeZone() {
        // given
        String tzId = "America/New_York";
        Object input = tzId;
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNotNull(result, "A valid time‑zone ID should produce a non‑null TimeZone");
        assertEquals(tzId, result.getID(), "The TimeZone ID should match the input string");
    }

    @Test
    void toTimeZone_UnknownTimeZoneId_ReturnsGMT() {
        // given
        String unknownId = "some-unknown-id";
        Object input = unknownId;
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNotNull(result, "Even an unknown ID returns a TimeZone instance");
        // According to TimeZone.getTimeZone(String), unknown IDs default to GMT.
        assertEquals("GMT", result.getID(), "Unknown IDs should default to GMT");
    }

    /* --------------------------------------------------------------
     * Group: handling of null and empty strings
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nullAndEmptyProvider")
    void toTimeZone_NullAndEmptyStrings(Object input) {
        // when
        TimeZone result = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNull(result, "Null, empty or blank strings should produce a null TimeZone");
    }

    /* --------------------------------------------------------------
     * Group: valid time zone IDs
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" => expected ID=\"{1}\"")
    @MethodSource("validIdsProvider")
    void toTimeZone_ValidIds(String input, String expectedId) {
        // when
        TimeZone tz = StringConversions.toTimeZone(input, mockConverter);
        // then
        assertNotNull(tz, "A valid ID must return a non‑null TimeZone");
        assertEquals(expectedId, tz.getID(), () -> "TimeZone ID should match the input string");
    }

    /* --------------------------------------------------------------
     * Group: unknown ID fallback to GMT
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] unknown ID=\"{0}\"")
    @MethodSource("unknownIdProvider")
    void toTimeZone_UnknownIdReturnsGMT(String unknownId) {
        // when
        TimeZone tz = StringConversions.toTimeZone(unknownId, mockConverter);
        // then
        assertNotNull(tz, "Even an unknown ID must return a TimeZone instance");
        assertEquals("GMT", tz.getID(), () -> "Unknown IDs should fall back to GMT per TimeZone#getTimeZone contract");
    }

    /* --------------------------------------------------------------
     * Group: whitespace trimming
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] raw=\"{0}\" => trimmed=\"{1}\"")
    @MethodSource("whitespaceProvider")
    void toTimeZone_WhitespaceTrimming(String rawInput, String expectedId) {
        // when
        TimeZone tz = StringConversions.toTimeZone(rawInput, mockConverter);
        // then
        assertNotNull(tz, "Trimming should still yield a non‑null TimeZone");
        assertEquals(expectedId, tz.getID(), () -> "Whitespace should be removed before lookup");
    }

    /* --------------------------------------------------------------
     * Group: converter argument ignored
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" with non‑null converter")
    @MethodSource("converterIgnoredProvider")
    void toTimeZone_ConverterArgumentIgnored(String input, Converter converter, String expectedId) {
        // when
        TimeZone tz = StringConversions.toTimeZone(input, converter);
        // then
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        // Verify that the method never interacted with the converter.
        verifyNoInteractions(converter);
    }

    /* --------------------------------------------------------------
     * Group: non‑string input causes ClassCastException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] non‑string input={0}")
    @MethodSource("nonStringProvider")
    void toTimeZone_NonStringInputThrowsClassCastException(Object nonString) {
        assertThrows(ClassCastException.class, () -> StringConversions.toTimeZone(nonString, mockConverter), "Passing a non‑String should result in a ClassCastException");
    }
}
