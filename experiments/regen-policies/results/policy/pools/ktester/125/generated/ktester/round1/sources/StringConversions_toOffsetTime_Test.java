package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class StringConversions_toOffsetTime_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter CONVERTER;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special configuration for these tests.
        CONVERTER = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validIsoOffsetTimeProvider() {
        return Stream.of(Arguments.of("Simple offset time", "10:15:30+01:00", OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(1))), Arguments.of("Parsing with nanoseconds", "10:15:30.123456789+02:00", OffsetTime.of(10, 15, 30, 123_456_789, ZoneOffset.ofHours(2))), Arguments.of("Parsing with UTC Z suffix", "10:15:30Z", OffsetTime.of(10, 15, 30, 0, ZoneOffset.UTC)));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("Completely invalid string", "not-a-time"), Arguments.of("Empty string", ""), Arguments.of("Time without offset", "10:15:30"), Arguments.of("Invalid offset format (missing colon)", "10:15:30+0100"));
    }

    @Test
    void parsesValidIsoOffsetTimeString() {
        // Given a string that matches ISO_OFFSET_TIME
        String source = "10:15:30+01:00";
        // When converting
        OffsetTime result = StringConversions.toOffsetTime(source, CONVERTER);
        // Then the result is the same as parsing directly with the formatter
        assertEquals(OffsetTime.parse(source), result);
    }

    @Test
    void fallsBackToOffsetDateTimeWhenDirectParseFails() {
        // Given a string that is not a pure OffsetTime but can be parsed as an OffsetDateTime
        String source = "2023-03-25T10:15:30+01:00";
        // When converting
        OffsetTime result = StringConversions.toOffsetTime(source, CONVERTER);
        // Then the method falls back to OffsetDateTime and extracts the OffsetTime part
        OffsetTime expected = OffsetDateTime.parse(source).toOffsetTime();
        assertEquals(expected, result);
    }

    @Test
    void throwsIllegalArgumentExceptionForUnparseableString() {
        // Given an invalid time representation
        String source = "not-a-time";
        // When converting, an IllegalArgumentException should be thrown
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(source, CONVERTER));
        // And the exception message should contain the original value
        assertTrue(ex.getMessage().contains("Unable to parse '" + source + "'"));
    }

    @Test
    void throwsIllegalArgumentExceptionWhenSourceIsNull() {
        // When the source object is null, the method ultimately throws IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(null, CONVERTER));
        // The message should still reference the inability to parse
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validIsoOffsetTimeProvider")
    void parsesValidIsoOffsetTime(String description, String source, OffsetTime expected) {
        // When converting
        OffsetTime result = StringConversions.toOffsetTime(source, CONVERTER);
        // Then the result matches the expected OffsetTime
        assertEquals(expected, result, "Failed parsing: " + description);
    }

    @Test
    void throwsClassCastExceptionWhenInputIsNotString() {
        // Passing an Integer triggers the cast to String inside the method
        assertThrows(ClassCastException.class, () -> StringConversions.toOffsetTime(123, CONVERTER));
    }

    @Test
    void returnsNullWhenOffsetDateTimeConversionReturnsNull() {
        // Mock the static call to StringConversions.toOffsetDateTime to return null
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class)) {
            // Preserve the original behavior for toOffsetTime (real method)
            mocked.when(() -> StringConversions.toOffsetDateTime(any(), any())).thenReturn(null);
            // The call to toOffsetTime will invoke the mocked toOffsetDateTime
            String source = "any";
            OffsetTime result = StringConversions.toOffsetTime(source, CONVERTER);
            assertNull(result, "When toOffsetDateTime returns null the method should return null");
        }
    }
}
