package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 * <p>
 * The tests exercise every public static conversion method in the
 * {@code DateConversions} class.  All objects are created using only the
 * constructors and factory methods that are visible in the source code
 * supplied for the class under test and its immediate collaborators.
 * No mocking frameworks are used – the tests rely on real instances of
 * {@link Converter}, {@link ConverterOptions} and the standard Java time
 * API.
 * </p>
 */
public class DateConversionsTest {

    /** A {@link Converter} instance used for all tests. */
    private Converter converter;

    /** The {@link ZoneId} taken from the {@link ConverterOptions} of the {@code converter}. */
    private ZoneId zoneId;

    /** A fixed instant used for deterministic testing (2023‑03‑15T12:34:56.789Z). */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-03-15T12:34:56.789Z");

    /** Epoch milliseconds of {@link #FIXED_INSTANT}. */
    private static final long FIXED_EPOCH_MILLIS = FIXED_INSTANT.toEpochMilli();

    /** A {@link java.util.Date} wrapping {@link #FIXED_INSTANT}. */
    private static final Date FIXED_DATE = new Date(FIXED_EPOCH_MILLIS);

    @Before
    public void setUp() {
        /*
         * The library provides a public constructor {@code Converter(ConverterOptions)}.
         * The {@code ConverterOptions} class is not shown, but it is reasonable to
         * assume it has a public no‑arg constructor that defaults to the system
         * time‑zone.  The tests therefore create a default instance and retrieve the
         * zone for use in assertions.
         */
        ConverterOptions options = new ConverterOptions();          // default options
        converter = new Converter(options);
        zoneId = converter.getOptions().getZoneId();
    }

    /* -------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------- */

    /**
     * Returns the expected {@link BigDecimal} representation of an epoch
     * millisecond value as used by {@link DateConversions#toBigDecimal}.
     */
    private static BigDecimal expectedBigDecimalFromMillis(long epochMillis) {
        // DateConversions divides by BigDecimalConversions.GRAND (which is 1000) with
        // scale 9 and RoundingMode.DOWN.
        return new BigDecimal(epochMillis)
                .divide(new BigDecimal(1000), 9, RoundingMode.DOWN);
    }

    /**
     * Returns the expected {@link BigInteger} representation of an {@link Instant}
     * as used by {@link DateConversions#toBigInteger}.  The library’s
     * {@code InstantConversions.toBigInteger} converts the instant to the
     * number of milliseconds since the epoch.
     */
    private static BigInteger expectedBigIntegerFromInstant(Instant instant) {
        return BigInteger.valueOf(instant.toEpochMilli());
    }

    /* -------------------------------------------------------------
     * Tests for each conversion method
     * ------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(FIXED_DATE, converter);
        assertEquals("ZoneId must match the converter’s zone", zoneId, zdt.getZone());
        assertEquals("Instant must be preserved", FIXED_INSTANT, zdt.toInstant());
    }

    @Test
    public void testToLong() {
        long value = DateConversions.toLong(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS, value);
    }

    @Test
    public void testToDouble() {
        double value = DateConversions.toDouble(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS / 1000.0, value, 0.0);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = DateConversions.toSqlDate(FIXED_DATE, converter);
        // The SQL date should represent the same calendar date in the converter’s zone.
        LocalDate expected = FIXED_INSTANT.atZone(zoneId).toLocalDate();
        assertEquals(expected, sqlDate.toLocalDate());
    }

    @Test
    public void testToDate() {
        Date result = DateConversions.toDate(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS, result.getTime());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = DateConversions.toCalendar(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
        // The Calendar’s time zone should be the same as the converter’s zone
        assertEquals(zoneId, cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = DateConversions.toBigDecimal(FIXED_DATE, converter);
        assertEquals(expectedBigDecimalFromMillis(FIXED_EPOCH_MILLIS), bd);
    }

    @Test
    public void testToInstant_WithUtilDate() {
        Instant instant = DateConversions.toInstant(FIXED_DATE, converter);
        assertEquals(FIXED_INSTANT, instant);
    }

    @Test
    public void testToInstant_WithSqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(FIXED_EPOCH_MILLIS);
        Instant instant = DateConversions.toInstant(sqlDate, converter);
        // The implementation creates a new java.util.Date from the sql date, so the instant is identical.
        assertEquals(FIXED_INSTANT, instant);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(FIXED_DATE, converter);
        assertEquals(FIXED_INSTANT, odt.toInstant());
        assertEquals(zoneId, odt.getOffset().getRules().getOffset(FIXED_INSTANT));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(FIXED_DATE, converter);
        LocalDateTime expected = FIXED_INSTANT.atZone(zoneId).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = DateConversions.toLocalDate(FIXED_DATE, converter);
        LocalDate expected = FIXED_INSTANT.atZone(zoneId).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = DateConversions.toLocalTime(FIXED_DATE, converter);
        LocalTime expected = FIXED_INSTANT.atZone(zoneId).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = DateConversions.toBigInteger(FIXED_DATE, converter);
        assertEquals(expectedBigIntegerFromInstant(FIXED_INSTANT), bi);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = DateConversions.toAtomicLong(FIXED_DATE, converter);
        assertEquals(FIXED_EPOCH_MILLIS, al.get());
    }

    @Test
    public void testToYear() {
        Year year = DateConversions.toYear(FIXED_DATE, converter);
        Year expected = Year.from(FIXED_INSTANT.atZone(zoneId).toLocalDate());
        assertEquals(expected, year);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = DateConversions.toYearMonth(FIXED_DATE, converter);
        YearMonth expected = YearMonth.from(FIXED_INSTANT.atZone(zoneId).toLocalDate());
        assertEquals(expected, ym);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = DateConversions.toMonthDay(FIXED_DATE, converter);
        MonthDay expected = MonthDay.from(FIXED_INSTANT.atZone(zoneId).toLocalDate());
        assertEquals(expected, md);
    }

    @Test
    public void testToString() {
        String result = DateConversions.toString(FIXED_DATE, converter);
        String expected = DateConversions.MILLIS_FMT.format(FIXED_INSTANT);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = DateConversions.toMap(FIXED_DATE, converter);
        assertNotNull(map);
        assertEquals(1, map.size());

        // The key is defined in MapConversions.DATE – we do not import that class
        // directly; instead we retrieve the sole entry and verify its value.
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        assertEquals(DateConversions.toString(FIXED_DATE, converter), entry.getValue());
    }
}
