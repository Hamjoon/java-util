package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

public class TimestampConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // Use the default zone (UTC) and default time zone for deterministic results
        options = new ConverterOptions(ZoneId.of("UTC"), TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    /* -------------------------------------------------
     *  toDouble tests
     * ------------------------------------------------- */

    @Test
    public void testToDouble_Basic() {
        Timestamp ts = Timestamp.valueOf("2023-01-02 12:34:56.123456789");
        double result = TimestampConversions.toDouble(ts, converter);
        // Expected seconds since epoch + nanos fraction
        double expected = ts.getTime() / 1000.0 + ts.getNanos() % 1_000_000_000 / 1_000_000_000.0;
        assertEquals(expected, result, 1e-9);
    }

    @Test
    public void testToDouble_Epoch() {
        Timestamp ts = new Timestamp(0L);
        ts.setNanos(0);
        double result = TimestampConversions.toDouble(ts, converter);
        assertEquals(0.0, result, 0.0);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_InvalidInput() {
        // Passing a non‑Timestamp should cause a ClassCastException inside toDuration
        TimestampConversions.toDouble("not a timestamp", converter);
    }

    /* -------------------------------------------------
     *  toBigDecimal tests
     * ------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic() {
        Timestamp ts = Timestamp.valueOf("2023-06-15 08:00:00.500");
        BigDecimal result = TimestampConversions.toBigDecimal(ts, converter);
        // Instant of the timestamp
        Instant instant = ts.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_InvalidInput() {
        TimestampConversions.toBigDecimal(12345, converter);
    }

    /* -------------------------------------------------
     *  toBigInteger tests
     * ------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic() {
        Timestamp ts = Timestamp.valueOf("1970-01-02 00:00:01.000000001");
        BigInteger result = TimestampConversions.toBigInteger(ts, converter);
        Duration duration = Duration.between(Instant.EPOCH, ts.toInstant());
        BigInteger expected = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger_NegativeDuration() {
        // Timestamp before epoch
        Timestamp ts = new Timestamp(-86_400_000L); // -1 day
        ts.setNanos(0);
        BigInteger result = TimestampConversions.toBigInteger(ts, converter);
        Duration duration = Duration.between(Instant.EPOCH, ts.toInstant());
        BigInteger expected = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------
     *  toLocalDateTime tests
     * ------------------------------------------------- */

    @Test
    public void testToLocalDateTime_Basic() {
        Timestamp ts = Timestamp.valueOf("2022-12-31 23:59:59.999999999");
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, converter);
        LocalDateTime expected = ts.toInstant()
                .atZone(options.getZoneId())
                .toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_InvalidInput() {
        TimestampConversions.toLocalDateTime(new Date(), converter);
    }

    /* -------------------------------------------------
     *  toDuration tests
     * ------------------------------------------------- */

    @Test
    public void testToDuration_Basic() {
        Timestamp ts = Timestamp.valueOf("2021-05-01 12:00:00.000");
        Duration d = TimestampConversions.toDuration(ts, converter);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, d);
    }

    @Test
    public void testToDuration_Null() {
        try {
            TimestampConversions.toDuration(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    /* -------------------------------------------------
     *  toOffsetDateTime tests
     * ------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_Basic() {
        Timestamp ts = Timestamp.valueOf("2020-02-29 10:20:30.123456789");
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(ts.toInstant(), options.getZoneId());
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToOffsetDateTime_WithDifferentZone() {
        // Change converter options to a non‑UTC zone
        ConverterOptions opt = new ConverterOptions(ZoneId.of("America/New_York"), TimeZone.getTimeZone("America/New_York"));
        Converter conv = new Converter(opt);
        Timestamp ts = Timestamp.valueOf("2020-06-01 12:00:00.0");
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, conv);
        ZonedDateTime expected = ZonedDateTime.ofInstant(ts.toInstant(), opt.getZoneId());
        assertEquals(expected.toOffsetDateTime(), odt);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_InvalidInput() {
        TimestampConversions.toOffsetDateTime("string", converter);
    }

    /* -------------------------------------------------
     *  toCalendar tests
     * ------------------------------------------------- */

    @Test
    public void testToCalendar_Basic() {
        Timestamp ts = Timestamp.valueOf("2019-11-11 11:11:11.111");
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(ts.getTime(), cal.getTimeInMillis());
        assertEquals(options.getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToCalendar_Null() {
        try {
            TimestampConversions.toCalendar(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    /* -------------------------------------------------
     *  toMap tests
     * ------------------------------------------------- */

    @Test
    public void testToMap_Basic() {
        Date date = new Date(1_600_000_000_000L); // 2020-09-13T12:26:40Z
        Map<String, Object> map = TimestampConversions.toMap(date, converter);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(date, converter);

        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(options.getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(date.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
        assertTrue(map instanceof CompactLinkedMap);
    }

    @Test
    public void testToMap_NullDate() {
        try {
            TimestampConversions.toMap(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToMap_Epoch() {
        Date epoch = new Date(0L);
        Map<String, Object> map = TimestampConversions.toMap(epoch, converter);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(epoch, converter);
        assertEquals("1970-01-01", map.get(MapConversions.DATE));
        assertEquals("00:00", ((String) map.get(MapConversions.TIME)).substring(0,5)); // ignore nanos part
        assertEquals(0L, map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    /* -------------------------------------------------
     *  Helper methods for internal conversions used in tests
     * ------------------------------------------------- */

    // Minimal stub of ConverterOptions required for the tests
    private static class ConverterOptions {
        private final ZoneId zoneId;
        private final TimeZone timeZone;

        public ConverterOptions(ZoneId zoneId, TimeZone timeZone) {
            this.zoneId = zoneId;
            this.timeZone = timeZone;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }
    }

    // Minimal stub of Converter that delegates to the provided options.
    // The real Converter has many methods, but only getOptions() is needed here.
    private static class Converter {
        private final ConverterOptions options;

        public Converter(ConverterOptions options) {
            this.options = options;
        }

        public ConverterOptions getOptions() {
            return options;
        }
    }

    // Minimal stubs for static conversion helpers used by TimestampConversions.
    // These mirror the expected behavior of the real library.

    private static class InstantConversions {
        static BigDecimal toBigDecimal(Instant instant, Converter converter) {
            // Represent the instant as seconds + nanos as a BigDecimal
            BigDecimal seconds = new BigDecimal(instant.getEpochSecond());
            BigDecimal nanos = new BigDecimal(instant.getNano()).divide(new BigDecimal(1_000_000_000));
            return seconds.add(nanos);
        }
    }

    private static class DurationConversions {
        static BigInteger toBigInteger(Duration duration, Converter converter) {
            // Convert total nanoseconds to BigInteger
            return BigInteger.valueOf(duration.getSeconds())
                    .multiply(BigInteger.valueOf(1_000_000_000L))
                    .add(BigInteger.valueOf(duration.getNano()));
        }
    }

    private static class BigDecimalConversions {
        static BigDecimal secondsAndNanosToDouble(long seconds, int nanos) {
            BigDecimal s = new BigDecimal(seconds);
            BigDecimal n = new BigDecimal(nanos).divide(new BigDecimal(1_000_000_000));
            return s.add(n);
        }
    }

    private static class MapConversions {
        static final String DATE = "date";
        static final String TIME = "time";
        static final String ZONE = "zone";
        static final String EPOCH_MILLIS = "epochMillis";
        static final String NANOS = "nanos";
    }
}
