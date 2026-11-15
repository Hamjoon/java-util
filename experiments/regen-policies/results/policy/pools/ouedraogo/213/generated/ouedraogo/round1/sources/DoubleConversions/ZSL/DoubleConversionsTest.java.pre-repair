package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DoubleConversions}.
 * <p>
 * The test class resides in the same package as {@code DoubleConversions}
 * so that the package‑private static methods can be accessed directly.
 * A {@link Converter} instance with a deterministic UTC configuration is created
 * via reflection (the {@code ConverterOptions} class does not expose public mutators
 * in the public API, but it does contain {@code setZoneId} and {@code setTimeZone}
 * methods that we can invoke safely for testing purposes).
 * </p>
 */
public class DoubleConversionsTest {

    private Converter converter;   // configured to use UTC
    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    @Before
    public void setUp() throws Exception {
        // Create a Converter with default options
        converter = new Converter(new ConverterOptions());

        // Force the options to use UTC – this makes all date‑time results deterministic
        ConverterOptions opts = converter.getOptions();

        // The real ConverterOptions class (in the library) provides the following
        // setter methods.  If they are not public we fall back to reflection.
        try {
            opts.getClass().getMethod("setZoneId", ZoneId.class).invoke(opts, UTC_ZONE);
            opts.getClass().getMethod("setTimeZone", TimeZone.class).invoke(opts, UTC_TZ);
        } catch (NoSuchMethodException e) {
            // Fallback: set the private fields directly
            java.lang.reflect.Field zoneIdField = opts.getClass().getDeclaredField("zoneId");
            zoneIdField.setAccessible(true);
            zoneIdField.set(opts, UTC_ZONE);

            java.lang.reflect.Field timeZoneField = opts.getClass().getDeclaredField("timeZone");
            timeZoneField.setAccessible(true);
            timeZoneField.set(opts, UTC_TZ);
        }
    }

    /** Helper to invoke the package‑private static methods via reflection. */
    private static Object invoke(String methodName, Object argument) throws Exception {
        Method m = DoubleConversions.class.getDeclaredMethod(methodName, Object.class, Converter.class);
        m.setAccessible(true);
        return m.invoke(null, argument, new Converter(new ConverterOptions()));
    }

    // -------------------------------------------------------------------------
    //  toInstant
    // -------------------------------------------------------------------------

    @Test
    public void testToInstant_WithWholeSecondsAndFraction() throws Exception {
        double input = 1622547801.123456789; // arbitrary epoch second with fraction
        Instant result = DoubleConversions.toInstant(input, converter);

        long expectedSeconds = 1622547801L;
        long expectedNanos = Math.round((input - expectedSeconds) * 1_000_000_000L);

        assertEquals(expectedSeconds, result.getEpochSecond());
        assertEquals(expectedNanos, result.getNano());
    }

    // -------------------------------------------------------------------------
    //  toDate
    // -------------------------------------------------------------------------

    @Test
    public void testToDate_ConvertsFromDoubleSeconds() {
        double input = 1_600_000_000.5; // 1,600,000,000.5 seconds since epoch
        Date date = DoubleConversions.toDate(input, converter);
        long expectedMillis = (long) (input * 1000);
        assertEquals(expectedMillis, date.getTime());
    }

    // -------------------------------------------------------------------------
    //  toSqlDate
    // -------------------------------------------------------------------------

    @Test
    public void testToSqlDate_UTCDate() {
        // 2021‑04‑01 00:00:00 UTC => epoch seconds 1617235200
        double input = 1_617_235_200.0;
        java.sql.Date sqlDate = DoubleConversions.toSqlDate(input, converter);
        assertEquals(LocalDate.of(2021, 4, 1), sqlDate.toLocalDate());
    }

    // -------------------------------------------------------------------------
    //  toCalendar
    // -------------------------------------------------------------------------

    @Test
    public void testToCalendar_ProducesCorrectInstant() {
        double input = 1_500_000_000.75; // seconds with fraction
        Calendar cal = DoubleConversions.toCalendar(input, converter);
        long expectedMillis = (long) (input * 1000);
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(UTC_TZ, cal.getTimeZone());
    }

    // -------------------------------------------------------------------------
    //  toLocalTime
    // -------------------------------------------------------------------------

    @Test
    public void testToLocalTime_ValidValue() {
        // 13:45:30.123456789 => seconds since midnight = 13*3600 + 45*60 + 30.123456789
        double seconds = 13 * 3600 + 45 * 60 + 30.123456789;
        LocalTime lt = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(13, 45, 30, 123_456_789), lt);
    }

    @Test
    public void testToLocalTime_NegativeThrows() {
        double bad = -0.1;
        try {
            DoubleConversions.toLocalTime(bad, converter);
            fail("Expected IllegalArgumentException for negative seconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test
    public void testToLocalTime_TooLargeThrows() {
        // 24 hours = 86400 seconds – not allowed (must be <= 86399.999999999)
        double bad = 86_400.0;
        try {
            DoubleConversions.toLocalTime(bad, converter);
            fail("Expected IllegalArgumentException for out‑of‑range seconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399"));
        }
    }

    // -------------------------------------------------------------------------
    //  toLocalDate / toLocalDateTime / toZonedDateTime
    // -------------------------------------------------------------------------

    @Test
    public void testToZonedDateTime_AndDerivedConversions() {
        // 2022‑01‑15 12:34:56.789 UTC
        double epochSeconds = Instant.parse("2022-01-15T12:34:56.789Z").getEpochSecond()
                + 0.789; // add fraction (the nanoseconds part will be rounded inside toInstant)

        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, converter);
        assertEquals(UTC_ZONE, zdt.getZone());
        assertEquals(LocalDate.of(2022, 1, 15), zdt.toLocalDate());
        assertEquals(LocalDateTime.of(2022, 1, 15, 12, 34, 56, 789_000_000), zdt.toLocalDateTime());

        // Verify the convenience wrappers return the same underlying values
        assertEquals(zdt.toLocalDate(), DoubleConversions.toLocalDate(epochSeconds, converter));
        assertEquals(zdt.toLocalDateTime(), DoubleConversions.toLocalDateTime(epochSeconds, converter));
    }

    // -------------------------------------------------------------------------
    //  toOffsetTime
    // -------------------------------------------------------------------------

    @Test
    public void testToOffsetTime_Valid() {
        // 08:15:30.500 (seconds since midnight)
        double seconds = 8 * 3600 + 15 * 60 + 30.5;
        OffsetTime ot = DoubleConversions.toOffsetTime(seconds, converter);
        // OffsetTime should be expressed in UTC (the converter's zone)
        OffsetTime expected = OffsetTime.of(LocalTime.of(8, 15, 30, 500_000_000), ZoneOffset.UTC);
        assertEquals(expected, ot);
    }

    @Test
    public void testToOffsetTime_NegativeThrows() {
        double bad = -5.0;
        try {
            DoubleConversions.toOffsetTime(bad, converter);
            fail("Expected IllegalArgumentException for negative seconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    // -------------------------------------------------------------------------
    //  toOffsetDateTime
    // -------------------------------------------------------------------------

    @Test
    public void testToOffsetDateTime() {
        double epochSeconds = 1_640_995_200.250; // 2022‑01‑01T00:00:00.250Z
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, converter);
        OffsetDateTime expected = Instant.ofEpochSecond(1_640_995_200, 250_000_000)
                .atZone(UTC_ZONE).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    // -------------------------------------------------------------------------
    //  toTimestamp
    // -------------------------------------------------------------------------

    @Test
    public void testToTimestamp() {
        double epochSeconds = 1_600_000_123.456; // arbitrary moment
        Timestamp ts = DoubleConversions.toTimestamp(epochSeconds, converter);
        Instant expected = DoubleConversions.toInstant(epochSeconds, converter);
        assertEquals(Timestamp.from(expected), ts);
    }

    // -------------------------------------------------------------------------
    //  toDuration
    // -------------------------------------------------------------------------

    @Test
    public void testToDuration_WithFraction() {
        double input = 12345.678901234; // 12345 seconds + fraction
        Duration dur = DoubleConversions.toDuration(input, converter);
        long expectedSeconds = 12_345L;
        long expectedNanos = (long) ((input - expectedSeconds) * 1_000_000_000L);
        assertEquals(expectedSeconds, dur.getSeconds());
        assertEquals(expectedNanos, dur.getNano());
    }

    @Test
    public void testToDuration_WholeSecondsOnly() {
        double input = 60.0; // exactly one minute
        Duration dur = DoubleConversions.toDuration(input, converter);
        assertEquals(Duration.ofMinutes(1), dur);
    }
}
