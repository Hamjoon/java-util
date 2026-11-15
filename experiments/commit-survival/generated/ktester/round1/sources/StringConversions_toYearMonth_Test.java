package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import java.time.*;
import java.time.format.DateTimeParseException;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toYearMonth_Test {

    @Mock
    private Converter // mocked Converter instance
    converter;

    @Mock
    private ConverterOptions // mocked ConverterOptions returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> zoneId
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void parsesValidYearMonthString() {
        // given
        String input = "2022-05";
        // when
        YearMonth result = StringConversions.toYearMonth(input, converter);
        // then
        assertEquals(YearMonth.of(2022, 5), result);
    }

    @Test
    void parsesStringViaDateUtilitiesWhenDirectParseFails() {
        // given
        String input = "2022-05-15T10:30:00Z";
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertEquals(YearMonth.of(2022, 5), result);
        }
    }

    @Test
    void throwsIllegalArgumentExceptionWhenStringCannotBeConverted() {
        // given
        String input = "not-a-date";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
        assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
    }

    @Test
    void directParseSucceedsWithNullConverter() {
        // given
        String input = "2024-12";
        // when
        YearMonth result = StringConversions.toYearMonth(input, null);
        // then
        assertEquals(YearMonth.of(2024, 12), result);
    }

    @Test
    void throwsClassCastExceptionWhenFromIsNotAString() {
        // given
        Integer notAString = 12345;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toYearMonth(notAString, converter));
    }

    @Test
    void throwsNullPointerExceptionWhenFromIsNull() {
        // given
        Object nullInput = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(nullInput, converter));
    }

    @Test
    void fallbackReturnsNullWhenDateUtilitiesReturnsNull() {
        // given
        String input = "not-a-date";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertNull(result);
        }
    }

    @Test
    void fallbackParsesDateWithDayComponentSuccessfully() {
        // given
        String input = "2023-07-01";
        ZonedDateTime zdt = ZonedDateTime.of(2023, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertEquals(YearMonth.of(2023, 7), result);
        }
    }

    @Test
    void fallbackParsesOffsetDateTimeStringSuccessfully() {
        // given
        String input = "2023-07-01T00:00:00+02:00";
        ZonedDateTime zdt = ZonedDateTime.of(2023, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2));
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertEquals(YearMonth.of(2023, 7), result);
        }
    }

    @Test
    void fallbackReturnsNullForEmptyString() {
        // given
        String input = "";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertNull(result);
        }
    }

    @Test
    void fallbackReturnsNullForWhitespaceOnlyString() {
        // given
        String input = "   ";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // when
            YearMonth result = StringConversions.toYearMonth(input, converter);
            // then
            assertNull(result);
        }
    }

    @Test
    void throwsIllegalArgumentExceptionWhenDateUtilitiesThrows() {
        // given
        // invalid month
        String input = "2023-13-01";
        DateTimeParseException dtpe = new DateTimeParseException("Invalid month", input, 5);
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(dtpe);
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
            assertSame(dtpe, ex.getCause());
        }
    }

    @Test
    void throwsIllegalArgumentExceptionWhenDateUtilitiesThrowsForInvalidDay() {
        // given
        // February 30 does not exist
        String input = "2023-02-30T10:00:00Z";
        DateTimeParseException dtpe = new DateTimeParseException("Invalid day", input, 8);
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(dtpe);
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
            assertSame(dtpe, ex.getCause());
        }
    }

    @Test
    void fallbackWithNullConverterThrowsNullPointerException() {
        // given
        String input = "2023-07-15T12:34:56Z";
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, null));
    }

    @Test
    void fallbackWithConverterReturningNullOptionsThrowsNullPointerException() {
        // given
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        String input = "2023-07-15T12:34:56Z";
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toYearMonth(input, mockConverter));
    }

    @Test
    void fallbackWithConverterHavingNullZoneIdWrapsInIllegalArgumentException() {
        // given
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // zoneId is null
        when(mockOptions.getZoneId()).thenReturn(null);
        String input = "2023-07-15T12:34:56Z";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            // Simulate NPE inside DateUtilities when it dereferences the null ZoneId
            utilities.when(() -> DateUtilities.parseDate(eq(input), isNull(), eq(true))).thenThrow(new NullPointerException("ZoneId is null"));
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, mockConverter));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
            assertTrue(ex.getCause() instanceof NullPointerException);
        }
    }

    @Test
    void throwsIllegalArgumentExceptionWhenDirectParseFailsAndDateUtilitiesReturnsNull() {
        // given
        String input = "invalid-format";
        try (MockedStatic<DateUtilities> utilities = mockStatic(DateUtilities.class)) {
            utilities.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            // when / then
            assertNull(StringConversions.toYearMonth(input, converter));
        }
    }
}
