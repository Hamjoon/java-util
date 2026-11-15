package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private Converter converter;

    @Before
    public void setUp() {
        // Force JVM default timezone to UTC so that ConverterOptions (which defaults to the JVM TZ)
        // will also use UTC. This makes the tests deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // ConverterOptions is assumed to have a public no‑arg constructor.
        ConverterOptions options = new ConverterOptions();
        // The options object will pick up the default JVM timezone (UTC) and the default ZoneId (UTC).
        // If the library provides setters they could be used, but they are not part of the public API
        // listed in the task, so we rely on defaults.
        this.converter = new Converter(options);
    }

    private void assertMapContainsSecondsAndNanos(Map<String, Object> map, Instant instant) {
        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertTrue("Map should contain key 'seconds'", map.containsKey("seconds"));
        assertTrue("Map should contain key 'nanos'", map.containsKey("nanos"));
        assertEquals("seconds value mismatch", instant.getEpochSecond(), map.get("seconds"));
        assertEquals("nanos value mismatch", instant.getNano(), map.get("nanos"));
    }

    @Test
    public void testToMap_EpochStart() {
        Instant instant = Instant.ofEpochSecond(0, 0);
        Map<String, Object> map = InstantConversions.toMap(instant, converter);
        assertMapContainsSecondsAndNanos(map, instant);
    }

    @Test
    public void testToMap_NegativeAndNanos() {
        Instant instant = Instant.ofEpochSecond(-12345, 987_654_321);
        Map<String, Object> map = InstantConversions.toMap(instant, converter);
        assertMapContainsSecondsAndNanos(map, instant);
    }

    @Test
    public void testToZonedDateTime() {
        Instant instant = Instant.now();
        ZonedDateTime expected = instant.atZone(UTC_ZONE);
        ZonedDateTime actual = InstantConversions.toZonedDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_UtcOffset() {
        Instant instant = Instant.now();
        // With UTC as default timezone the offset should be zero.
        OffsetDateTime expected = instant.atOffset(ZoneOffset.UTC);
        OffsetDateTime actual = InstantConversions.toOffsetDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        long expected = instant.toEpochMilli();
        long actual = InstantConversions.toLong(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 500_000_000);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        double actual = InstantConversions.toDouble(instant, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochSecond(2_000_000_000L, 0);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, converter);
        assertEquals(instant.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.now();
        Timestamp ts = InstantConversions.toTimestamp(instant, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 0);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, converter);
        assertEquals(instant.toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 250_000_000);
        Date date = InstantConversions.toDate(instant, converter);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 0);
        Calendar cal = InstantConversions.toCalendar(instant, converter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(12345, 678_901_234);
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(instant.getNano()));
        BigInteger actual = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(12345, 500_000_000);
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        BigDecimal actual = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.now();
        LocalDateTime expected = instant.atZone(UTC_ZONE).toLocalDateTime();
        LocalDateTime actual = InstantConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        Instant instant = Instant.now();
        LocalDate expected = instant.atZone(UTC_ZONE).toLocalDate();
        LocalDate actual = InstantConversions.toLocalDate(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        Instant instant = Instant.now();
        LocalTime expected = instant.atZone(UTC_ZONE).toLocalTime();
        LocalTime actual = InstantConversions.toLocalTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testCompactMapBuilderInsertionOrder() {
        // Verify that the map returned by toMap preserves insertion order (seconds first, nanos second)
        Instant instant = Instant.ofEpochSecond(42, 99);
        Map<String, Object> map = InstantConversions.toMap(instant, converter);
        assertTrue("Map should be an instance of CompactMap", map instanceof CompactMap);
        // The builder used insertion order, so iterator order should be seconds then nanos
        String[] expectedKeys = {"seconds", "nanos"};
        int idx = 0;
        for (String key : map.keySet()) {
            assertEquals(expectedKeys[idx++], key);
        }
        assertEquals("Map should contain exactly two keys", 2, idx);
    }
}
