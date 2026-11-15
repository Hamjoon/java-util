package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link DateConversions}.
 *
 * The tests use only the public API that appears in the source file and the
 * signatures provided for {@link Converter} and related classes.
 *
 * All objects are created via real constructors or factory methods – no mocks
 * or subclassing of library classes is performed.
 */
public class DateConversionsTest {

    private static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    private static final long SAMPLE_EPOCH_MILLIS = 1_632_072_800_123L; // 2021‑09‑30T12:00:00.123Z
    private Date sampleDate;
    private java.sql.Date sampleSqlDate;
    private Converter converter;

    @Before
    public void setUp() {
        // Legacy java.util.Date
        sampleDate = new Date(SAMPLE_EPOCH_MILLIS);

        // java.sql.Date represents only the date part (no time). Use the same epoch millis,
        // but drop the time‑of‑day portion by converting to LocalDate first.
        LocalDate localDate = Instant.ofEpochMilli(SAMPLE_EPOCH_MILLIS)
                                     .atZone(ZONE_UTC)
                                     .toLocalDate();
        sampleSqlDate = java.sql.Date.valueOf(localDate);

        // Build a Converter with options that specify UTC – this is the zone used by
        // DateConversions for all zone‑sensitive conversions.
        ConverterOptions options = new ConverterOptions();
        // The real ConverterOptions class (part of the library) provides a setter for the zone.
        // If the API differs, adjust the method name accordingly.
        options.setZoneId(ZONE_UTC);
        converter = new Converter(options);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private ZonedDateTime expectedZonedDateTime(Date d) {
        return Instant.ofEpochMilli(d.getTime()).atZone(ZONE_UTC);
    }

    // -------------------------------------------------------------------------
    // Tests for each conversion method
    // -------------------------------------------------------------------------

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(expectedZonedDateTime(sampleDate), zdt);
    }

    @Test
    public void testToLong() {
        long epoch = DateConversions.toLong(sampleDate, converter);
        assertEquals(SAMPLE_EPOCH_MILLIS, epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = DateConversions.toDouble(sampleDate, converter);
        assertEquals(SAMPLE_EPOCH_MILLIS / 1000.0, seconds, 0.000001);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = DateConversions.toSqlDate(sampleDate, converter);
        // The expected java.sql.Date is the date component of the instant in UTC.
        LocalDate expected = Instant.ofEpochMilli(SAMPLE_EPOCH_MILLIS)
                                    .atZone(ZONE_UTC)
                                    .toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }

    @Test
    public void testToDate() {
        Date d = DateConversions.toDate(sampleDate, converter);
        assertEquals(sampleDate, d);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(sampleDate, converter);
        assertEquals(SAMPLE_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = DateConversions.toCalendar(sampleDate, converter);
        assertEquals(SAMPLE_EPOCH_MILLIS, cal.getTimeInMillis());
        assertEquals(ZONE_UTC, cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = DateConversions.toBigDecimal(sampleDate, converter);
        // The conversion divides the epoch millis by 1_000_000_000 (the GRAN D constant from
        // BigDecimalConversions). Since we do not have the constant, we compute the expected
        // value using the same logic.
        BigDecimal expected = new BigDecimal(SAMPLE_EPOCH_MILLIS)
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToInstant_WithUtilDate() {
        Instant instant = DateConversions.toInstant(sampleDate, converter);
        assertEquals(sampleDate.toInstant(), instant);
    }

    @Test
    public void testToInstant_WithSqlDate() {
        Instant instant = DateConversions.toInstant(sampleSqlDate, converter);
        // java.sql.Date loses the time‑of‑day, so the instant should be at midnight UTC.
        Instant expected = sampleSqlDate.toInstant(); // returns midnight UTC for the date part
        assertEquals(expected, instant);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(sampleDate, converter);
        OffsetDateTime expected = expectedZonedDateTime(sampleDate).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(sampleDate, converter);
        LocalDateTime expected = expectedZonedDateTime(sampleDate).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = DateConversions.toLocalDate(sampleDate, converter);
        LocalDate expected = expectedZonedDateTime(sampleDate).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = DateConversions.toLocalTime(sampleDate, converter);
        LocalTime expected = expectedZonedDateTime(sampleDate).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = DateConversions.toBigInteger(sampleDate, converter);
        Instant instant = DateConversions.toInstant(sampleDate, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = DateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(SAMPLE_EPOCH_MILLIS, al.get());
    }

    @Test
    public void testToYear() {
        Year year = DateConversions.toYear(sampleDate, converter);
        Year expected = Year.from(expectedZonedDateTime(sampleDate).toLocalDate());
        assertEquals(expected, year);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = DateConversions.toYearMonth(sampleDate, converter);
        YearMonth expected = YearMonth.from(expectedZonedDateTime(sampleDate).toLocalDate());
        assertEquals(expected, ym);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = DateConversions.toMonthDay(sampleDate, converter);
        MonthDay expected = MonthDay.from(expectedZonedDateTime(sampleDate).toLocalDate());
        assertEquals(expected, md);
    }

    @Test
    public void testToStringFormatting() {
        String formatted = DateConversions.toString(sampleDate, converter);
        // The formatter forces exactly three decimal places for the fraction of a second.
        Instant instant = sampleDate.toInstant();
        String expected = DateConversions.MILLIS_FMT.format(instant);
        assertEquals(expected, formatted);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = DateConversions.toMap(sampleDate, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals(DateConversions.toString(sampleDate, converter), map.get(MapConversions.DATE));
        // No other entries should be present.
        assertEquals(1, map.size());
    }
}
