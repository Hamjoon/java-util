package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalTime_Test {

    /**
     * The {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * The default {@link Converter} used for normal test cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        defaultConverter = null;
    }

    private static Stream<Arguments> validLocalTimeProvider() {
        return Stream.of(// hour, minute, second
        Arguments.of("10:15:30", LocalTime.of(10, 15, 30)), // hour and minute only
        Arguments.of("10:15", LocalTime.of(10, 15)), // hour, minute, second, nanosecond fraction
        Arguments.of("10:15:30.123456789", LocalTime.of(10, 15, 30, 123_456_789)));
    }

    private static Stream<Arguments> zonedFallbackProvider() {
        return Stream.of(// Offset +01:00
        Arguments.of("2023-03-25T10:15:30+01:00", LocalTime.of(10, 15, 30)), // UTC (Z)
        Arguments.of("2023-03-25T10:15:30Z", LocalTime.of(10, 15, 30)));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("not a time"), // date‑only
        Arguments.of("2023-03-25"), // offset without date
        Arguments.of("10:15:30+02:00"), // empty string
        Arguments.of(""));
    }

    @Test
    void toLocalTime_parsesSimpleLocalTimeString() {
        // GIVEN
        String source = "12:34:56";
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid LocalTime string");
        assertEquals(LocalTime.of(12, 34, 56), result);
    }

    @Test
    void toLocalTime_fallsBackToZonedDateTime_whenLocalTimeParseFails() {
        // GIVEN
        // This string cannot be parsed by LocalTime.parse but is a valid ISO‑8601 ZonedDateTime.
        String source = "2023-03-25T12:34:56Z";
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null when the string is a valid ZonedDateTime");
        // The ZonedDateTime represents 12:34:56 UTC, so the extracted LocalTime must be 12:34:56.
        assertEquals(LocalTime.of(12, 34, 56), result);
    }

    @Test
    void toLocalTime_returnsNull_whenStringIsNotParsableAsLocalOrZonedTime() {
        // GIVEN
        String source = "not-a-time-at-all";
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, converter);
        // THEN
        assertNull(result, "Result should be null when the string cannot be parsed as either LocalTime or ZonedDateTime");
    }

    @Test
    void toLocalTime_returnsNull_whenZonedConversionReturnsNull() {
        // GIVEN
        // A string that LocalTime.parse fails on and that toZonedDateTime also cannot handle.
        // Using an empty string forces both parsers to fail.
        String source = "";
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, converter);
        // THEN
        assertNull(result, "Result should be null when both parsing attempts fail (empty string case)");
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "valid LocalTime parsing" group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → {1}")
    @MethodSource("validLocalTimeProvider")
    void toLocalTime_parsesValidLocalTimeStrings(String source, LocalTime expected) {
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, defaultConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid LocalTime string");
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "fallback to ZonedDateTime parsing" group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] fallback parse \"{0}\" → {1}")
    @MethodSource("zonedFallbackProvider")
    void toLocalTime_fallsBackToZonedDateTime(String source, LocalTime expected) {
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, defaultConverter);
        // THEN
        assertNotNull(result, "Result should not be null when fallback succeeds");
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "invalid format returns null" group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] invalid input \"{0}\" → null")
    @MethodSource("invalidInputProvider")
    void toLocalTime_returnsNullForInvalidInputs(String source) {
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, defaultConverter);
        // THEN
        assertNull(result, "Result should be null for an unparsable string");
    }

    /* -----------------------------------------------------------
     *  Null source handling – should return null (no exception)
     * ----------------------------------------------------------- */
    @Test
    void toLocalTime_returnsNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalTime result = StringConversions.toLocalTime(source, defaultConverter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
    }

    /* -----------------------------------------------------------
     *  Non‑String source – expect ClassCastException
     * ----------------------------------------------------------- */
    @Test
    void toLocalTime_throwsClassCastExceptionWhenSourceIsNotString() {
        // GIVEN
        // Integer, not a String
        Object source = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toLocalTime(source, defaultConverter), "A non‑String source must cause a ClassCastException");
    }

    /* -----------------------------------------------------------
     *  Converter that throws during the fallback conversion
     * ----------------------------------------------------------- */
    @Test
    void toLocalTime_propagatesExceptionFromConverterDuringFallback(@Mock Converter throwingConverter) {
        // Arrange a converter that throws on any conversion attempt.
        doAnswer(invocation -> {
            throw new RuntimeException("forced exception from converter");
        }).when(throwingConverter).convert(any(), any());
        // This string cannot be parsed by LocalTime.parse, forcing the fallback.
        String source = "2023-03-25T10:15:30+01:00";
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toLocalTime(source, throwingConverter), "Exception from the converter should propagate out of toLocalTime");
        assertTrue(ex.getMessage().contains("forced exception"));
    }

    /* -----------------------------------------------------------
     *  Null converter – expect NullPointerException during fallback
     * ----------------------------------------------------------- */
    @Test
    void toLocalTime_throwsNullPointerExceptionWhenConverterIsNull() {
        // GIVEN
        // forces fallback
        String source = "2023-03-25T10:15:30+01:00";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toLocalTime(source, null), "A null converter should cause a NullPointerException during the fallback conversion");
    }
}
