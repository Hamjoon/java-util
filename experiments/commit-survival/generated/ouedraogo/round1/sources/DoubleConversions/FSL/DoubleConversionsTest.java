package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link DoubleConversions}. All methods are package‑private static,
 * therefore the test class lives in the same package to gain access.
 *
 * The tests use a real {@link Converter} instance with default {@link ConverterOptions}.
 * It is assumed that {@code new Converter(new ConverterOptions())} creates an
 * options object that returns the system default time‑zone and zone‑id.
 */
public class DoubleConversionsTest {

    private static final Converter DEFAULT_CONVERTER = createDefaultConverter();

    private static Converter createDefaultConverter() {
        // The library provides a public constructor that accepts ConverterOptions.
        // We rely on the existence of a no‑arg constructor for ConverterOptions.
        // If the library supplies a different API, adjust accordingly.
        try {
            Class<?> optsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object optsInstance = optsClass.getDeclaredConstructor().newInstance();
            return new Converter(optsInstance);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create default Converter; ensure ConverterOptions has a public no‑arg constructor", e);
        }
    }

    /** Helper to compare two Instants with nanosecond precision. */
    private static void assertInstantEquals(Instant expected, Instant actual) {
        assertEquals("Epoch seconds differ", expected.getEpochSecond(), actual.getEpochSecond());
        assertEquals("Nanoseconds differ", expected.getNano(), actual.getNano());
    }

    @Test
    public void testToInstantWholeAndFractionalSeconds() {
        // 0 seconds
        Instant i0 = DoubleConversions.toInstant(0.0, DEFAULT_CONVERTER);
        assertInstantEquals(Instant.ofEpochSecond(0, 0), i0);

        // Positive whole seconds
        Instant i5 = DoubleConversions.toInstant(5.0, DEFAULT_CONVERTER);
        assertInstantEquals(Instant.ofEpochSecond(5, 0), i5);

        // Positive with fractional part – 1.75 seconds = 1 sec + 750_000_000 ns
        Instant i1_75 = DoubleConversions.toInstant(1.75, DEFAULT_CONVERTER);
        assertInstantEquals(Instant.ofEpochSecond(1, 750_000_000), i1_75);

        // Negative seconds with fraction – -2.3 seconds = -3 sec + 700_000_000 ns
        // because Instant normalises the sign.
        Instant iNeg = DoubleConversions.toInstant(-2.3, DEFAULT_CONVERTER);
        // Math.round((-2.3 - (-2)) * 1_000_000_000) = Math.round(-0.3 * 1e9) = -300_000_000
        // The implementation casts (long)seconds = -2, then adds nanos = -300_000_000,
        // which Instant.ofEpochSecond normalises to -3 seconds + 700_000_000 ns.
        Instant expectedNeg = Instant.ofEpochSecond(-3, 700_000_000);
        assertInstantEquals(expectedNeg, iNeg);
    }

    @Test
    public void testToInstantRounding() {
        // 1.000000001 seconds → 1 sec + 1 ns (rounded)
        Instant i = DoubleConversions.toInstant(1.000000001, DEFAULT_CONVERTER);
        assertInstantEquals(Instant.ofEpochSecond(1, 1), i);

        // 1.999999999 seconds → 2 sec + 0 ns after rounding (since 0.999999999*1e9 = 999_999_999 ≈ 1_000_000_000)
        Instant i2 = DoubleConversions.toInstant(1.999999999, DEFAULT_CONVERTER);
        assertInstantEquals(Instant.ofEpochSecond(2, 0), i2);
    }

    @Test
    public void testToDateAndSqlDate() {
        // 0.0 seconds → epoch
        Date d0 = DoubleConversions.toDate(0.0, DEFAULT_CONVERTER);
        assertEquals(0L, d0.getTime());

        // 1.5 seconds → 1500 ms
        Date d1_5 = DoubleConversions.toDate(1.5, DEFAULT_CONVERTER);
        assertEquals(1500L, d1_5.getTime());

        // Verify java.sql.Date conversion yields same epoch day (time part truncated)
        java.sql.Date sqlDate = (java.sql.Date) DoubleConversions.toSqlDate(86400.0, DEFAULT_CONVERTER);
        // 86400 seconds = 1 day → epoch day = 1 (1970‑01‑02)
        assertEquals(86_400_000L, sqlDate.getTime());
    }

    @Test
    public void testToCalendarTimeZonePreservation() {
        // Use a known time‑zone to make the test deterministic.
        TimeZone utc = TimeZone.getTimeZone("UTC");
        // Create a Converter with options that return UTC.
        Converter utcConverter = createConverterWithZone(utc, ZoneId.of("UTC"));

        // 12345.678 seconds → 12_345_678 ms
        Calendar cal = DoubleConversions.toCalendar(12_345.678, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(12_345_678L, cal.getTimeInMillis());
    }

    private Converter createConverterWithZone(TimeZone tz, ZoneId zid) {
        try {
            Class<?> optsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object opts = optsClass.getDeclaredConstructor().newInstance();

            // Use reflection to set the time‑zone fields if they exist.
            // The actual field names are unknown; we attempt common ones.
            try {
                java.lang.reflect.Method setTz = optsClass.getMethod("setTimeZone", TimeZone.class);
                setTz.invoke(opts, tz);
            } catch (NoSuchMethodException ignored) { }

            try {
                java.lang.reflect.Method setZid = optsClass.getMethod("setZoneId", ZoneId.class);
                setZid.invoke(opts, zid);
            } catch (NoSuchMethodException ignored) { }

            return new Converter(opts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Converter with specific zone", e);
        }
    }

    @Test
    public void testToLocalTimeValidValues() {
        // Midnight
        LocalTime lt0 = DoubleConversions.toLocalTime(0.0, DEFAULT_CONVERTER);
        assertEquals(LocalTime.MIDNIGHT, lt0);

        // 12:34:56.789123456 -> seconds = 45296.789123456
        double seconds = 12 * 3600 + 34 * 60 + 56.789123456;
        LocalTime expected = LocalTime.of(12, 34, 56, 789_123_456);
        assertEquals(expected, DoubleConversions.toLocalTime(seconds, DEFAULT_CONVERTER));

        // Maximum allowed value (just under 24h)
        double max = 86_399.999_999_999; // 23:59:59.999999999
        LocalTime maxTime = DoubleConversions.toLocalTime(max, DEFAULT_CONVERTER);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), maxTime);
    }

    @Test
    public void testToLocalTimeOutOfRange() {
        // Value greater than the number of seconds in a day should throw IllegalArgumentException
        double tooBig = 86_400.0; // exactly 24h
        try {
            DoubleConversions.toLocalTime(tooBig, DEFAULT_CONVERTER);
            fail("Expected IllegalArgumentException for out‑of‑range LocalTime");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }

        // Negative value also illegal
        try {
            DoubleConversions.toLocalTime(-1.0, DEFAULT_CONVERTER);
            fail("Expected IllegalArgumentException for negative LocalTime");
        } catch (IllegalArgumentException e) {
            // message already verified above
        }
    }

    @Test
    public void testToZonedAndOffsetDateTime() {
        // Use a fixed instant for reproducibility: 2021‑03‑14T15:09:26Z (epoch seconds 1615734566)
        double epochSeconds = 1_615_734_566.0;
        // Use UTC zone for deterministic result
        Converter utcConverter = createConverterWithZone(TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"));

        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, utcConverter);
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
        assertEquals(2021, zdt.getYear());
        assertEquals(3, zdt.getMonthValue());
        assertEquals(14, zdt.getDayOfMonth());
        assertEquals(15, zdt.getHour());
        assertEquals(9, zdt.getMinute());
        assertEquals(26, zdt.getSecond());

        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, utcConverter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToLocalDateAndLocalDateTime() {
        // Same epoch as previous test, UTC zone.
        double epochSeconds = 1_615_734_566.0;
        Converter utcConverter = createConverterWithZone(TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"));

        LocalDate ld = DoubleConversions.toLocalDate(epochSeconds, utcConverter);
        assertEquals(LocalDate.of(2021, 3, 14), ld);

        LocalDateTime ldt = DoubleConversions.toLocalDateTime(epochSeconds, utcConverter);
        assertEquals(LocalDateTime.of(2021, 3, 14, 15, 9, 26), ldt);
    }

    @Test
    public void testToTimestamp() {
        double epochSec = 1_600_000_000.123; // ~2020‑09‑13T12:26:40.123Z
        Converter utcConverter = createConverterWithZone(TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"));

        Timestamp ts = DoubleConversions.toTimestamp(epochSec, utcConverter);
        Instant expectedInstant = DoubleConversions.toInstant(epochSec, utcConverter);
        assertEquals(Timestamp.from(expectedInstant), ts);
        assertEquals(expectedInstant.getEpochSecond(), ts.getTime() / 1000L);
        // Verify nanos part
        assertEquals(expectedInstant.getNano(), ts.getNanos());
    }

    @Test
    public void testToDurationWholeAndFractional() {
        // 5 seconds exactly
        Duration d5 = DoubleConversions.toDuration(5.0, DEFAULT_CONVERTER);
        assertEquals(Duration.ofSeconds(5), d5);

        // 2.75 seconds → 2 sec + 750_000_000 ns
        Duration d2_75 = DoubleConversions.toDuration(2.75, DEFAULT_CONVERTER);
        assertEquals(Duration.ofSeconds(2, 750_000_000), d2_75);

        // Negative duration – allowed by Duration.ofSeconds
        Duration dNeg = DoubleConversions.toDuration(-3.5, DEFAULT_CONVERTER);
        assertEquals(Duration.ofSeconds(-4, 500_000_000), dNeg);
    }
}
