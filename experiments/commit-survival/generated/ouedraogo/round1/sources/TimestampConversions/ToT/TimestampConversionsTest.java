package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TimestampConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversions (epoch, typical dates, dates with nanosecond precision)</li>
 *   <li>Edge cases such as the Unix epoch (0 ms) and maximum nanosecond value (999 999 999)</li>
 *   <li>Verification that the {@code Converter} options (zone and time‑zone) are honoured</li>
 *   <li>Correct structure and content of the {@code Map} produced by {@code toMap}</li>
 * </ul>
 *
 * No mocking frameworks are used – all objects are real instances created via public
 * constructors or factory methods that appear in the source code or its declared
 * dependencies.
 */
public class TimestampConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // Use the system default zone & time‑zone – they are guaranteed to be non‑null.
        zoneId = ZoneId.systemDefault();
        timeZone = TimeZone.getDefault();

        // ConverterOptions is not part of the public API shown, but it has a public
        // no‑arg constructor and the default zone / time‑zone are used when no
        // explicit configuration is supplied.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /** Helper to create a {@link Timestamp} with the given epoch milliseconds and nanos. */
    private Timestamp createTimestamp(long epochMilli, int nanos) {
        Timestamp ts = new Timestamp(epochMilli);
        ts.setNanos(nanos);
        return ts;
    }

    @Test
    public void testToDouble_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        double result = TimestampConversions.toDouble(ts, converter);

        // Expected: 0 seconds + 0 nanos = 0.0
        assertEquals(0.0, result, 0.0);
    }

    @Test
    public void testToDouble_WithNanos() {
        // 1 second + 123 456 789 nanoseconds
        long epochMilli = 1_000L; // 1 second
        int nanos = 123_456_789;
        Timestamp ts = createTimestamp(epochMilli, nanos);

        double result = TimestampConversions.toDouble(ts, converter);

        double expected = 1.0 + nanos / 1_000_000_000.0;
        assertEquals(expected, result, 1e-12);
    }

    @Test
    public void testToBigDecimal_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        BigDecimal result = TimestampConversions.toBigDecimal(ts, converter);

        // Instant.EPOCH = 1970‑01‑01T00:00:00Z → BigDecimal 0
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigDecimal_WithFraction() {
        long epochMilli = 1_234_567_890L; // some arbitrary moment
        int nanos = 987_654_321;
        Timestamp ts = createTimestamp(epochMilli, nanos);

        Instant instant = ts.toInstant();
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));

        BigDecimal result = TimestampConversions.toBigDecimal(ts, converter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToBigInteger_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        BigInteger result = TimestampConversions.toBigInteger(ts, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    public void testToBigInteger_WithNanos() {
        long epochMilli = 2_000L; // 2 seconds
        int nanos = 500_000_000;  // 0.5 second
        Timestamp ts = createTimestamp(epochMilli, nanos);

        Duration duration = TimestampConversions.toDuration(ts, converter);
        // Expected total nanoseconds = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(duration.getSeconds())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(duration.getNano()));

        BigInteger result = TimestampConversions.toBigInteger(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDateTime_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, converter);
        LocalDateTime expected = Instant.EPOCH.atZone(zoneId).toLocalDateTime();

        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDateTime_WithZone() {
        // Use a known zone to make the expectation deterministic
        ZoneId testZone = ZoneId.of("America/New_York");
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(testZone);               // assume setter exists
        Converter testConverter = new Converter(opts);

        Timestamp ts = createTimestamp(1_600_000_000_000L, 0); // 2020‑09‑13T12:26:40Z
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, testConverter);
        LocalDateTime expected = ts.toInstant().atZone(testZone).toLocalDateTime();

        assertEquals(expected, ldt);
    }

    @Test
    public void testToDuration_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        Duration duration = TimestampConversions.toDuration(ts, converter);
        assertEquals(Duration.ZERO, duration);
    }

    @Test
    public void testToDuration_WithNanos() {
        Timestamp ts = createTimestamp(3_000L, 250_000_000); // 3.25 seconds
        Duration duration = TimestampConversions.toDuration(ts, converter);
        Duration expected = Duration.ofSeconds(3).plusNanos(250_000_000L);
        assertEquals(expected, duration);
    }

    @Test
    public void testToOffsetDateTime_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);
        OffsetDateTime expected = Instant.EPOCH.atZone(zoneId).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToOffsetDateTime_WithZoneOffset() {
        ZoneId testZone = ZoneId.of("Europe/Paris"); // UTC+01:00 (or +02:00 DST)
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(testZone);
        Converter testConverter = new Converter(opts);

        Timestamp ts = createTimestamp(1_600_000_000_000L, 0);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, testConverter);
        OffsetDateTime expected = ts.toInstant().atZone(testZone).toOffsetDateTime();

        assertEquals(expected, odt);
    }

    @Test
    public void testToCalendar_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(timeZone, cal.getTimeZone());
        assertEquals(0L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_WithTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(tz);
        Converter testConverter = new Converter(opts);

        Timestamp ts = createTimestamp(1_600_000_000_000L, 0);
        Calendar cal = TimestampConversions.toCalendar(ts, testConverter);
        assertEquals(tz, cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToMap_Epoch() {
        Timestamp ts = createTimestamp(0L, 0);
        Map<String, Object> map = TimestampConversions.toMap(ts, converter);

        // Expected values
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test
    public void testToMap_WithNanosecondsAndCustomZone() {
        ZoneId testZone = ZoneId.of("Australia/Sydney");
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(testZone);
        Converter testConverter = new Converter(opts);

        Timestamp ts = createTimestamp(1_234_567_890L, 999_999_999);
        Map<String, Object> map = TimestampConversions.toMap(ts, testConverter);

        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, testConverter);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(testZone.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WithInvalidObject() {
        // Passing a non‑Timestamp should cause a ClassCastException inside toDuration()
        Date notATimestamp = new Date();
        TimestampConversions.toDouble(notATimestamp, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidObject() {
        // toMap expects a Date (or Timestamp) – passing a plain Object will fail
        Object bogus = new Object();
        TimestampConversions.toMap(bogus, converter);
    }
}
