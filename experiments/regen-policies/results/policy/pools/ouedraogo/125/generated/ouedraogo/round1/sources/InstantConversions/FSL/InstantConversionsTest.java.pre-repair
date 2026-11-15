package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link InstantConversions}.
 *
 * The tests cover every static conversion method, exercising typical values,
 * boundary conditions (negative epoch seconds, zero nanos) and verifying that
 * the supplied {@link Converter} options (zone and time‑zone) are honoured.
 *
 * The library provides a {@code ConverterOptions} class (not shown in the
 * excerpt) with {@code setZoneId} and {@code setTimeZone} mutators – the tests
 * use those to configure a {@link Converter} instance.
 */
public class InstantConversionsTest {

    private Converter utcConverter;
    private Converter offsetConverter;

    @Before
    public void setUp() {
        // Build a Converter whose options use UTC (zone offset 0)
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // Build a Converter whose options use a non‑UTC zone (e.g. America/New_York)
        ConverterOptions offsetOptions = new ConverterOptions();
        offsetOptions.setZoneId(ZoneId.of("America/New_York"));
        offsetOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        offsetConverter = new Converter(offsetOptions);
    }

    @Test
    public void testToMap_basic() {
        Instant instant = Instant.ofEpochSecond(1_234_567L, 890_123_456);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);

        assertEquals(2, map.size());
        assertTrue(map.containsKey("seconds"));
        assertTrue(map.containsKey("nanos"));
        assertEquals(1_234_567L, map.get("seconds"));
        assertEquals(890_123_456, map.get("nanos"));
    }

    @Test
    public void testToMap_negativeEpoch() {
        Instant instant = Instant.ofEpochSecond(-10L, 999_999_999);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);

        assertEquals(-10L, map.get("seconds"));
        assertEquals(999_999_999, map.get("nanos"));
    }

    @Test
    public void testToZonedDateTime_utc() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.123456789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);

        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_nonUtc() {
        Instant instant = Instant.parse("2023-03-25T12:34:56Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, offsetConverter);

        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        // The instant stays the same; the local date‑time changes according to the zone.
        assertEquals(instant, zdt.toInstant());
        assertEquals(LocalDateTime.of(2023, 3, 25, 8, 34, 56), zdt.toLocalDateTime());
    }

    @Test
    public void testToOffsetDateTime_utc() {
        Instant instant = Instant.parse("2022-01-01T00:00:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);

        assertEquals(ZoneId.of("UTC").getRules().getOffset(instant), odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_withOffset() {
        // New York is UTC‑5 in winter (standard time)
        Instant instant = Instant.parse("2022-01-01T12:00:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, offsetConverter);

        assertEquals(ZoneId.of("America/New_York").getRules().getOffset(instant), odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_123L);
        long millis = InstantConversions.toLong(instant, utcConverter);
        assertEquals(1_600_000_000_123L, millis);
    }

    @Test
    public void testToDouble() {
        Instant instant = Instant.ofEpochSecond(1_000L, 500_000_000); // 1000.5 seconds
        double d = InstantConversions.toDouble(instant, utcConverter);
        assertEquals(1000.5, d, 0.000_000_001);
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochMilli(42L);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(42L, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.parse("2021-12-31T23:59:59.999999999Z");
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.ofEpochMilli(0L); // epoch
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(0L, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochMilli(12345L);
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(12345L, date.getTime());
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochMilli(9_876_543_210L);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(9_876_543_210L, cal.getTimeInMillis());
        // Verify that the calendar respects the converter’s time‑zone (UTC here)
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(2L, 3);
        // Expected nanoseconds = 2 * 1_000_000_000 + 3
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(3L));
        BigInteger actual = InstantConversions.toBigInteger(instant, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(5L, 250_000_000); // 5.25 seconds
        BigDecimal bd = InstantConversions.toBigDecimal(instant, utcConverter);
        assertEquals(new BigDecimal("5.25"), bd);
    }

    @Test
    public void testToLocalDateTime_utc() {
        Instant instant = Instant.parse("2020-02-29T23:59:59.999999999Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, utcConverter);
        assertEquals(LocalDateTime.of(2020, 2, 29, 23, 59, 59, 999_999_999), ldt);
    }

    @Test
    public void testToLocalDateTime_nonUtc() {
        // Same instant, but converted to New York time (UTC‑5)
        Instant instant = Instant.parse("2020-02-29T12:00:00Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, offsetConverter);
        assertEquals(LocalDateTime.of(2020, 2, 29, 7, 0, 0), ldt);
    }

    @Test
    public void testToLocalDate() {
        Instant instant = Instant.parse("2021-07-15T00:00:00Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, utcConverter);
        assertEquals(LocalDate.of(2021, 7, 15), ld);
    }

    @Test
    public void testToLocalTime() {
        Instant instant = Instant.parse("2021-07-15T08:15:30.123456789Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, utcConverter);
        assertEquals(LocalTime.of(8, 15, 30, 123_456_789), lt);
    }

    @Test
    public void testAllConversions_consistency() {
        // Pick a random instant and run every conversion, then cross‑check.
        Instant instant = Instant.ofEpochSecond(1_609_459_200L, 123_456_789); // 2020‑12‑01T00:00:00.123456789Z

        // Primitive / wrapper
        assertEquals(instant.toEpochMilli(), InstantConversions.toLong(instant, utcConverter));
        assertEquals(InstantConversions.toLong(instant, utcConverter), InstantConversions.toAtomicLong(instant, utcConverter).get());

        // Date / time types
        assertEquals(Timestamp.from(instant), InstantConversions.toTimestamp(instant, utcConverter));
        assertEquals(new java.sql.Date(instant.toEpochMilli()), InstantConversions.toSqlDate(instant, utcConverter));
        assertEquals(new Date(instant.toEpochMilli()), InstantConversions.toDate(instant, utcConverter));

        // Map representation
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);
        assertEquals(instant.getEpochSecond(), map.get("seconds"));
        assertEquals(instant.getNano(), map.get("nanos"));

        // BigInteger / BigDecimal
        BigInteger expectedBI = BigInteger.valueOf(instant.getEpochSecond())
                                          .multiply(BigInteger.valueOf(1_000_000_000L))
                                          .add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expectedBI, InstantConversions.toBigInteger(instant, utcConverter));
        assertEquals(BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano()),
                     InstantConversions.toBigDecimal(instant, utcConverter));

        // Zoned / Offset / Local conversions
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        assertEquals(instant, zdt.toInstant());

        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        assertEquals(instant, odt.toInstant());

        assertEquals(zdt.toLocalDateTime(), InstantConversions.toLocalDateTime(instant, utcConverter));
        assertEquals(zdt.toLocalDate(), InstantConversions.toLocalDate(instant, utcConverter));
        assertEquals(zdt.toLocalTime(), InstantConversions.toLocalTime(instant, utcConverter));
    }
}
