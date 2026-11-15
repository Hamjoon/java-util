package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class StringConversions_toMonthDay_Test {

    @Mock
    Converter mockConverter;

    @Mock
    ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the converter hierarchy used by StringConversions.toMonthDay
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void toMonthDay_ParsesIsoFormatDirectly() {
        // Given
        // valid MonthDay ISO format
        String iso = "--02-28";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            // When
            MonthDay result = StringConversions.toMonthDay(iso, mockConverter);
            // Then
            assertEquals(MonthDay.of(2, 28), result);
            // DateUtilities must not be invoked for a successful direct parse
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void toMonthDay_ParsesCustomPattern() {
        // Given
        // matches the MM_DD regex (dot matches any char)
        String pattern = "2.28";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            // When
            MonthDay result = StringConversions.toMonthDay(pattern, mockConverter);
            // Then
            assertEquals(MonthDay.of(2, 28), result);
            // regex path does not call DateUtilities
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void toMonthDay_UsesDateUtilitiesWhenPatternAndIsoFail() {
        // Given
        String dateTime = "2020-02-28T10:15:30Z";
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 28, 10, 15, 30, 0, ZoneOffset.UTC);
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(dateTime), any(ZoneId.class), eq(true))).thenReturn(zdt);
            // When
            MonthDay result = StringConversions.toMonthDay(dateTime, mockConverter);
            // Then
            assertEquals(MonthDay.of(2, 28), result);
            mocked.verify(() -> DateUtilities.parseDate(eq(dateTime), eq(ZoneId.of("UTC")), eq(true)), times(1));
        }
    }

    @Test
    void toMonthDay_ReturnsNullWhenDateUtilitiesReturnsNull() {
        // Given
        String unknown = "not-a-date";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(unknown), any(ZoneId.class), eq(true))).thenReturn(null);
            // When
            MonthDay result = StringConversions.toMonthDay(unknown, mockConverter);
            // Then
            assertNull(result);
            mocked.verify(() -> DateUtilities.parseDate(eq(unknown), eq(ZoneId.of("UTC")), eq(true)), times(1));
        }
    }

    @Test
    void toMonthDay_ThrowsIllegalArgumentExceptionWhenDateUtilitiesFails() {
        // Given
        String bad = "bad-input";
        RuntimeException inner = new RuntimeException("boom");
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(bad), any(ZoneId.class), eq(true))).thenThrow(inner);
            // When / Then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(bad, mockConverter));
            assertTrue(ex.getMessage().contains("Unable to extract Month-Day from string"));
            assertSame(inner, ex.getCause());
            mocked.verify(() -> DateUtilities.parseDate(eq(bad), eq(ZoneId.of("UTC")), eq(true)), times(1));
        }
    }

    @ParameterizedTest(name = "ISO string \"{0}\" → MonthDay {1}")
    @CsvSource({ "'--12-25', 12, 25", "'--01-01', 1, 1", "'--02-28', 2, 28" })
    void toMonthDay_ParsesIsoFormatMultiple(String iso, int month, int day) {
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            MonthDay result = StringConversions.toMonthDay(iso, mockConverter);
            assertEquals(MonthDay.of(month, day), result);
            // still no DateUtilities call
            mocked.verifyNoInteractions();
        }
    }

    @ParameterizedTest(name = "Pattern \"{0}\" → MonthDay {1}")
    @CsvSource({ "'12-25', 12, 25", "'3-7',   3, 7" })
    void toMonthDay_ParsesCustomPatternMultiple(String pattern, int month, int day) {
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            MonthDay result = StringConversions.toMonthDay(pattern, mockConverter);
            assertEquals(MonthDay.of(month, day), result);
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void toMonthDay_OutOfRangeViaRegex_ThrowsDateTimeException() {
        // Given: month 13 is invalid
        String badPattern = "13-01";
        // When / Then
        assertThrows(DateTimeException.class, () -> StringConversions.toMonthDay(badPattern, mockConverter), "MonthDay.of should throw DateTimeException for out‑of‑range month");
    }

    @Test
    void toMonthDay_InvalidDateStringWrappedInIllegalArgumentException() {
        // Given: a string that is not a MonthDay nor MM_DD and DateUtilities throws a parsing exception
        // February 30th does not exist
        String invalid = "2023-02-30";
        DateTimeParseException parseEx = new DateTimeParseException("Invalid date", invalid, 0);
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(invalid), any(ZoneId.class), eq(true))).thenThrow(parseEx);
            // When / Then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(invalid, mockConverter));
            assertTrue(ex.getMessage().contains("Unable to extract Month-Day from string"));
            assertSame(parseEx, ex.getCause());
        }
    }

    @Test
    void toMonthDay_NullFromParameter_ThrowsNullPointerException() {
        // Given
        Object nullFrom = null;
        // When / Then
        assertThrows(NullPointerException.class, () -> StringConversions.toMonthDay(nullFrom, mockConverter), "Casting null to String should cause NPE inside MonthDay.parse");
    }
}
