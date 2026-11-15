package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toYearMonth_Test {

    /**
     * Converter used for all tests – created fresh for each test to avoid state leakage.
     */
    private Converter converter;

    /**
     * A mock of {@link Converter} that we can program to return a desired {@link ConverterOptions}.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A mock of {@link ConverterOptions} that supplies the {@link ZoneId} used by the conversion.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options; the default zone is the system default zone.
        converter = new Converter(new ConverterOptions());
        // Ensure a clean state before each test.
        reset(mockConverter, mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no test accidentally re‑uses a stale instance.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid conversion – UTC zone
        Arguments.of("UTC zone, 2023‑05‑15T10:00:00Z", Date.from(Instant.parse("2023-05-15T10:00:00Z")), ZoneId.of("UTC"), YearMonth.of(2023, 5)), // Non‑UTC zone causing month rollover (UTC‑08 → previous month)
        Arguments.of("America/Los_Angeles, 2023‑03‑01T01:00:00Z → 2023‑02‑28", Date.from(Instant.parse("2023-03-01T01:00:00Z")), ZoneId.of("America/Los_Angeles"), YearMonth.of(2023, 2)), // Positive offset causing month forward rollover (UTC+01 → next month)
        Arguments.of("Europe/Berlin, 2023‑01‑31T23:00:00Z → 2023‑02‑01", Date.from(Instant.parse("2023-01-31T23:00:00Z")), ZoneId.of("Europe/Berlin"), YearMonth.of(2023, 2)), // Epoch start (1970‑01‑01) UTC
        Arguments.of("UTC zone, epoch start", new Date(0L), ZoneId.of("UTC"), YearMonth.of(1970, 1)), // Far‑future date
        Arguments.of("UTC zone, 9999‑12‑31T23:59:59Z", Date.from(Instant.parse("9999-12-31T23:59:59Z")), ZoneId.of("UTC"), YearMonth.of(9999, 12)), // Negative epoch with offset (America/New_York)
        Arguments.of("America/New_York, 1969‑12‑31T23:30:00Z → 1969‑12‑31 18:30‑05:00", Date.from(Instant.parse("1969-12-31T23:30:00Z")), ZoneId.of("America/New_York"), YearMonth.of(1969, 12)), // java.sql.Timestamp (subclass of Date)
        Arguments.of("UTC zone, java.sql.Timestamp 2022‑08‑15T12:34:56Z", new Timestamp(Instant.parse("2022-08-15T12:34:56Z").toEpochMilli()), ZoneId.of("UTC"), YearMonth.of(2022, 8)), // Daylight‑saving transition (America/New_York)
        Arguments.of("America/New_York, 2023‑03‑12T07:30:00Z → 2023‑03‑12 02:30‑05:00", Date.from(Instant.parse("2023-03-12T07:30:00Z")), ZoneId.of("America/New_York"), YearMonth.of(2023, 3)));
    }

    @Test
    void toYearMonth_convertsDateToCorrectYearMonth() {
        // GIVEN – a java.util.Date representing 2023‑05‑15 10:30:45 in the default time‑zone
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2023);
        // months are 0‑based in Calendar
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 30);
        cal.set(Calendar.SECOND, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        // WHEN – converting the Date to a YearMonth
        YearMonth result = DateConversions.toYearMonth(date, converter);
        // THEN – the YearMonth should represent May 2023
        YearMonth expected = YearMonth.of(2023, 5);
        assertEquals(expected, result, () -> "Expected YearMonth " + expected + " but got " + result);
    }

    @Test
    void toYearMonth_usesConverterZoneId() {
        // GIVEN – a date at the very end of a day in UTC, but the converter is set to a
        //         different zone (e.g., America/New_York) which moves the date back one day.
        Calendar utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        // 2023‑01‑01T23:00:00Z
        utcCal.set(2023, Calendar.JANUARY, 1, 23, 0, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        Date utcDate = utcCal.getTime();
        // Create ConverterOptions with a specific ZoneId (America/New_York is UTC‑5)
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(java.time.ZoneId.of("America/New_York"));
        Converter nyConverter = new Converter(options);
        // WHEN – converting using the New York zone
        YearMonth result = DateConversions.toYearMonth(utcDate, nyConverter);
        // THEN – the instant falls on 2023‑01‑01 18:00 in NY, which is still Jan 2023
        // (the date does not cross a month boundary, but we assert the exact month/year)
        YearMonth expected = YearMonth.of(2023, 1);
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_nullDate_throwsNullPointerException() {
        // GIVEN – a null source
        Date nullDate = null;
        // WHEN / THEN – the method should throw NPE because it casts to Date before checking.
        assertThrows(NullPointerException.class, () -> DateConversions.toYearMonth(nullDate, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toYearMonth_validConversions(String description, Date from, ZoneId zone, YearMonth expected) {
        // GIVEN – a converter that returns the supplied zone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN – the conversion is performed
        YearMonth result = DateConversions.toYearMonth(from, mockConverter);
        // THEN – the result matches the expected YearMonth
        assertEquals(expected, result, () -> "Failed conversion: " + description + ". Expected " + expected + " but got " + result);
    }

    @Test
    void toYearMonth_nullFrom_throwsNullPointerException() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toYearMonth(null, mockConverter), "A null 'from' argument should result in NullPointerException");
    }

    @Test
    void toYearMonth_invalidFromType_throwsClassCastException() {
        // GIVEN – a non‑Date object (String)
        Object invalidFrom = "2023-05-15";
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toYearMonth(invalidFrom, mockConverter), "Passing a non‑Date object should cause ClassCastException");
    }

    @Test
    void toYearMonth_nullConverter_throwsNullPointerException() {
        // GIVEN – a valid Date but a null converter
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toYearMonth(date, null), "A null converter should cause NullPointerException when its options are accessed");
    }

    @Test
    void toYearMonth_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN – a converter whose getOptions() returns null
        when(mockConverter.getOptions()).thenReturn(null);
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toYearMonth(date, mockConverter), "Null options inside the converter should trigger NullPointerException");
    }

    @Test
    void toYearMonth_optionsWithNullZoneId_throwsNullPointerException() {
        // GIVEN – options that return a null ZoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(null);
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toYearMonth(date, mockConverter), "A null ZoneId inside the options should cause NullPointerException");
    }
}
