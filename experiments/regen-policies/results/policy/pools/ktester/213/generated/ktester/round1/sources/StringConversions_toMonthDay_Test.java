package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toMonthDay_Test {

    private Converter converter;

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with default options.
        converter = new Converter(new ConverterOptions());
        // By default, a non‑null ZoneId is returned to avoid NPEs in DateUtilities.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    private static Stream<Arguments> invalidMatcherProvider() {
        return Stream.of(Arguments.of("13/01"), Arguments.of("02/30"));
    }

    @Test
    void toMonthDay_ParsesIsoString() {
        // GIVEN
        String iso = "2023-12-25";
        // WHEN
        MonthDay result = StringConversions.toMonthDay(iso, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid ISO string");
        assertEquals(MonthDay.of(12, 25), result, "MonthDay parsed from ISO string does not match expected value");
    }

    @Test
    void toMonthDay_ParsesUsingMmDdPattern() {
        // GIVEN
        // The pattern "^(\\d{1,2}).(\\d{1,2})$" treats any single character as a separator.
        String mmdd = "3-7";
        // WHEN
        MonthDay result = StringConversions.toMonthDay(mmdd, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a string matching the MM_DD pattern");
        assertEquals(MonthDay.of(3, 7), result, "MonthDay parsed from MM_DD pattern does not match expected value");
    }

    @Test
    void toMonthDay_InvalidStringThrowsIllegalArgumentException() {
        // GIVEN
        String invalid = "not-a-date";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(invalid, converter), "Expected IllegalArgumentException for an unparseable string");
        assertTrue(ex.getMessage().contains("Unable to extract Month-Day from string"), "Exception message should indicate the inability to extract a month‑day");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_SuccessfulParse_IsoStringWithLeadingDashes() {
        // GIVEN
        String iso = "--02-28";
        // WHEN
        MonthDay result = StringConversions.toMonthDay(iso, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(MonthDay.of(2, 28), result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_ParsesUsingMatcher_MmSlashDd() {
        String input = "2/28";
        MonthDay result = StringConversions.toMonthDay(input, mockConverter);
        assertEquals(MonthDay.of(2, 28), result);
    }

    @Test
    void toMonthDay_ParsesUsingMatcher_DifferentSeparator() {
        String input = "2-28";
        MonthDay result = StringConversions.toMonthDay(input, mockConverter);
        assertEquals(MonthDay.of(2, 28), result);
    }

    @Test
    void toMonthDay_ParsesUsingMatcher_LeadingZeroAndDot() {
        String input = "02.28";
        MonthDay result = StringConversions.toMonthDay(input, mockConverter);
        assertEquals(MonthDay.of(2, 28), result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_DateParseNonNull_ReturnsMonthDayFromZonedDateTime() {
        String input = "2021-02-28T10:15:30Z";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            ZonedDateTime zdt = ZonedDateTime.of(2021, 2, 28, 10, 15, 30, 0, ZoneOffset.UTC);
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            MonthDay result = StringConversions.toMonthDay(input, mockConverter);
            assertEquals(MonthDay.of(2, 28), result);
        }
    }

    @Test
    void toMonthDay_DateParseNull_ReturnsNull() {
        String input = "unparsable";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            MonthDay result = StringConversions.toMonthDay(input, mockConverter);
            assertNull(result);
        }
    }

    @Test
    void toMonthDay_DateParseException_WrapsInIllegalArgumentException() {
        String input = "exception-date";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(new RuntimeException("boom"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, mockConverter));
            assertTrue(ex.getMessage().contains("Unable to extract Month-Day"));
            assertNotNull(ex.getCause());
            assertEquals("boom", ex.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toMonthDay(null, mockConverter));
    }

    @Test
    void toMonthDay_NonStringInput_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> StringConversions.toMonthDay(12345, mockConverter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("invalidMatcherProvider")
    void toMonthDay_InvalidMonthDayRangeMatcher_ThrowsDateTimeException(String input) {
        assertThrows(DateTimeException.class, () -> StringConversions.toMonthDay(input, mockConverter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_EmptyString_ReturnsNull() {
        String input = "";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            MonthDay result = StringConversions.toMonthDay(input, mockConverter);
            assertNull(result);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toMonthDay_NullConverter_WrapsNullPointerException() {
        String input = "2021-02-28T10:15:30Z";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, null));
        // The root cause should be a NullPointerException from converter.getOptions()
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void toMonthDay_ConverterOptionsNull_WrapsNullPointerException() {
        String input = "2021-02-28T10:15:30Z";
        // mockConverter.getOptions() returns null
        when(mockConverter.getOptions()).thenReturn(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, mockConverter));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void toMonthDay_ConverterZoneIdNull_WrapsException() {
        String input = "2021-02-28T10:15:30Z";
        // mock Options returning null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            // DateUtilities will throw NPE because ZoneId is null
            mocked.when(() -> DateUtilities.parseDate(eq(input), isNull(), eq(true))).thenThrow(new NullPointerException("zoneId is null"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, mockConverter));
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof NullPointerException);
            assertEquals("zoneId is null", ex.getCause().getMessage());
        }
    }
}
