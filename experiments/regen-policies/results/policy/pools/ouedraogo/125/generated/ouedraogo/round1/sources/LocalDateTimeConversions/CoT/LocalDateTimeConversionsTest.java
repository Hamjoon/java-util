package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public class LocalDateTimeConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // Use system default zone / time‑zone – the real ConverterOptions class
        // provides these getters, so a default instance is sufficient for the tests.
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor
        this.zoneId = options.getZoneId();                 // usually ZoneId.systemDefault()
        this.timeZone = options.getTimeZone();            // usually TimeZone.getDefault()
        this.converter = new Converter(options);
    }

    /* -------------------------------------------------------------
       1. Basic happy‑path tests for every conversion method
       ------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 25, 12, 30, 45, 123_000_000);
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        assertEquals(ldt, zdt.toLocalDateTime());
        assertEquals(zoneId, zdt.getZone());
    }

    @Test
    public void testToOffsetDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 25, 12, 30);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(timeZone.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(ldt, odt.toLocalDateTime());
        assertEquals(expected, odt.getOffset());
    }

    @Test
    public void testToInstant() {
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        Instant instant = LocalDateTimeConversions.toInstant(ldt, converter);
        ZonedDateTime zdt = ldt.atZone(zoneId);
        assertEquals(zdt.toInstant(), instant);
    }

    @Test
    public void testToLong() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        long epochMilli = LocalDateTimeConversions.toLong(ldt, converter);
        assertEquals(1000L, epochMilli);
    }

    @Test
    public void testToDouble() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 500_000_000);
        double d = LocalDateTimeConversions.toDouble(ldt, converter);
        // InstantConversions.toDouble returns epoch seconds with fractional part
        assertEquals(0.5, d, 0.000001);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(ldt, converter);
        assertEquals(ldt, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDateTime ldt = LocalDateTime.of(2022, 2, 28, 10, 15);
        LocalDate date = LocalDateTimeConversions.toLocalDate(ldt, converter);
        assertEquals(LocalDate.of(2022, 2, 28), date);
    }

    @Test
    public void testToLocalTime() {
        LocalDateTime ldt = LocalDateTime.of(2022, 2, 28, 10, 15, 30);
        LocalTime time = LocalDateTimeConversions.toLocalTime(ldt, converter);
        assertEquals(LocalTime.of(10, 15, 30), time);
    }

    @Test
    public void testToAtomicLong() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 2);
        AtomicLong al = LocalDateTimeConversions.toAtomicLong(ldt, converter);
        assertEquals(2000L, al.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 10, 8, 30, 15, 250_000_000);
        Timestamp ts = LocalDateTimeConversions.toTimestamp(ldt, converter);
        Instant expected = ldt.atZone(zoneId).toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToCalendar() {
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 10, 8, 30);
        Calendar cal = LocalDateTimeConversions.toCalendar(ldt, converter);
        assertEquals(timeZone, cal.getTimeZone());
        assertEquals(ldt.atZone(zoneId).toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 0, 0);
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(ldt, converter);
        assertEquals(LocalDateTimeConversions.toLong(ldt, converter), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 12, 0);
        Date date = LocalDateTimeConversions.toDate(ldt, converter);
        assertEquals(LocalDateTimeConversions.toLong(ldt, converter), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        BigInteger bi = LocalDateTimeConversions.toBigInteger(ldt, converter);
        assertEquals(BigInteger.valueOf(1000L), bi);
    }

    @Test
    public void testToBigDecimal() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1, 500_000_000);
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(ldt, converter);
        assertEquals(new BigDecimal("1000.5"), bd);
    }

    @Test
    public void testToString() {
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 123_000_000);
        String s = LocalDateTimeConversions.toString(ldt, converter);
        assertEquals("2022-12-31T23:59:59.123", s);
    }

    @Test
    public void testToMap() {
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(ldt, converter);
        assertEquals("2022-12-31", map.get(MapConversions.DATE));
        assertEquals("23:59:59", map.get(MapConversions.TIME));
    }

    /* -------------------------------------------------------------
       2. Edge‑case / exception tests
       ------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTimeWithWrongType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTimeWithNull() {
        LocalDateTimeConversions.toZonedDateTime(null, converter);
    }

    @Test
    public void testToOffsetDateTimeWithNegativeOffset() {
        // Simulate a converter whose TimeZone is GMT‑5 (offset -5h)
        TimeZone gmtMinus5 = TimeZone.getTimeZone("GMT-05:00");
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(gmtMinus5);
        opts.setZoneId(gmtMinus5.toZoneId());
        Converter conv = new Converter(opts);

        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 0);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, conv);
        ZoneOffset expected = ZoneOffset.ofHours(-5);
        assertEquals(expected, odt.getOffset());
    }

    @Test
    public void testToInstantDuringDstGap() {
        // US daylight‑saving transition: 2023‑03‑12 02:00 -> 03:00 (gap)
        ZoneId usEastern = ZoneId.of("America/New_York");
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(usEastern);
        opts.setTimeZone(TimeZone.getTimeZone(usEastern));
        Converter conv = new Converter(opts);

        // 2023‑03‑12 02:30 does not exist; java.time resolves by shifting forward
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 12, 2, 30);
        Instant instant = LocalDateTimeConversions.toInstant(ldt, conv);
        ZonedDateTime resolved = ldt.atZone(usEastern);
        assertEquals(resolved.toInstant(), instant);
    }

    @Test
    public void testToLongEdgeEpoch() {
        LocalDateTime ldt = LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000);
        long epochMilli = LocalDateTimeConversions.toLong(ldt, converter);
        // One millisecond before epoch start
        assertEquals(-1L, epochMilli);
    }

    @Test
    public void testToStringWithNanosecondPrecision() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0, 0, 999_999_999);
        String s = LocalDateTimeConversions.toString(ldt, converter);
        // ISO_LOCAL_DATE_TIME prints up to nanoseconds, but trailing zeros are omitted.
        assertEquals("2022-01-01T00:00:00.999999999", s);
    }

    @Test
    public void testToMapContainsOnlyDateAndTimeKeys() {
        LocalDateTime ldt = LocalDateTime.of(2000, 2, 29, 6, 7, 8);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(ldt, converter);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertEquals(2, map.size());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWithWrongType() {
        LocalDateTimeConversions.toString(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithWrongType() {
        LocalDateTimeConversions.toMap(Boolean.TRUE, converter);
    }
}
