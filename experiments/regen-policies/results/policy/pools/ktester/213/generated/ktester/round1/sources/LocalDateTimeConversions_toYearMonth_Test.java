package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toYearMonth_Test {

    @Mock
    private Converter // mocked Converter
    mockConverter;

    @Mock
    private ConverterOptions // mocked ConverterOptions returned by the Converter
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: Converter -> ConverterOptions -> ZoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify no unexpected interactions with the default mocks.
        verifyNoMoreInteractions(mockConverter, mockOptions);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Normal conversion – UTC zone", LocalDateTime.of(2023, 5, 15, 10, 30, 45), ZoneId.of("UTC"), YearMonth.of(2023, 5)), Arguments.of("Edge of year – America/New_York zone", LocalDateTime.of(1999, 12, 31, 23, 59, 59), ZoneId.of("America/New_York"), YearMonth.of(1999, 12)), Arguments.of("Leap day – UTC", LocalDateTime.of(2020, 2, 29, 12, 0, 0), ZoneId.of("UTC"), YearMonth.of(2020, 2)), Arguments.of("Non‑leap February – UTC", LocalDateTime.of(2019, 2, 28, 23, 59, 59), ZoneId.of("UTC"), YearMonth.of(2019, 2)), Arguments.of("Maximum supported ISO year – UTC", LocalDateTime.of(Year.MAX_VALUE, 12, 31, 23, 59, 59), ZoneId.of("UTC"), YearMonth.of(Year.MAX_VALUE, 12)), Arguments.of("Minimum supported ISO year – UTC", LocalDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0), ZoneId.of("UTC"), YearMonth.of(Year.MIN_VALUE, 1)), Arguments.of("Custom offset zone (UTC+05:30) – should not affect YearMonth", LocalDateTime.of(2023, 3, 31, 23, 30, 0), ZoneOffset.ofHoursMinutes(5, 30), YearMonth.of(2023, 3)), Arguments.of("Negative year handling – UTC", LocalDateTime.of(-44, 5, 15, 10, 0, 0), ZoneId.of("UTC"), YearMonth.of(-44, 5)), Arguments.of("Nanosecond precision ignored – UTC", LocalDateTime.of(2023, 8, 20, 15, 45, 30, 123_456_789), ZoneId.of("UTC"), YearMonth.of(2023, 8)));
    }

    @Test
    void toYearMonth_convertsLocalDateTimeUsingConverterZoneId() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, 5, 15, 10, 20, 30);
        // WHEN
        YearMonth result = LocalDateTimeConversions.toYearMonth(source, mockConverter);
        // THEN
        assertEquals(YearMonth.of(2023, 5), result, "The YearMonth derived from the LocalDateTime should match the year and month components.");
        // Verify the expected interactions with the mocks.
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toYearMonth_withDifferentZoneId_stillReturnsCorrectYearMonth() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        LocalDateTime source = LocalDateTime.of(2021, 12, 31, 23, 45);
        // WHEN
        YearMonth result = LocalDateTimeConversions.toYearMonth(source, mockConverter);
        // THEN
        assertEquals(YearMonth.of(2021, 12), result, "YearMonth should be based on the date part of the LocalDateTime irrespective of the zone.");
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    /* -------------------------------------------------------------
     *  Valid conversions – parameterized
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toYearMonth_validConversions(String description, LocalDateTime source, ZoneId zone, YearMonth expected) {
        // configure zone for this iteration
        when(mockOptions.getZoneId()).thenReturn(zone);
        YearMonth result = LocalDateTimeConversions.toYearMonth(source, mockConverter);
        assertEquals(expected, result, description);
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    /* -------------------------------------------------------------
     *  Invalid "from" type – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toYearMonth_invalidFrom_string() {
        String badInput = "2023-05-15T10:30:45";
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toYearMonth(badInput, mockConverter), "A String should cause a ClassCastException.");
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toYearMonth_invalidFrom_localDate() {
        LocalDate badInput = LocalDate.of(2023, 5, 15);
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toYearMonth(badInput, mockConverter), "A LocalDate should cause a ClassCastException.");
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toYearMonth_invalidFrom_timestamp() {
        Timestamp badInput = Timestamp.valueOf("2023-05-15 10:30:45");
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toYearMonth(badInput, mockConverter), "A Timestamp should cause a ClassCastException.");
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    /* -------------------------------------------------------------
     *  Null arguments
     * ------------------------------------------------------------- */
    @Test
    void toYearMonth_nullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYearMonth(null, mockConverter), "Null 'from' should result in NullPointerException.");
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toYearMonth_nullConverter_throwsNPE() {
        LocalDateTime source = LocalDateTime.now();
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYearMonth(source, null), "Null 'converter' should result in NullPointerException.");
        // No interaction verification needed because converter is null.
    }

    /* -------------------------------------------------------------
     *  Converter mis‑configuration
     * ------------------------------------------------------------- */
    @Test
    void toYearMonth_converterReturnsNullOptions_throwsNPE() {
        // a dedicated mock that returns null for getOptions()
        Converter badConverter = mock(Converter.class);
        when(badConverter.getOptions()).thenReturn(null);
        LocalDateTime source = LocalDateTime.of(2022, 1, 1, 0, 0);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYearMonth(source, badConverter), "Null options should cause NullPointerException.");
        verify(badConverter).getOptions();
    }

    @Test
    void toYearMonth_optionsWithNullZoneId_throwsNPE() {
        // mock options that returns null for getZoneId()
        ConverterOptions badOptions = mock(ConverterOptions.class);
        when(badOptions.getZoneId()).thenReturn(null);
        when(mockConverter.getOptions()).thenReturn(badOptions);
        LocalDateTime source = LocalDateTime.of(2022, 1, 1, 0, 0);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYearMonth(source, mockConverter), "Null zoneId should cause NullPointerException.");
        verify(mockConverter).getOptions();
        verify(badOptions).getZoneId();
    }

    @Test
    void toYearMonth_converterThrowsOnGetOptions_propagatesException() {
        Converter explodingConverter = mock(Converter.class);
        when(explodingConverter.getOptions()).thenThrow(new IllegalStateException("boom"));
        LocalDateTime source = LocalDateTime.of(2023, 1, 1, 0, 0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toYearMonth(source, explodingConverter), "Exception from getOptions() must propagate.");
        assertEquals("boom", ex.getMessage());
        verify(explodingConverter).getOptions();
    }

    @Test
    void toYearMonth_optionsThrowsOnGetZoneId_propagatesException() {
        ConverterOptions explodingOptions = mock(ConverterOptions.class);
        when(explodingOptions.getZoneId()).thenThrow(new IllegalStateException("zone failure"));
        when(mockConverter.getOptions()).thenReturn(explodingOptions);
        LocalDateTime source = LocalDateTime.of(2023, 1, 1, 0, 0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toYearMonth(source, mockConverter), "Exception from getZoneId() must propagate.");
        assertEquals("zone failure", ex.getMessage());
        verify(mockConverter).getOptions();
        verify(explodingOptions).getZoneId();
    }
}
