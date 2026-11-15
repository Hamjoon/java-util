package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Rule;

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

public class ZonedDateTimeConversionsTest {

    private Converter converter;               // uses default options (system zone)
    private Converter converterUtc;            // explicit UTC options
    private Converter converterNy;              // explicit America/New_York options

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The library supplies a public ConverterOptions class; we rely on its default ctor.
        converter = new Converter(new ConverterOptions());

        // Create converters with known zones for the zone‑adjustment tests.
        ConverterOptions utcOpts = new ConverterOptions();
        utcOpts.setZoneId(ZoneId.of("UTC"));
        utcOpts.setTimeZone(TimeZone.getTimeZone("UTC"));
        converterUtc = new Converter(utcOpts);

        ConverterOptions nyOpts = new ConverterOptions();
        nyOpts.setZoneId(ZoneId.of("America/New_York"));
        nyOpts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        converterNy = new Converter(nyOpts);
    }

    /* -------------------------------------------------
       1. BASIC FUNCTIONALITY TESTS
       ------------------------------------------------- */

    @Test
    public void testToLong() {
        ZonedDateTime zdt = ZonedDateTime.now();
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(zdt, converter));
    }

    @Test
    public void testToDouble() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Instant i = zdt.toInstant();
        double expected = i.getEpochSecond() + i.getNano() / 1_000_000_000d;
        double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
        assertEquals(expected, actual, 0.000000001);
    }

    @Test
    public void testToInstant() {
        ZonedDateTime zdt = ZonedDateTime.now();
        assertEquals(zdt.toInstant(),
                     ZonedDateTimeConversions.toInstant(zdt, converter));
    }

    @Test
    public void testToLocalDateTimeSameZone() {
        ZonedDateTime zdt = ZonedDateTime.now();
        LocalDateTime expected = zdt.withZoneSameInstant(converter.getOptions().getZoneId())
                                    .toLocalDateTime();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalDateTime(zdt, converter));
    }

    @Test
    public void testToLocalDateTimeDifferentZone() {
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.parse("2023-03-25T12:00:00Z"),
                                                    ZoneId.of("UTC"));
        LocalDateTime expected = utc.withZoneSameInstant(ZoneId.of("America/New_York"))
                                   .toLocalDateTime();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalDateTime(utc, converterNy));
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime zdt = ZonedDateTime.now();
        LocalDate expected = ZonedDateTimeConversions.toLocalDateTime(zdt, converter)
                                                      .toLocalDate();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalDate(zdt, converter));
    }

    @Test
    public void testToLocalTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        LocalTime expected = ZonedDateTimeConversions.toLocalDateTime(zdt, converter)
                                                      .toLocalTime();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalTime(zdt, converter));
    }

    @Test
    public void testToOffsetDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        assertEquals(zdt.toOffsetDateTime(),
                     ZonedDateTimeConversions.toOffsetDateTime(zdt, converter));
    }

    @Test
    public void testToAtomicLong() {
        ZonedDateTime zdt = ZonedDateTime.now();
        long expected = ZonedDateTimeConversions.toLong(zdt, converter);
        AtomicLong al = ZonedDateTimeConversions.toAtomicLong(zdt, converter);
        assertEquals(expected, al.get());
    }

    @Test
    public void testToTimestamp() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Timestamp expected = Timestamp.from(zdt.toInstant());
        assertEquals(expected,
                     ZonedDateTimeConversions.toTimestamp(zdt, converter));
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, converter);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        ZonedDateTime zdt = ZonedDateTime.now();
        java.sql.Date sqlDate = ZonedDateTimeConversions.toSqlDate(zdt, converter);
        assertEquals(zdt.toInstant().toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Date date = ZonedDateTimeConversions.toDate(zdt, converter);
        assertEquals(zdt.toInstant().toEpochMilli(), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Instant i = zdt.toInstant();
        // Instant → epoch‑millis → BigInteger (the library’s InstantConversions does the same)
        BigInteger expected = BigInteger.valueOf(i.toEpochMilli());
        assertEquals(expected,
                     ZonedDateTimeConversions.toBigInteger(zdt, converter));
    }

    @Test
    public void testToBigDecimal() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Instant i = zdt.toInstant();
        // Represent epoch‑millis as a decimal with scale 3 (milliseconds)
        BigDecimal expected = new BigDecimal(i.toEpochMilli()).movePointLeft(3);
        assertEquals(0,
                     expected.compareTo(ZonedDateTimeConversions.toBigDecimal(zdt, converter)));
    }

    @Test
    public void testToString() {
        ZonedDateTime zdt = ZonedDateTime.now();
        String expected = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected,
                     ZonedDateTimeConversions.toString(zdt, converter));
    }

    @Test
    public void testToMap() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10,
                                            15, 30, 45, 123_000_000,
                                            ZoneId.of("Europe/Paris"));
        Map<String, Object> map = ZonedDateTimeConversions.toMap(zdt, converter);
        assertEquals("2023-05-10", map.get(MapConversions.DATE));
        assertEquals("15:30:45.123", map.get(MapConversions.TIME));
        assertEquals("Europe/Paris", map.get(MapConversions.ZONE));
    }

    /* -------------------------------------------------
       2. EDGE CASES & EXCEPTION TESTS
       ------------------------------------------------- */

    @Test
    public void testNullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ZonedDateTimeConversions.toLong(null, converter);
    }

    @Test
    public void testWrongTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZonedDateTimeConversions.toLong("I am not a ZonedDateTime", converter);
    }

    @Test
    public void testToLocalDateDifferentZone() {
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.parse("2022-12-01T00:00:00Z"),
                                                    ZoneId.of("UTC"));
        // New York is UTC‑5 in winter
        LocalDate expected = utc.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDate();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalDate(utc, converterNy));
    }

    @Test
    public void testToLocalTimeDifferentZone() {
        ZonedDateTime utc = ZonedDateTime.ofInstant(Instant.parse("2022-06-01T12:00:00Z"),
                                                    ZoneId.of("UTC"));
        // New York is UTC‑4 in summer
        LocalTime expected = utc.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalTime();
        assertEquals(expected,
                     ZonedDateTimeConversions.toLocalTime(utc, converterNy));
    }

    @Test
    public void testToCalendarUsesConverterTimeZone() {
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, converterNy);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
    }

    @Test
    public void testToMapWithCustomZoneId() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1,
                                            0, 0, 0, 0,
                                            ZoneId.of("GMT+02:00"));
        Map<String, Object> map = ZonedDateTimeConversions.toMap(zdt, converter);
        assertEquals("GMT+02:00", map.get(MapConversions.ZONE));
    }

    @Test
    public void testToDoubleWithNegativeEpoch() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("-0001-01-01T00:00:00Z"),
                                                    ZoneId.of("UTC"));
        double actual = ZonedDateTimeConversions.toDouble(zdt, converterUtc);
        double expected = -31556952000d; // approximate seconds for year -1
        assertTrue(actual < 0);
        assertEquals(expected, actual, Math.abs(expected) * 0.001);
    }

    @Test
    public void testToBigIntegerLargeInstant() {
        // Use Instant.MAX (year 1000000000) – within java.time range
        Instant max = Instant.MAX;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(max, ZoneId.of("UTC"));
        BigInteger expected = BigInteger.valueOf(max.toEpochMilli());
        assertEquals(expected,
                     ZonedDateTimeConversions.toBigInteger(zdt, converterUtc));
    }

    @Test
    public void testToBigDecimalScale() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234), ZoneId.of("UTC"));
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(zdt, converterUtc);
        // 1234 ms = 1.234 seconds
        BigDecimal expected = new BigDecimal("1.234");
        assertEquals(0, expected.compareTo(actual));
    }
}
