package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link TimestampConversions}.
 *
 * The test class resides in the same package as {@code TimestampConversions}
 * so that the package‑private static methods are directly accessible.
 *
 * The tests cover:
 *   1. Normal (happy‑path) conversion for each static method.
 *   2. Edge‑case handling – null arguments, non‑Timestamp objects,
 *      timestamps before the epoch and timestamps with zero / maximum nanos.
 *   3. Validation that the produced {@link Map} contains the expected keys
 *      and values.
 */
public class TimestampConversionsTest {

    /** A {@link Converter} instance with default options – the concrete
     *  implementation of {@code ConverterOptions} is part of the library
     *  and provides a default {@code ZoneId} (UTC) and {@code TimeZone}
     *  (UTC). */
    private Converter converter;

    /** A reference {@link Instant} used by many tests. */
    private Instant instant;

    /** A {@link Timestamp} built from {@link #instant} and the same
     *  nanosecond fraction. */
    private Timestamp timestamp;

    /** The zone used by the default {@code ConverterOptions}. */
    private ZoneId zoneId;

    @Before
    public void setUp() {
        // The library supplies a no‑arg constructor for ConverterOptions.
        // If the real library requires a different construction strategy,
        // this block can be adapted without changing the test logic.
        ConverterOptions options = new ConverterOptions(); // default = UTC
        this.converter = new Converter(options);
        this.zoneId = options.getZoneId();               // typically UTC

        // Choose an instant with a non‑zero nanosecond component.
        this.instant = Instant.parse("2023-01-01T12:34:56.123456789Z");
        this.timestamp = new Timestamp(instant.toEpochMilli());
        this.timestamp.setNanos(instant.getNano());
    }

    /* -------------------------------------------------------------------- */
    /* 1.  Basic happy‑path tests                                            */
    /* -------------------------------------------------------------------- */

    @Test
    public void testToDouble() {
        double actual = TimestampConversions.toDouble(timestamp, converter);
        Duration d = Duration.between(Instant.EPOCH, instant);
        double expected = d.getSeconds() + d.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal actual = TimestampConversions.toBigDecimal(timestamp, converter);
        // Expected value: seconds + nanos * 10⁻⁹
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano()).movePointLeft(9));
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToBigInteger() {
        BigInteger actual = TimestampConversions.toBigInteger(timestamp, converter);
        Duration d = Duration.between(Instant.EPOCH, instant);
        BigInteger expected = BigInteger.valueOf(d.getSeconds())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(d.getNano()));
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime actual = TimestampConversions.toLocalDateTime(timestamp, converter);
        LocalDateTime expected = instant.atZone(zoneId).toLocalDateTime();
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration() {
        Duration actual = TimestampConversions.toDuration(timestamp, converter);
        Duration expected = Duration.between(Instant.EPOCH, instant);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(timestamp, converter);
        OffsetDateTime expected = instant.atZone(zoneId).toOffsetDateTime();
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar actual = TimestampConversions.toCalendar(timestamp, converter);
        Calendar expected = Calendar.getInstance(converter.getOptions().getTimeZone());
        expected.setTimeInMillis(timestamp.getTime());
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTimeZone(), actual.getTimeZone());
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = TimestampConversions.toMap(timestamp, converter);
        // Verify that the map contains exactly five entries.
        assertEquals(5, map.size());

        // Expected values derived from the same conversion logic used inside the class.
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(timestamp, converter);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(timestamp.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    /* -------------------------------------------------------------------- */
    /* 2.  Edge‑case and exception tests                                    */
    /* -------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullTimestamp() {
        TimestampConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongSourceType() {
        TimestampConversions.toDouble("not a timestamp", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullTimestamp() {
        TimestampConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongSourceType() {
        TimestampConversions.toBigDecimal(new Date(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullTimestamp() {
        TimestampConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongSourceType() {
        TimestampConversions.toBigInteger(Boolean.TRUE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullTimestamp() {
        TimestampConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_WrongSourceType() {
        TimestampConversions.toLocalDateTime(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDuration_NullTimestamp() {
        TimestampConversions.toDuration(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDuration_WrongSourceType() {
        TimestampConversions.toDuration(new Object(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullTimestamp() {
        TimestampConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongSourceType() {
        TimestampConversions.toOffsetDateTime(Instant.now(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullTimestamp() {
        TimestampConversions.toCalendar(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WrongSourceType() {
        TimestampConversions.toCalendar(Double.valueOf(1.23), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullDate() {
        TimestampConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongSourceType() {
        TimestampConversions.toMap(Boolean.FALSE, converter);
    }

    /** Verify handling of timestamps that represent moments *before* the epoch. */
    @Test
    public void testBeforeEpoch() {
        Instant before = Instant.parse("1960-01-01T00:00:00.000000001Z");
        Timestamp ts = new Timestamp(before.toEpochMilli());
        ts.setNanos(before.getNano());

        // All conversions should succeed and produce negative epoch‑based values.
        assertEquals(Duration.between(Instant.EPOCH, before), TimestampConversions.toDuration(ts, converter));
        assertEquals(before.atZone(zoneId).toOffsetDateTime(),
                TimestampConversions.toOffsetDateTime(ts, converter));
    }

    /** Verify that a timestamp with zero nanoseconds is handled correctly. */
    @Test
    public void testZeroNanos() {
        Instant zeroNanos = Instant.parse("2022-12-31T23:59:59Z"); // exactly on a second
        Timestamp ts = new Timestamp(zeroNanos.toEpochMilli());
        ts.setNanos(zeroNanos.getNano()); // 0

        assertEquals(0, TimestampConversions.toDouble(ts, converter), 0.0);
        assertEquals(BigInteger.ZERO, TimestampConversions.toBigInteger(ts, converter));
        assertEquals(0, TimestampConversions.toMap(ts, converter).get(MapConversions.NANOS));
    }

    /** Verify that the maximum nanosecond value (999 999 999) is preserved. */
    @Test
    public void testMaxNanos() {
        Instant maxNanos = Instant.ofEpochSecond(1_600_000_000L, 999_999_999L);
        Timestamp ts = new Timestamp(maxNanos.toEpochMilli());
        ts.setNanos(maxNanos.getNano());

        Map<String, Object> map = TimestampConversions.toMap(ts, converter);
        assertEquals(999_999_999, map.get(MapConversions.NANOS));
    }
}
