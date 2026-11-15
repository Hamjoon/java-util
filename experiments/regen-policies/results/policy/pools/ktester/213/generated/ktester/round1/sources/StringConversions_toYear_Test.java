package com.cedarsoftware.util.convert;

import java.time.Year;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class StringConversions_toYear_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; use the default constructor
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  Group: Parsing valid year strings
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validYearStringProvider() {
        return Stream.of(Arguments.of("2023", Year.of(2023), "Plain 4‑digit year"), Arguments.of("  1999  ", Year.of(1999), "Year with surrounding whitespace"), Arguments.of("+2020", Year.of(2020), "Leading plus sign"), Arguments.of("0005", Year.of(5), "Leading zeros"), Arguments.of("-0007", Year.of(-7), "Negative year with leading zeros"), Arguments.of("2147483647", Year.of(2147483647), "Maximum int value"), Arguments.of("-2147483648", Year.of(-2147483648), "Minimum int value"));
    }

    /* -------------------------------------------------------------
     *  Group: Parsing non‑numeric but date‑time strings
     * ------------------------------------------------------------- */
    private static Stream<Arguments> dateStringFallbackProvider() {
        return Stream.of(Arguments.of("2022-05-10", 2022, "ISO‑8601 date without time"), Arguments.of("2021-12-31T23:59:59Z", 2021, "Full ISO‑8601 timestamp"));
    }

    /* -------------------------------------------------------------
     *  Group: Invalid formats causing exception
     * ------------------------------------------------------------- */
    private static Stream<Arguments> invalidFormatProvider() {
        return Stream.of(Arguments.of("abcd", "Non‑numeric garbage string"), Arguments.of("2022/13/01", "Malformed date string"));
    }

    /* -------------------------------------------------------------
     *  Group: Non‑String input type
     * ------------------------------------------------------------- */
    private static Stream<Arguments> nonStringInputProvider() {
        return Stream.of(Arguments.of(12345, "Integer input"), Arguments.of(new java.util.Date(), "Date object input"));
    }

    /* -------------------------------------------------------------
     *  Group: Numeric out‑of‑range and overflow scenarios
     * ------------------------------------------------------------- */
    private static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(Arguments.of("1000000000", "Year beyond supported upper bound"), Arguments.of("-1000000000", "Year beyond supported lower bound"), Arguments.of("9999999999", "Value larger than Integer.MAX_VALUE"));
    }

    @Test
    void shouldParseSimpleFourDigitYearString() {
        Year result = StringConversions.toYear("2023", converter);
        assertNotNull(result);
        assertEquals(2023, result.getValue());
    }

    @Test
    void shouldTrimAndParseYearStringWithWhitespace() {
        Year result = StringConversions.toYear("  2021  ", converter);
        assertNotNull(result);
        assertEquals(2021, result.getValue());
    }

    @Test
    void shouldFallbackToZonedDateTimeWhenNumericParseFails() {
        // Mock the static fallback method to return a ZonedDateTime with the required year
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            ZonedDateTime zdt = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).withYear(expectedYear);
            mocked.when(() -> StringConversions.toZonedDateTime(eq(from), any())).thenReturn(zdt);
            Year result = StringConversions.toYear(from, converter);
            assertNotNull(result, description);
            assertEquals(expectedYear, result.getValue(), description);
        }
    }

    @Test
    void shouldReturnNullWhenZonedDateTimeConversionReturnsNull() {
        // Empty string becomes null after trim; toZonedDateTime returns null for such input
        Year result = StringConversions.toYear("", converter);
        assertNull(result);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenParsingIsImpossible() {
        // Force the fallback to throw an exception so the method wraps it in IllegalArgumentException
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(from), any())).thenThrow(new DateTimeParseException("cannot parse", from, 0));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(from, converter), description);
            assertTrue(ex.getMessage().contains("Unable to parse 4-digit year"), description);
        }
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        Year result = StringConversions.toYear(null, converter);
        assertNull(result, "Null source should result in null");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validYearStringProvider")
    void shouldParseValidYearStrings(Object from, Year expected, String description) {
        Year result = StringConversions.toYear(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result, description);
    }

    @Test
    void shouldReturnNullWhenStringIsEmptyOrWhitespace() {
        // Empty string becomes null after trim; we mock the fallback to return null
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq("   "), any())).thenReturn(null);
            Year result = StringConversions.toYear("   ", converter);
            assertNull(result, "Whitespace‑only string should result in null");
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("nonStringInputProvider")
    void shouldThrowClassCastExceptionWhenInputIsNotString(Object from, String description) {
        assertThrows(ClassCastException.class, () -> StringConversions.toYear(from, converter), description);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("outOfRangeProvider")
    void shouldThrowIllegalArgumentExceptionForOutOfRangeValues(String from, String description) {
        // For the first two cases Integer.parseInt succeeds but Year.of throws DateTimeException.
        // We mock the fallback to also fail, forcing an IllegalArgumentException.
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(from), any())).thenThrow(new DateTimeParseException("cannot parse", from, 0));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(from, converter), description);
            assertTrue(ex.getMessage().contains("Unable to parse 4-digit year"), description);
        }
    }
}
