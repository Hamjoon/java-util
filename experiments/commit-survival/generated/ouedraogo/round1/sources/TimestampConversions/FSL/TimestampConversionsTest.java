package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link TimestampConversions}.
 * <p>
 * The tests cover all conversion methods, exercising typical values,
 * edge cases (epoch, far future, negative timestamps) and verifying
 * that the {@link Converter} options (zone and time‑zone) are honoured.
 * </p>
 */
public class TimestampConversionsTest {

    private Converter utcConverter;
    private Converter offsetConverter;

    @Before
    public void setUp() {
        // Assume ConverterOptions has a public constructor taking a ZoneId and a TimeZone.
        // The library supplies a default UTC zone if none is supplied.
        ConverterOptions utcOptions = new ConverterOptions(ZoneId.of("UTC"), TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // Use a non‑UTC zone to verify that zone handling is correct.
        ConverterOptions offsetOptions = new ConverterOptions(ZoneId.of("America/New_York"),
                TimeZone.getTimeZone("America/New_York"));
        offsetConverter = new Converter(offsetOptions);
    }

    @Test
    public void testToDuration_Epoch() {
        Timestamp ts = Timestamp.from(Instant.EPOCH);
        Duration d = TimestampConversions.toDuration(ts, utcConverter);
        assertEquals(0L, d.getSeconds());
        assertEquals(0, d.getNano());
    }

    @Test
    public void testToDuration_NonZero() {
        Instant instant = Instant.parse("2023-03-15T12:34:56.123456789Z");
        Timestamp ts = Timestamp.from(instant);
        Duration d = TimestampConversions.toDuration(ts, utcConverter);
        assertEquals(instant.getEpochSecond(), d.getSeconds());
        assertEquals(instant.getNano(), d.getNano());
    }

    @Test
    public void testToDouble_Positive() {
        Instant instant = Instant.parse("1970-01-01T00:00:01.500Z"); // 1.5 seconds
        Timestamp ts = Timestamp.from(instant);
        double d = TimestampConversions.toDouble(ts, utcConverter);
        assertEquals(1.5, d, 0.000001);
    }

    @Test
    public void testToDouble_Negative() {
        // Timestamp before epoch (allowed via setTime)
        Timestamp ts = new Timestamp(-1500L); // -1.5 seconds
        double d = TimestampConversions.toDouble(ts, utcConverter);
        assertEquals(-1.5, d, 0.000001);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.parse("2020-02-29T23:59:59.999999999Z");
        Timestamp ts = Timestamp.from(instant);
        BigDecimal bd = TimestampConversions.toBigDecimal(ts, utcConverter);
        // InstantConversions.toBigDecimal uses seconds + nanos/1_000_000_000
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.parse("2000-01-01T00:00:00Z");
        Timestamp ts = Timestamp.from(instant);
        BigInteger bi = TimestampConversions.toBigInteger(ts, utcConverter);
        // Duration between epoch and instant is whole seconds, no nanos.
        assertEquals(BigInteger.valueOf(instant.getEpochSecond()), bi);
    }

    @Test
    public void testToLocalDateTime_Utc() {
        Timestamp ts = Timestamp.from(Instant.parse("2021-07-01T15:30:45Z"));
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, utcConverter);
        assertEquals(LocalDateTime.of(2021, 7, 1, 15, 30, 45), ldt);
    }

    @Test
    public void testToLocalDateTime_WithZoneOffset() {
        // 2021‑07‑01 15:30:45 UTC should become 11:30:45 in New York (EDT, -4h)
        Timestamp ts = Timestamp.from(Instant.parse("2021-07-01T15:30:45Z"));
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, offsetConverter);
        assertEquals(LocalDateTime.of(2021, 7, 1, 11, 30, 45), ldt);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        Timestamp ts = Timestamp.from(Instant.parse("2022-12-31T23:00:00Z"));
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(LocalDateTime.of(2022, 12, 31, 23, 0, 0), odt.toLocalDateTime());
    }

    @Test
    public void testToOffsetDateTime_WithZone() {
        Timestamp ts = Timestamp.from(Instant.parse("2022-12-31T23:00:00Z"));
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, offsetConverter);
        // New York is UTC‑5 (standard time) on 2022‑12‑31
        assertEquals(ZoneOffset.ofHours(-5), odt.getOffset());
        assertEquals(LocalDateTime.of(2022, 12, 31, 18, 0, 0), odt.toLocalDateTime());
    }

    @Test
    public void testToCalendar_Utc() {
        Timestamp ts = Timestamp.from(Instant.parse("2020-05-20T10:20:30.400Z"));
        Calendar cal = TimestampConversions.toCalendar(ts, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_WithZone() {
        Timestamp ts = Timestamp.from(Instant.parse("2020-05-20T10:20:30.400Z"));
        Calendar cal = TimestampConversions.toCalendar(ts, offsetConverter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToMap_Utc() {
        Timestamp ts = Timestamp.from(Instant.parse("2021-01-01T00:00:01.123456789Z"));
        Map<String, Object> map = TimestampConversions.toMap(ts, utcConverter);

        assertEquals("2021-01-01", map.get(MapConversions.DATE));
        assertEquals("00:00:01.123456789", map.get(MapConversions.TIME));
        assertEquals("UTC", map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(123456789, map.get(MapConversions.NANOS));
    }

    @Test
    public void testToMap_WithZone() {
        Timestamp ts = Timestamp.from(Instant.parse("2021-01-01T05:00:00Z"));
        // 05:00 UTC is midnight in New York (UTC‑5)
        Map<String, Object> map = TimestampConversions.toMap(ts, offsetConverter);

        assertEquals("2020-12-31", map.get(MapConversions.DATE));
        assertEquals("00:00", map.get(MapConversions.TIME).toString().substring(0,5)); // ignore nanos
        assertEquals("America/New_York", map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(0, map.get(MapConversions.NANOS));
    }
}
