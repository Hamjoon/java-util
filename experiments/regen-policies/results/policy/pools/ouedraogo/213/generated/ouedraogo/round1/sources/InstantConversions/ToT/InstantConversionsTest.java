package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link InstantConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions (valid Instant → target type)</li>
 *   <li>Edge cases (null source, extreme epoch values)</li>
 *   <li>Behaviour that depends on {@link ConverterOptions} (zone‑id / time‑zone handling)</li>
 * </ul>
 *
 * The library’s {@link Converter} is instantiated with the default
 * {@link ConverterOptions}.  The default options use the JVM’s
 * {@link ZoneId#systemDefault()} and the corresponding {@link TimeZone},
 * which is sufficient for deterministic assertions because the expected
 * values are derived from the same defaults.
 */
public class InstantConversionsTest
{
    /** A fixed instant used by most tests – 2023‑03‑25T12:34:56.789123456Z */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-03-25T12:34:56.789123456Z");

    /** Converter instance with default options (system zone & time‑zone). */
    private Converter converter;

    @Before
    public void setUp()
    {
        // The library supplies a public constructor that accepts a ConverterOptions instance.
        // Using the no‑arg constructor of ConverterOptions gives us the defaults.
        ConverterOptions options = new ConverterOptions();          // defaults → system zone
        converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  toMap()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_Typical()
    {
        Map<String, Object> map = InstantConversions.toMap(FIXED_INSTANT, converter);
        assertNotNull(map);
        assertEquals(1, map.size());

        // The key is defined in MapConversions.INSTANT (a constant string)
        String expectedKey = com.cedarsoftware.util.convert.MapConversions.INSTANT;
        assertTrue(map.containsKey(expectedKey));

        // The value must be the ISO‑8601 string representation of the instant
        assertEquals(FIXED_INSTANT.toString(), map.get(expectedKey));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NullSource()
    {
        // The method casts the Object to Instant – a null source triggers NPE before the cast,
        // but the library’s contract expects a ClassCastException for an invalid type.
        InstantConversions.toMap(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toZonedDateTime()                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_DefaultZone()
    {
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(FIXED_INSTANT, converter);
        ZoneId expectedZone = converter.getOptions().getZoneId();

        assertEquals(expectedZone, zdt.getZone());
        assertEquals(FIXED_INSTANT, zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_WithExplicitZone()
    {
        // Change the zone inside the ConverterOptions via reflection (allowed – JDK API only)
        ZoneId newZone = ZoneId.of("America/New_York");
        setOptionZoneId(converter, newZone);

        ZonedDateTime zdt = InstantConversions.toZonedDateTime(FIXED_INSTANT, converter);
        assertEquals(newZone, zdt.getZone());

        // The instant part must stay unchanged
        assertEquals(FIXED_INSTANT, zdt.toInstant());

        // Verify the local date‑time reflects the offset of New York at the instant
        LocalDateTime expectedLdt = LocalDateTime.ofInstant(FIXED_INSTANT, newZone);
        assertEquals(expectedLdt, zdt.toLocalDateTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toOffsetDateTime()                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_DefaultTimeZone()
    {
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(FIXED_INSTANT, converter);
        TimeZone tz = converter.getOptions().getTimeZone();

        // Compute the expected offset in seconds using the same logic as the method
        int offsetMillis = tz.getOffset(FIXED_INSTANT.toEpochMilli());
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);

        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(FIXED_INSTANT, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_WithCustomTimeZone()
    {
        // Use a fixed offset time‑zone (UTC+02:30)
        TimeZone customTz = TimeZone.getTimeZone("GMT+02:30");
        setOptionTimeZone(converter, customTz);

        OffsetDateTime odt = InstantConversions.toOffsetDateTime(FIXED_INSTANT, converter);
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(2, 30);
        assertEquals(expected, odt.getOffset());
        assertEquals(FIXED_INSTANT, odt.toInstant());
    }

    /* ---------------------------------------------------------------------- */
    /*  toLong()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLong_Typical()
    {
        long epochMilli = InstantConversions.toLong(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToLong_EpochBoundary()
    {
        // Test the earliest representable Instant (1970‑01‑01T00:00:00Z)
        Instant epoch = Instant.ofEpochMilli(0);
        assertEquals(0L, InstantConversions.toLong(epoch, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toDouble()                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDouble_Typical()
    {
        double d = InstantConversions.toDouble(FIXED_INSTANT, converter);
        // Expected value = seconds + nanos / 1_000_000_000
        double expected = FIXED_INSTANT.getEpochSecond() + FIXED_INSTANT.getNano() / 1_000_000_000d;
        assertEquals(expected, d, 1e-12);
    }

    @Test
    public void testToDouble_NegativeInstant()
    {
        Instant beforeEpoch = Instant.ofEpochSecond(-1, 500_000_000); // -0.5 seconds
        double d = InstantConversions.toDouble(beforeEpoch, converter);
        double expected = -1 + 0.5; // because nanos are positive
        assertEquals(expected, d, 1e-12);
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicLong()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong()
    {
        AtomicLong al = InstantConversions.toAtomicLong(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), al.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  toTimestamp()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimestamp()
    {
        Timestamp ts = InstantConversions.toTimestamp(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT, ts.toInstant());
    }

    /* ---------------------------------------------------------------------- */
    /*  toSqlDate()                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_DefaultZone()
    {
        java.sql.Date sqlDate = InstantConversions.toSqlDate(FIXED_INSTANT, converter);
        LocalDate expected = FIXED_INSTANT.atZone(converter.getOptions().getZoneId()).toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }

    @Test
    public void testToSqlDate_WithDifferentZone()
    {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        setOptionZoneId(converter, zone);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(FIXED_INSTANT, converter);
        LocalDate expected = FIXED_INSTANT.atZone(zone).toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }

    /* ---------------------------------------------------------------------- */
    /*  toDate()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDate()
    {
        Date date = InstantConversions.toDate(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), date.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toCalendar()                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar()
    {
        Calendar cal = InstantConversions.toCalendar(FIXED_INSTANT, converter);
        assertEquals(FIXED_INSTANT.toEpochMilli(), cal.getTimeInMillis());

        // The calendar must use the same time‑zone as the ConverterOptions
        TimeZone expectedTz = converter.getOptions().getTimeZone();
        assertEquals(expectedTz, cal.getTimeZone());
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigInteger()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Typical()
    {
        BigInteger bi = InstantConversions.toBigInteger(FIXED_INSTANT, converter);
        BigInteger expected = BigInteger.valueOf(FIXED_INSTANT.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(FIXED_INSTANT.getNano()));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigInteger_NegativeInstant()
    {
        Instant neg = Instant.ofEpochSecond(-10, 123_456_789);
        BigInteger bi = InstantConversions.toBigInteger(neg, converter);
        BigInteger expected = BigInteger.valueOf(-10)
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789));
        assertEquals(expected, bi);
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigDecimal()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Typical()
    {
        BigDecimal bd = InstantConversions.toBigDecimal(FIXED_INSTANT, converter);
        // The conversion uses the same helper as toDouble()
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(
                FIXED_INSTANT.getEpochSecond(),
                FIXED_INSTANT.getNano());
        assertEquals(0, expected.compareTo(bd));
    }

    /* ---------------------------------------------------------------------- */
    /*  toLocalDateTime(), toLocalDate(), toLocalTime()                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLocalDateTime_FromZoned()
    {
        LocalDateTime ldt = InstantConversions.toLocalDateTime(FIXED_INSTANT, converter);
        LocalDateTime expected = FIXED_INSTANT.atZone(converter.getOptions().getZoneId()).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testLocalDate_FromZoned()
    {
        LocalDate ld = InstantConversions.toLocalDate(FIXED_INSTANT, converter);
        LocalDate expected = FIXED_INSTANT.atZone(converter.getOptions().getZoneId()).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testLocalTime_FromZoned()
    {
        LocalTime lt = InstantConversions.toLocalTime(FIXED_INSTANT, converter);
        LocalTime expected = FIXED_INSTANT.atZone(converter.getOptions().getZoneId()).toLocalTime();
        assertEquals(expected, lt);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper methods – reflection based mutation of ConverterOptions          */
    /* ---------------------------------------------------------------------- */

    /**
     * Sets a new {@link ZoneId} into the {@link ConverterOptions} held by the given converter.
     * The library does not expose a public mutator, so reflection is used (still part of the JDK).
     */
    private static void setOptionZoneId(Converter converter, ZoneId zoneId)
    {
        try
        {
            java.lang.reflect.Field optsField = Converter.class.getDeclaredField("options");
            optsField.setAccessible(true);
            Object options = optsField.get(converter);

            java.lang.reflect.Method setter = options.getClass().getMethod("setZoneId", ZoneId.class);
            setter.invoke(options, zoneId);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to set ZoneId via reflection", e);
        }
    }

    /**
     * Sets a new {@link TimeZone} into the {@link ConverterOptions} held by the given converter.
     */
    private static void setOptionTimeZone(Converter converter, TimeZone tz)
    {
        try
        {
            java.lang.reflect.Field optsField = Converter.class.getDeclaredField("options");
            optsField.setAccessible(true);
            Object options = optsField.get(converter);

            java.lang.reflect.Method setter = options.getClass().getMethod("setTimeZone", TimeZone.class);
            setter.invoke(options, tz);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to set TimeZone via reflection", e);
        }
    }
}
