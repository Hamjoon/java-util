package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class ZonedDateTimeConversionsTest {

    private ZonedDateTime utcZdt;
    private ZonedDateTime nyZdt;
    private Converter utcConverter;
    private Converter nyConverter;

    /**
     * Helper that creates a {@link Converter} whose {@link ConverterOptions}
     * are configured to the supplied {@link ZoneId}.  The library does not
     * expose a public mutator for the zone, therefore reflection is used to
     * set the private fields directly – this is acceptable for a unit‑test
     * where we need a real, non‑mocked {@code Converter}.
     */
    private Converter createConverter(ZoneId zoneId) throws Exception {
        // Build a default options instance (the constructor is public in the library)
        ConverterOptions options = new ConverterOptions();

        // The real library stores the zone in a field named "zoneId" and the
        // time‑zone in a field named "timeZone".  Use reflection to set them.
        Field zoneField = ConverterOptions.class.getDeclaredField("zoneId");
        zoneField.setAccessible(true);
        zoneField.set(options, zoneId);

        Field tzField = ConverterOptions.class.getDeclaredField("timeZone");
        tzField.setAccessible(true);
        tzField.set(options, TimeZone.getTimeZone(zoneId));

        // Construct the Converter with these options
        return new Converter(options);
    }

    @Before
    public void setUp() throws Exception {
        // A fixed instant for reproducibility
        Instant instant = Instant.parse("2023-03-15T12:34:56.123Z");

        // ZonedDateTime in UTC
        utcZdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);

        // Same instant, but in New York (America/New_York)
        nyZdt = ZonedDateTime.ofInstant(instant, ZoneId.of("America/New_York"));

        utcConverter = createConverter(ZoneOffset.UTC);
        nyConverter  = createConverter(ZoneId.of("America/New_York"));
    }

    /* -------------------------------------------------------------
     *  toLong
     * ------------------------------------------------------------- */
    @Test
    public void testToLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(utcZdt, utcConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWithWrongType() {
        ZonedDateTimeConversions.toLong("not a ZonedDateTime", utcConverter);
    }

    /* -------------------------------------------------------------
     *  toDouble
     * ------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        ZonedDateTime zdt = utcZdt;
        Instant instant = zdt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, ZonedDateTimeConversions.toDouble(zdt, utcConverter), 0.000000001);
    }

    /* -------------------------------------------------------------
     *  toInstant
     * ------------------------------------------------------------- */
    @Test
    public void testToInstant() {
        Instant expected = nyZdt.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(nyZdt, nyConverter));
    }

    /* -------------------------------------------------------------
     *  toLocalDateTime (zone conversion)
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDateTimeSameZone() {
        // converter zone = UTC, same as utcZdt
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(utcZdt, utcConverter);
        assertEquals(utcZdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDateTimeDifferentZone() {
        // utcZdt -> converter zone = America/New_York
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(utcZdt, nyConverter);
        ZonedDateTime adjusted = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York"));
        assertEquals(adjusted.toLocalDateTime(), ldt);
    }

    /* -------------------------------------------------------------
     *  toLocalDate
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDateDifferentZone() {
        LocalDate expected = nyZdt.withZoneSameInstant(utcConverter.getOptions().getZoneId()).toLocalDate();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDate(nyZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toLocalTime
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalTimeDifferentZone() {
        LocalTime expected = nyZdt.withZoneSameInstant(utcConverter.getOptions().getZoneId()).toLocalTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalTime(nyZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toOffsetDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = utcZdt.toOffsetDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toOffsetDateTime(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toAtomicLong
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        long epochMilli = utcZdt.toInstant().toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(utcZdt, utcConverter);
        assertEquals(epochMilli, atomic.get());
    }

    /* -------------------------------------------------------------
     *  toTimestamp
     * ------------------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(utcZdt.toInstant());
        assertEquals(expected, ZonedDateTimeConversions.toTimestamp(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toCalendar
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(utcZdt, utcConverter);
        assertEquals(TimeZone.getTimeZone(ZoneOffset.UTC), cal.getTimeZone());
        assertEquals(utcZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    /* -------------------------------------------------------------
     *  toSqlDate
     * ------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toSqlDate(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toDate
     * ------------------------------------------------------------- */
    @Test
    public void testToDate() {
        Date expected = new Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toDate(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toBigInteger
     * ------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toBigInteger(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        BigDecimal expected = BigDecimal.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toBigDecimal(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toString
     * ------------------------------------------------------------- */
    @Test
    public void testToString() {
        String expected = utcZdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(utcZdt, utcConverter));
    }

    /* -------------------------------------------------------------
     *  toMap
     * ------------------------------------------------------------- */
    @Test
    public void testToMap() {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(utcZdt, utcConverter);
        assertEquals(3, map.size());
        assertEquals(utcZdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(utcZdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(utcZdt.getZone().toString(), map.get(MapConversions.ZONE));
    }

    /* -------------------------------------------------------------
     *  Null / Wrong‑type handling (error scenarios)
     * ------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToLongWithNull() {
        ZonedDateTimeConversions.toLong(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNull() {
        ZonedDateTimeConversions.toString(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateWithWrongType() {
        ZonedDateTimeConversions.toLocalDate(new Date(), utcConverter);
    }
}
