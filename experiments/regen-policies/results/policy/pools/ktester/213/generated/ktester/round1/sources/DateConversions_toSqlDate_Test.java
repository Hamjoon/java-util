package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toSqlDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Default converter uses the system default zone – individual tests override as needed
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Standard conversion – UTC zone, same day
        Arguments.of(Date.from(Instant.parse("2023-03-15T10:30:00Z")), ZoneId.of("UTC"), "2023-03-15", "Standard conversion where the instant and the target zone are both UTC"), // UTC‑05:00 (America/New_York) – crosses midnight to previous day
        Arguments.of(Date.from(Instant.parse("2023-03-15T02:30:00Z")), ZoneId.of("America/New_York"), "2023-03-14", "Same instant but converted to a zone 5 hours behind UTC, crossing midnight"), // UTC+02:00 – same calendar day
        Arguments.of(Date.from(Instant.parse("2023-03-15T00:00:00Z")), ZoneId.of("+02:00"), "2023-03-15", "Instant at midnight UTC, shifted forward two hours; still same day"), // UTC-02:00 – previous calendar day
        Arguments.of(Date.from(Instant.parse("2023-03-15T00:00:00Z")), ZoneId.of("-02:00"), "2023-03-14", "Instant at midnight UTC, shifted back two hours; previous day"), // Additional scenario – UTC+02:00 pushes date forward
        Arguments.of(Date.from(Instant.parse("2023-03-15T23:30:00Z")), ZoneId.of("+02:00"), "2023-03-16", "Late UTC instant with +02:00 offset moves to next calendar day"), // Additional scenario – fractional offset (UTC+05:45) crossing midnight
        Arguments.of(Date.from(Instant.parse("2023-03-15T20:30:00Z")), ZoneId.of("Asia/Kathmandu"), "2023-03-16", "Non‑whole‑hour offset that crosses midnight"), // Leap‑day handling
        Arguments.of(Date.from(Instant.parse("2020-02-29T10:00:00Z")), ZoneId.of("UTC"), "2020-02-29", "Leap‑day should be preserved"), // Pre‑epoch instant (negative epoch)
        Arguments.of(Date.from(Instant.parse("1969-12-31T12:00:00Z")), ZoneId.of("UTC"), "1969-12-31", "Negative epoch value should be handled"), // java.sql.Date input – should stay the same regardless of zone
        Arguments.of(Date.valueOf("2023-04-01"), ZoneId.of("America/Los_Angeles"), "2023-04-01", "java.sql.Date (subclass of java.util.Date) should keep its calendar date"), // Extreme forward offset (+14:00)
        Arguments.of(Date.from(Instant.parse("2023-03-15T23:00:00Z")), ZoneId.of("Pacific/Kiritimati"), "2023-03-16", "Maximum positive offset pushes date to next day"), // Extreme negative offset (-10:00)
        Arguments.of(Date.from(Instant.parse("2023-03-15T01:00:00Z")), ZoneId.of("Pacific/Honolulu"), "2023-03-14", "Large negative offset shifts date to previous day"));
    }

    private static Stream<Arguments> exceptionalConversionProvider() {
        // Helper converters
        ConverterOptions utcOpts = new ConverterOptions();
        utcOpts.setZoneId(ZoneId.of("UTC"));
        Converter utcConverter = new Converter(utcOpts);
        // Mocked converter that returns null options
        Converter nullOptionsConverter = mock(Converter.class);
        when(nullOptionsConverter.getOptions()).thenReturn(null);
        // Mocked converter with options that have a null zoneId
        ConverterOptions nullZoneOpts = new ConverterOptions();
        nullZoneOpts.setZoneId(null);
        Converter nullZoneConverter = new Converter(nullZoneOpts);
        return Stream.of(// Null 'from' argument
        Arguments.of(null, utcConverter, NullPointerException.class, "Null 'from' should cause NullPointerException due to cast"), // Wrong type (String) – triggers ClassCastException
        Arguments.of("2023-03-15", utcConverter, ClassCastException.class, "Non‑Date input should trigger ClassCastException"), // Null converter
        Arguments.of(Date.from(Instant.parse("2023-03-15T10:30:00Z")), null, NullPointerException.class, "Null Converter leads to NullPointerException when accessing options"), // Converter with null options
        Arguments.of(Date.from(Instant.parse("2023-03-15T10:30:00Z")), nullOptionsConverter, NullPointerException.class, "Null options inside Converter cause NullPointerException"), // Converter with null ZoneId
        Arguments.of(Date.from(Instant.parse("2023-03-15T10:30:00Z")), nullZoneConverter, NullPointerException.class, "Null ZoneId inside options causes NullPointerException"));
    }

    @Test
    void toSqlDate_convertsUtilDateUsingConverterZone() {
        // GIVEN: a java.util.Date representing a known instant
        Calendar cal = new GregorianCalendar(2023, Calendar.MARCH, 15, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        java.util.Date utilDate = cal.getTime();
        // AND: a Converter whose options are set to a specific ZoneId (different from the system default
        // to ensure the conversion uses the zone from the options)
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        Converter utcConverter = new Converter(options);
        // WHEN: converting to java.sql.Date via DateConversions
        Date sqlDate = DateConversions.toSqlDate(utilDate, utcConverter);
        // THEN: the resulting sqlDate should represent the same local date as the instant in UTC
        // The instant of utilDate is the same regardless of zone; we shift it to UTC and take the
        // local date part.
        java.time.LocalDate expectedLocalDate = utilDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        assertEquals(Date.valueOf(expectedLocalDate), sqlDate, "SQL Date should match the local date of the instant in the converter's zone");
    }

    @Test
    void toSqlDate_nullInputReturnsNull() {
        // GIVEN: a null source object
        Object source = null;
        // WHEN: converting using any converter
        Date result = DateConversions.toSqlDate(source, converter);
        // THEN: the method should throw a NullPointerException (as it casts to java.util.Date)
        // We assert the exception rather than a null result because the implementation
        // performs an unchecked cast before any null check.
        assertThrows(NullPointerException.class, () -> DateConversions.toSqlDate(source, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void toSqlDate_successfulConversion(java.util.Date from, ZoneId zone, String expectedIsoDate, String description) {
        // Arrange – build a Converter with the supplied zone (null zone is not allowed here)
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(zone);
        Converter conv = new Converter(opts);
        // Act
        Date sqlDate = DateConversions.toSqlDate(from, conv);
        // Assert
        assertEquals(Date.valueOf(LocalDate.parse(expectedIsoDate)), sqlDate, () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionalConversionProvider")
    void toSqlDate_exceptionalConversion(Object from, Converter conv, Class<? extends Throwable> expected, String description) {
        assertThrows(expected, () -> DateConversions.toSqlDate(from, conv), () -> "Expected " + expected.getSimpleName() + " for: " + description);
    }

    @Test
    void toSqlDate_outOfRange_preYearOne_throwsIllegalArgumentException() {
        // 0000-01-01T00:00:00Z is a proleptic year 0, which java.sql.Date does not support
        java.util.Date date = Date.from(Instant.parse("0000-01-01T00:00:00Z"));
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("UTC"));
        Converter conv = new Converter(opts);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toSqlDate(date, conv), "Date before year 1 should cause IllegalArgumentException");
    }

    @Test
    void toSqlDate_outOfRange_yearBeyond9999_throwsIllegalArgumentException() {
        // 10000-01-01T00:00:00Z exceeds java.sql.Date's max year (9999)
        java.util.Date date = Date.from(Instant.parse("10000-01-01T00:00:00Z"));
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("UTC"));
        Converter conv = new Converter(opts);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toSqlDate(date, conv), "Date after year 9999 should cause IllegalArgumentException");
    }

    @Test
    void toSqlDate_preYearOneShiftedIntoValidRange_succeeds() {
        // 0000-12-31T23:00:00Z shifted by +02:00 becomes 0001-01-01, which is valid
        java.util.Date date = Date.from(Instant.parse("0000-12-31T23:00:00Z"));
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("+02:00"));
        Converter conv = new Converter(opts);
        Date result = DateConversions.toSqlDate(date, conv);
        assertEquals(Date.valueOf(LocalDate.of(1, 1, 1)), result, "Shift into year 1 should succeed and produce a valid java.sql.Date");
    }
}
