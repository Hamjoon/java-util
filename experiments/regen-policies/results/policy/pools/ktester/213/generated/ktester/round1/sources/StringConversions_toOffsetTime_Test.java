package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class StringConversions_toOffsetTime_Test {

    /**
     * Converter instance used for the tests.
     */
    private static Converter converter;

    /* --------------------------------------------------------------
     *  Helper holder class for parameterized arguments
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final String input;

        // Expected string representation of OffsetTime, or null if exception expected
        final String expected;

        final Converter converter;

        final String description;

        ArgumentsHolder(String input, String expected, Converter converter, String description) {
            this.input = input;
            this.expected = expected;
            this.converter = converter;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they are sufficient for the conversion tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<ArgumentsHolder> successfulParsingProvider() {
        return Stream.of(// ISO_OFFSET_TIME – direct parsing
        new ArgumentsHolder("12:34:56+02:00", "12:34:56+02:00", converter, "A valid ISO‑OFFSET‑TIME string is parsed directly."), // UTC designator 'Z'
        new ArgumentsHolder("12:34:56Z", "12:34:56Z", converter, "Valid UTC offset using 'Z' designator."), // Fractional seconds
        new ArgumentsHolder("12:34:56.123456789+02:00", "12:34:56.123456789+02:00", converter, "OffsetTime.parse supports nanosecond fractions."), // Fallback: pure OffsetDateTime string
        new ArgumentsHolder("2023-08-15T12:34:56+02:00", "12:34:56+02:00", converter, "Fallback to OffsetDateTime parsing."), // Fallback with nanosecond fraction
        new ArgumentsHolder("2023-08-15T12:34:56.987654321+02:00", "12:34:56.987654321+02:00", converter, "Fallback retains nanosecond precision."), // Null converter but valid direct parse
        new ArgumentsHolder("12:34:56+02:00", "12:34:56+02:00", null, "Null converter does not affect direct parsing."), // Null converter with fallback required
        new ArgumentsHolder("2023-08-15T12:34:56+02:00", "12:34:56+02:00", null, "Null converter does not affect fallback parsing."));
    }

    private static Stream<ArgumentsHolder> exceptionParsingProvider() {
        return Stream.of(// Empty string
        new ArgumentsHolder("", null, converter, "Empty string"), // Whitespace‑only string
        new ArgumentsHolder("   ", null, converter, "Whitespace‑only string"), // Time without offset
        new ArgumentsHolder("12:34:56", null, converter, "Time without offset"), // Offset without colon (invalid ISO format)
        new ArgumentsHolder("12:34:56+0200", null, converter, "Offset without colon"), // Trailing whitespace
        new ArgumentsHolder("12:34:56+02:00 ", null, converter, "Trailing whitespace"));
    }

    @Test
    void parsesValidIsoOffsetTimeString() {
        // GIVEN
        String isoOffsetTime = "12:34:56+02:00";
        OffsetTime expected = OffsetTime.parse(isoOffsetTime);
        // WHEN
        OffsetTime actual = StringConversions.toOffsetTime(isoOffsetTime, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void fallsBackToOffsetDateTimeWhenOffsetTimeParseFails() {
        // GIVEN
        // This string is a valid ISO_OFFSET_DATE_TIME but NOT a valid ISO_OFFSET_TIME.
        String isoOffsetDateTime = "2023-01-02T12:34:56+02:00";
        OffsetTime expected = OffsetDateTime.parse(isoOffsetDateTime).toOffsetTime();
        // WHEN
        OffsetTime actual = StringConversions.toOffsetTime(isoOffsetDateTime, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void throwsIllegalArgumentExceptionForUnparseableString() {
        // GIVEN
        String badString = "not-a-time";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(badString, converter));
        // The message should contain the original string for easier debugging.
        assertTrue(ex.getMessage().contains(badString));
    }

    @ParameterizedTest(name = "{index} ⇒ {3}")
    @MethodSource("successfulParsingProvider")
    void parsesSuccessfully(ArgumentsHolder holder) {
        // GIVEN
        String input = holder.input;
        Converter conv = holder.converter;
        // WHEN
        OffsetTime result = StringConversions.toOffsetTime(input, conv);
        // THEN
        assertNotNull(result);
        assertEquals(OffsetTime.parse(holder.expected), result);
    }

    @ParameterizedTest(name = "{index} ⇒ {3}")
    @MethodSource("exceptionParsingProvider")
    void throwsIllegalArgumentExceptionForInvalidFormats(ArgumentsHolder holder) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(holder.input, holder.converter));
        // The message should contain the original (raw) string.
        assertTrue(ex.getMessage().contains(holder.input));
    }

    @Test
    void throwsClassCastExceptionWhenInputIsNotString() {
        // GIVEN
        Integer nonString = 12345;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toOffsetTime(nonString, converter));
    }

    @Test
    void throwsNullPointerExceptionWhenInputIsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toOffsetTime(nullInput, converter));
    }

    @Test
    void fallbackReturnsNullWhenToOffsetDateTimeReturnsNull() {
        // GIVEN
        String unparseable = "invalid‑date‑string";
        // Mock the static method toOffsetDateTime to return null for this input.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toOffsetDateTime(unparseable, converter)).thenReturn(null);
            // WHEN
            OffsetTime result = StringConversions.toOffsetTime(unparseable, converter);
            // THEN
            assertNull(result);
        }
    }

    @Test
    void fallbackThrowsIllegalArgumentExceptionWithOriginalCause() {
        // GIVEN
        String bad = "!!!@@@###";
        // Mock toOffsetDateTime to throw a RuntimeException (simulating a failure).
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toOffsetDateTime(bad, converter)).thenThrow(new RuntimeException("inner failure"));
            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(bad, converter));
            // THEN
            assertTrue(ex.getMessage().contains(bad), "Exception message must contain the original string");
            // The cause should be the first parsing exception (DateTimeParseException)
            assertTrue(ex.getCause() instanceof DateTimeParseException, "Cause should be the DateTimeParseException from the first parse attempt");
        }
    }
}
