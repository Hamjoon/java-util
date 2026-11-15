package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toYear_Test {

    // mock – only needed as a placeholder for the API
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for every test
        converter = mock(Converter.class);
    }

    private static Stream<Arguments> numericParsingProvider() {
        return Stream.of(Arguments.of("plain 4‑digit year", "2022", Year.of(2022)), Arguments.of("whitespace trimmed", "   1999   ", Year.of(1999)), Arguments.of("leading zeros", "0010", Year.of(10)), Arguments.of("explicit plus sign", "+2023", Year.of(2023)), Arguments.of("negative year (proleptic)", "-44", Year.of(-44)), Arguments.of("maximum int value", "2147483647", Year.of(2147483647)), Arguments.of("minimum int value", "-2147483648", Year.of(-2147483648)));
    }

    private static Stream<Arguments> zonedDateTimeFallbackProvider() {
        return Stream.of(Arguments.of("ISO‑8601 timestamp without offset", "2021-07-15T12:30:00Z", ZonedDateTime.ofInstant(Instant.parse("2021-07-15T12:30:00Z"), ZoneOffset.UTC), Year.of(2021)), Arguments.of("ISO‑8601 with zone offset", "2023-03-10T15:00+02:00", ZonedDateTime.ofInstant(Instant.parse("2023-03-10T13:00:00Z"), ZoneOffset.UTC), Year.of(2023)), Arguments.of("non‑null converter does not affect outcome", "2020-12-31T23:59:59Z", ZonedDateTime.ofInstant(Instant.parse("2020-12-31T23:59:59Z"), ZoneOffset.UTC), Year.of(2020)));
    }

    private static Stream<Arguments> nullResultProvider() {
        return Stream.of(Arguments.of("null input", null), Arguments.of("empty string", ""), Arguments.of("whitespace‑only string", "   "));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("numeric overflow (above Integer.MAX_VALUE)", "2147483648", new RuntimeException("cannot parse")), Arguments.of("numeric overflow (below Integer.MIN_VALUE)", "-2147483649", new RuntimeException("cannot parse")), Arguments.of("mixed alphanumeric string", "1234abc", new RuntimeException("cannot parse")), Arguments.of("plus sign only after trim", "+   ", new RuntimeException("cannot parse")), Arguments.of("literal \"null\" string", "null", new RuntimeException("cannot parse")));
    }

    /**
     * 1. Direct parsing of a clean 4‑digit year string **
     */
    @Test
    void toYear_parsesSimpleYearString() {
        Year result = StringConversions.toYear("2023", converter);
        assertEquals(Year.of(2023), result);
    }

    /**
     * 2. Trims surrounding whitespace before parsing **
     */
    @Test
    void toYear_trimsWhitespace() {
        Year result = StringConversions.toYear("   1999   ", converter);
        assertEquals(Year.of(1999), result);
    }

    /**
     * 3. Falls back to ZonedDateTime when the string is not a plain year **
     */
    @Test
    void toYear_fallsBackToZonedDateTime() {
        // Mock only the static toZonedDateTime method; let every other static method run normally
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            ZonedDateTime zdt = ZonedDateTime.of(2015, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC);
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(zdt);
            Year result = StringConversions.toYear("not-a-year", converter);
            assertEquals(Year.of(2015), result);
        }
    }

    /**
     * 4. Returns null when the fallback conversion yields null **
     */
    @Test
    void toYear_returnsNullWhenZonedDateTimeIsNull() {
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(// simulate inability to convert
            null);
            Year result = StringConversions.toYear(null, converter);
            assertNull(result);
        }
    }

    /**
     * 5. Propagates an IllegalArgumentException when both strategies fail **
     */
    @Test
    void toYear_throwsWhenBothParsesFail() {
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new RuntimeException("cannot parse"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear("bad-input", converter));
            assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from 'bad-input'"));
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericParsingProvider")
    void toYear_directNumericParsing(String description, String input, Year expected) {
        Year result = StringConversions.toYear(input, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("zonedDateTimeFallbackProvider")
    void toYear_fallbackToZonedDateTime(String description, String input, ZonedDateTime zdt, Year expected) {
        // Mock only the static toZonedDateTime method; all other static methods keep their real behaviour
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(zdt);
            Year result = StringConversions.toYear(input, converter);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullResultProvider")
    void toYear_returnsNullWhenFallbackIsNull(String description, Object input) {
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            // Force the fallback to return null regardless of the argument
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(null);
            Year result = StringConversions.toYear(input, converter);
            assertNull(result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("illegalArgumentProvider")
    void toYear_throwsIllegalArgumentException(String description, String input, RuntimeException fallbackException) {
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(fallbackException);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, converter), description);
            // The original parsing exception (NumberFormatException) should be the cause
            assertNotNull(ex.getCause(), "Cause should be present");
            assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"), "Message should contain the original input");
        }
    }

    @Test
    void toYear_throwsClassCastExceptionForNonStringInput() {
        // The method casts the argument to String before any try/catch.
        // Supplying an Integer should cause an immediate ClassCastException.
        assertThrows(ClassCastException.class, () -> StringConversions.toYear(12345, converter), "Non‑String input must cause ClassCastException");
    }
}
