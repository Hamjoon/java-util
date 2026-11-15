package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link InstantConversions}. All public static methods are exercised
 * with a fixed {@link Instant} and a {@link Converter} that supplies deterministic
 * {@link ConverterOptions}. The tests verify that each conversion produces the
 * expected value according to the Java time API.
 */
public class InstantConversionsTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2023-01-02T03:04:05.006007008Z");
    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not part of the supplied source but the library provides a
        // default constructor that uses the system default zone. For deterministic tests we
        // explicitly set the zone to UTC.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = InstantConversions.toMap(FIXED_INSTANT, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(FIXED_INSTANT.toString(), map.get(MapConversions.INSTANT));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.atZone(ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(FIXED_INSTANT, converter);
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        assertEquals(FIXED_INSTANT.atOffset(expectedOffset), odt);
    }

    @Test
    public void testToLong() {
        long epochMilli = InstantConversions.toLong(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        double seconds = InstantConversions.toDouble(FIXED_INSTANT, converter);
        // Compute expected value using the same helper logic
        double expected = BigDecimal.valueOf(FIXED_INSTANT.getEpochSecond())
                .add(BigDecimal.valueOf(FIXED_INSTANT.getNano(), 9))
                .doubleValue();
        assertEquals(expected, seconds, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = InstantConversions.toAtomicLong(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = InstantConversions.toTimestamp(FIXED_INSTANT, converter);
        assertEquals(Timestamp.from(FIXED_INSTANT), ts);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = InstantConversions.toSqlDate(FIXED_INSTANT, converter);
        // The SQL date should represent the local date in UTC
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2023, 1, 2)), sqlDate);
    }

    @Test
    public void testToDate() {
        Date date = InstantConversions.toDate(FIXED_INSTANT, converter);
        assertEquals(new Date(FIXED_INSTANT.toEpochMilli()), date);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = InstantConversions.toCalendar(FIXED_INSTANT, converter);
        assertNotNull(cal);
        assertEquals(FIXED_INSTANT.toEpochMilli(), cal.getTimeInMillis());
        // The calendar should use the UTC time zone supplied by the options
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bigInt = InstantConversions.toBigInteger(FIXED_INSTANT, converter);
        // Expected nanoseconds = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(FIXED_INSTANT.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(FIXED_INSTANT.getNano()));
        assertEquals(expected, bigInt);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = InstantConversions.toBigDecimal(FIXED_INSTANT, converter);
        BigDecimal expected = BigDecimal.valueOf(FIXED_INSTANT.getEpochSecond())
                .add(BigDecimal.valueOf(FIXED_INSTANT.getNano(), 9));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = InstantConversions.toLocalDateTime(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.atZone(ZoneId.of("UTC")).toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = InstantConversions.toLocalDate(FIXED_INSTANT, converter);
        assertEquals(LocalDate.of(2023, 1, 2), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = InstantConversions.toLocalTime(FIXED_INSTANT, converter);
        assertEquals(LocalTime.of(3, 4, 5, 6_007_008), lt);
    }
}
