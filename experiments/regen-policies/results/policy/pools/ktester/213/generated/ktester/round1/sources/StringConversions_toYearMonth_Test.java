package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import java.time.*;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class StringConversions_toYearMonth_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
        // Configure the converter to return our mocked options
        when(converter.getOptions()).thenReturn(options);
        // Use a deterministic ZoneId for the DateUtilities parsing
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the mock context
        mocks.close();
    }

    @Test
    void toYearMonth_parsesSimpleYearMonthString() {
        // GIVEN
        String input = "2022-08";
        // WHEN
        YearMonth result = StringConversions.toYearMonth(input, converter);
        // THEN
        assertEquals(YearMonth.of(2022, 8), result);
    }

    @Test
    void toYearMonth_parsesDateTimeStringViaDateUtilities() {
        // GIVEN
        // This string cannot be parsed by YearMonth.parse but can be parsed as a ZonedDateTime
        String input = "2022-08-15T12:30:00Z";
        // WHEN
        YearMonth result = StringConversions.toYearMonth(input, converter);
        // THEN
        // The year‑month part of the ZonedDateTime should be extracted
        assertEquals(YearMonth.of(2022, 8), result);
    }

    @Test
    void toYearMonth_invalidString_throwsIllegalArgumentException() {
        // GIVEN
        String input = "not-a-date";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
        assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
    }

    @Test
    void toYearMonth_fallbackParsesZonedDateTime() {
        // GIVEN
        String input = "2023-07-15T10:20:30Z";
        // Mock DateUtilities.parseDate to return a specific ZonedDateTime
        ZonedDateTime mockedZdt = ZonedDateTime.of(LocalDateTime.of(2023, 7, 15, 10, 20, 30), ZoneOffset.UTC);
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(mockedZdt);
            // WHEN
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // THEN
            assertEquals(YearMonth.of(2023, 7), result);
            utilities.verify(() -> DateUtilities.parseDate(eq(input), eq(ZoneId.of("UTC")), eq(true)));
        }
    }

    @Test
    void toYearMonth_fallbackReturnsNull() {
        // GIVEN
        String input = "not-a-date";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // WHEN
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // THEN
            assertNull(result);
        }
    }

    @Test
    void toYearMonth_fallbackThrowsException_wrappedInIllegalArgumentException() {
        // GIVEN
        String input = "bad-format";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(new NumberFormatException("bad number"));
            // WHEN / THEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string: bad-format"));
            assertTrue(ex.getCause() instanceof NumberFormatException);
        }
    }

    @Test
    void toYearMonth_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, converter));
    }

    @Test
    void toYearMonth_nonStringFrom_throwsClassCastException() {
        // GIVEN
        // Integer, not a String
        Object input = 12345;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toYearMonth(input, converter));
    }

    @Test
    void toYearMonth_emptyString_returnsNull() {
        // GIVEN
        String input = "";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // WHEN
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // THEN
            assertNull(result);
        }
    }

    @Test
    void toYearMonth_whitespaceString_returnsNull() {
        // GIVEN
        String input = "   ";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // WHEN
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // THEN
            assertNull(result);
        }
    }

    @Test
    void toYearMonth_converterWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        String input = "2023/07";
        // simulate missing ZoneId
        when(options.getZoneId()).thenReturn(null);
        // YearMonth.parse will fail, then DateUtilities.parseDate will be called with null ZoneId.
        // We let the static method throw NPE (as it would in real code).
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), isNull(), eq(true))).thenThrow(new NullPointerException("ZoneId is null"));
            // WHEN / THEN
            NullPointerException ex = assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, converter));
            assertTrue(ex.getMessage().contains("ZoneId is null"));
        }
    }

    @Test
    void toYearMonth_nullConverter_throwsNullPointerException() {
        // GIVEN
        String input = "2023/07";
        // YearMonth.parse fails; the method will attempt converter.getOptions() -> NPE
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, null));
    }

    @Test
    void toYearMonth_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        String input = "2023/07";
        // Options is null
        when(converter.getOptions()).thenReturn(null);
        // YearMonth.parse fails, then fallback tries options.getZoneId() -> NPE
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, converter));
    }

    @Test
    void toYearMonth_fallbackZonedDateTimeDecember() {
        // GIVEN
        String input = "2023-12-31T23:59:59+02:00";
        ZonedDateTime mockedZdt = ZonedDateTime.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), ZoneOffset.ofHours(2));
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(mockedZdt);
            // WHEN
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // THEN
            assertEquals(YearMonth.of(2023, 12), result);
        }
    }
}
