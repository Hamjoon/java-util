package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>normal conversions for a typical {@link LocalDate}</li>
 *   <li>edge cases such as {@code null} input (expected {@link ClassCastException})</li>
 *   <li>verification that the {@code Converter} options (zone / time‑zone) are honoured</li>
 *   <li>numeric conversions that depend on epoch calculations</li>
 *   <li>object‑graph conversions (String, Map, Timestamp, Calendar, etc.)</li>
 * </ul>
 *
 * <p>All objects are created via public constructors or factory methods that appear in the supplied
 * source code – no mocking or subclassing of library classes is performed.
 */
public class LocalDateConversionsTest {

    /** A {@link Converter} instance using the system default zone and time‑zone. */
    private Converter converter;

    /** A sample date – 2023‑03‑15 (chosen because it is not a leap‑year day). */
    private static final LocalDate SAMPLE_DATE = LocalDate.of(2023, 3, 15);

    /** The epoch milliseconds corresponding to {@code SAMPLE_DATE} at midnight in the converter's zone. */
    private long expectedEpochMilli;

    @Before
    public void setUp() {
        // The Converter API only exposes a constructor taking ConverterOptions.
        // The concrete ConverterOptions class is part of the library; we assume it has a
        // public no‑arg constructor that defaults to the system zone and time‑zone.
        converter = new Converter(new ConverterOptions());

        // Compute the expected epoch milli for the sample date at 00:00:00 in the converter's zone.
        ZoneId zone = converter.getOptions().getZoneId();
        ZonedDateTime zdt = SAMPLE_DATE.atStartOfDay(zone);
        expectedEpochMilli = zdt.toInstant().toEpochMilli();
    }

    /* -------------------------------------------------------------
     *  toInstant / toLong / toDouble
     * ------------------------------------------------------------- */
    @Test
    public void testToInstantAndRelatedNumericConversions() {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        assertEquals("Instant must match expected epoch milli", expectedEpochMilli, instant.toEpochMilli());

        long asLong = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        assertEquals("toLong must return the same epoch milli", expectedEpochMilli, asLong);

        double asDouble = LocalDateConversions.toDouble(SAMPLE_DATE, converter);
        assertEquals("toDouble must be epoch seconds (millis / 1000)",
                expectedEpochMilli / 1000d, asDouble, 0.000001);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstantWithInvalidInput() {
        LocalDateConversions.toInstant("not a LocalDate", converter);
    }

    /* -------------------------------------------------------------
     *  toLocalDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(SAMPLE_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        LocalDateTime expected = SAMPLE_DATE.atStartOfDay(zone).toLocalDateTime();
        assertEquals("LocalDateTime should be midnight of the sample date in the converter zone", expected, ldt);
    }

    /* -------------------------------------------------------------
     *  toZonedDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(SAMPLE_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, zone);
        assertEquals("ZonedDateTime must reflect the converter's ZoneId", expected, zdt);
    }

    /* -------------------------------------------------------------
     *  toOffsetDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(SAMPLE_DATE, converter);
        TimeZone tz = converter.getOptions().getTimeZone();
        int offsetSeconds = tz.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = OffsetDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, offset);
        assertEquals("OffsetDateTime must use the converter's TimeZone offset", expected, odt);
    }

    /* -------------------------------------------------------------
     *  toAtomicLong
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateConversions.toAtomicLong(SAMPLE_DATE, converter);
        assertEquals("AtomicLong value must equal epoch milli", expectedEpochMilli, atomic.get());
    }

    /* -------------------------------------------------------------
     *  toTimestamp
     * ------------------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateConversions.toTimestamp(SAMPLE_DATE, converter);
        long expected = SAMPLE_DATE.toEpochDay() * 86400L * 1000L;
        assertEquals("Timestamp must represent midnight of the LocalDate (UTC based)", expected, ts.getTime());
    }

    /* -------------------------------------------------------------
     *  toCalendar
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateConversions.toCalendar(SAMPLE_DATE, converter);
        assertEquals("Calendar must be created with the converter's TimeZone",
                converter.getOptions().getTimeZone(), cal.getTimeZone());

        assertEquals("Calendar time in millis must equal expected epoch milli",
                expectedEpochMilli, cal.getTimeInMillis());
    }

    /* -------------------------------------------------------------
     *  toSqlDate and toDate
     * ------------------------------------------------------------- */
    @Test
    public void testToSqlDateAndToDate() {
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(SAMPLE_DATE, converter);
        Date utilDate = LocalDateConversions.toDate(SAMPLE_DATE, converter);

        assertEquals("java.sql.Date should hold the same epoch milli as util Date", expectedEpochMilli, sqlDate.getTime());
        assertEquals("java.util.Date should hold the same epoch milli as util Date", expectedEpochMilli, utilDate.getTime());
    }

    /* -------------------------------------------------------------
     *  toBigInteger / toBigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void testToBigIntegerAndBigDecimal() {
        // The InstantConversions class is not part of the supplied source, but we can
        // rely on its contract: it converts an Instant to a numeric representation
        // based on epoch milliseconds.
        BigInteger bi = LocalDateConversions.toBigInteger(SAMPLE_DATE, converter);
        assertEquals("BigInteger must equal epoch milli", BigInteger.valueOf(expectedEpochMilli), bi);

        BigDecimal bd = LocalDateConversions.toBigDecimal(SAMPLE_DATE, converter);
        assertEquals("BigDecimal must equal epoch milli as decimal", BigDecimal.valueOf(expectedEpochMilli), bd);
    }

    /* -------------------------------------------------------------
     *  toString
     * ------------------------------------------------------------- */
    @Test
    public void testToString() {
        String str = LocalDateConversions.toString(SAMPLE_DATE, converter);
        assertEquals("String representation must follow ISO_LOCAL_DATE", SAMPLE_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE), str);
    }

    /* -------------------------------------------------------------
     *  toMap
     * ------------------------------------------------------------- */
    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(SAMPLE_DATE, converter);
        assertNotNull("Resulting map must not be null", map);
        assertTrue("Map must contain the DATE key", map.containsKey(MapConversions.DATE));
        assertEquals("Map value for DATE must be the LocalDate's toString()", SAMPLE_DATE.toString(), map.get(MapConversions.DATE));
        // Verify that the map preserves insertion order (CompactMap builder was called with insertionOrder())
        // Since we only put one entry, size check suffices.
        assertEquals("Map should contain exactly one entry", 1, map.size());
    }

    /* -------------------------------------------------------------
     *  Null handling – every conversion should throw ClassCastException
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testNullInputToInstant() {
        LocalDateConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNullInputToZonedDateTime() {
        LocalDateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNullInputToOffsetDateTime() {
        LocalDateConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNullInputToString() {
        LocalDateConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNullInputToMap() {
        LocalDateConversions.toMap(null, converter);
    }
}
