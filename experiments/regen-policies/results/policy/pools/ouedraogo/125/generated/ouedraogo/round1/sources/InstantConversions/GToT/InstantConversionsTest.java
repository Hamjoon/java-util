package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * The tests cover:
 * <ul>
 *   <li>Basic happy‑path conversion for every public static method.</li>
 *   <li>Edge‑case values such as {@code Instant.MIN}, {@code Instant.MAX},
 *       zero epoch, and large nanosecond values.</li>
 *   <li>Null handling – all methods should throw {@link NullPointerException}
 *       when the source object is {@code null}.</li>
 *   <li>Invalid source type – passing a non‑Instant object must result in a
 *       {@link ClassCastException} (the implementation casts directly).</li>
 * </ul>
 *
 * The test uses a {@link Converter} configured with UTC zone information to
 * guarantee deterministic results independent of the host environment.
 */
public class InstantConversionsTest {

    private Converter utcConverter;

    @Before
    public void setUp() {
        // Force the JVM default time‑zone to UTC so that any code that falls back
        // to the default behaves predictably.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Build ConverterOptions with an explicit UTC ZoneId and TimeZone.
        // The real library provides setters for these – they are used here.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /* 1. toMap(Object, Converter)                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMap_HappyPath() {
        Instant instant = Instant.ofEpochSecond(1_234_567L, 123_456_789);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);

        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1_234_567L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInstant() {
        InstantConversions.toMap(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongSourceType() {
        InstantConversions.toMap("not an instant", utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 2. toZonedDateTime(Object, Converter)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_HappyPath() {
        Instant instant = Instant.parse("2023-03-15T12:34:56.789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInstant() {
        InstantConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongSourceType() {
        InstantConversions.toZonedDateTime(42, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 3. toOffsetDateTime(Object, Converter)                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_Utc() {
        Instant instant = Instant.parse("2020-01-01T00:00:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        assertEquals(ZoneId.of("UTC"), odt.getOffset().getId().equals("Z") ? ZoneId.of("UTC") : odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInstant() {
        InstantConversions.toOffsetDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongSourceType() {
        InstantConversions.toOffsetDateTime(new Object(), utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 4. toLong(Object, Converter)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLong_EpochMilli() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_000_000);
        long expected = 1_600_000_000_123L;
        assertEquals(expected, InstantConversions.toLong(instant, utcConverter));
    }

    @Test
    public void testToLong_MinInstant() {
        Instant min = Instant.MIN; // -31557014167219200-01-01T00:00:00Z
        // toEpochMilli may overflow for MIN; we just verify that the method
        // returns the same value as Instant.toEpochMilli()
        assertEquals(min.toEpochMilli(), InstantConversions.toLong(min, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInstant() {
        InstantConversions.toLong(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongSourceType() {
        InstantConversions.toLong(Boolean.TRUE, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 5. toDouble(Object, Converter)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDouble_Basic() {
        Instant instant = Instant.ofEpochSecond(10L, 500_000_000); // 10.5 seconds
        double result = InstantConversions.toDouble(instant, utcConverter);
        assertEquals(10.5, result, 0.000000001);
    }

    @Test
    public void testToDouble_LargeSeconds() {
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE / 1_000_000_000L, 0);
        double result = InstantConversions.toDouble(instant, utcConverter);
        // The double representation should be close to the real value.
        double expected = ((double) instant.getEpochSecond());
        assertEquals(expected, result, Math.abs(expected) * 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInstant() {
        InstantConversions.toDouble(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongSourceType() {
        InstantConversions.toDouble("string", utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 6. toAtomicLong(Object, Converter)                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_HappyPath() {
        Instant instant = Instant.ofEpochMilli(9_876_543_210L);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(9_876_543_210L, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInstant() {
        InstantConversions.toAtomicLong(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 7. toTimestamp(Object, Converter)                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_HappyPath() {
        Instant instant = Instant.parse("2022-12-31T23:59:59.999Z");
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), ts.getTime());
        assertEquals(999_000_000, ts.getNanos());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInstant() {
        InstantConversions.toTimestamp(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 8. toSqlDate(Object, Converter)                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_HappyPath() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_000L);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), sqlDate.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInstant() {
        InstantConversions.toSqlDate(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 9. toDate(Object, Converter)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDate_HappyPath() {
        Instant instant = Instant.now();
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInstant() {
        InstantConversions.toDate(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 10. toCalendar(Object, Converter)                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCalendar_HappyPath() {
        Instant instant = Instant.ofEpochMilli(1_234_567_890L);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
        // The Calendar should also be in UTC.
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInstant() {
        InstantConversions.toCalendar(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 11. toBigInteger(Object, Converter)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic() {
        Instant instant = Instant.ofEpochSecond(2L, 500_000_000);
        // Expected = 2 * 1_000_000_000 + 500_000_000 = 2_500_000_000
        BigInteger expected = BigInteger.valueOf(2_500_000_000L);
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test
    public void testToBigInteger_NegativeInstant() {
        Instant instant = Instant.ofEpochSecond(-3L, 250_000_000);
        // (-3) * 1_000_000_000 + 250_000_000 = -2_750_000_000
        BigInteger expected = BigInteger.valueOf(-2_750_000_000L);
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInstant() {
        InstantConversions.toBigInteger(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 12. toBigDecimal(Object, Converter)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic() {
        Instant instant = Instant.ofEpochSecond(3L, 333_333_333);
        // 3.333333333 seconds as BigDecimal
        BigDecimal expected = new BigDecimal("3.333333333");
        assertEquals(0, expected.compareTo(InstantConversions.toBigDecimal(instant, utcConverter)));
    }

    @Test
    public void testToBigDecimal_NegativeInstant() {
        Instant instant = Instant.ofEpochSecond(-1L, 500_000_000);
        // -0.5 seconds
        BigDecimal expected = new BigDecimal("-0.5");
        assertEquals(0, expected.compareTo(InstantConversions.toBigDecimal(instant, utcConverter)));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInstant() {
        InstantConversions.toBigDecimal(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 13. toLocalDateTime(Object, Converter)                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalDateTime_HappyPath() {
        Instant instant = Instant.parse("2021-07-20T15:45:30.123Z");
        assertEquals(instant.atZone(ZoneId.of("UTC")).toLocalDateTime(),
                     InstantConversions.toLocalDateTime(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInstant() {
        InstantConversions.toLocalDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 14. toLocalDate(Object, Converter)                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_HappyPath() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        assertEquals(instant.atZone(ZoneId.of("UTC")).toLocalDate(),
                     InstantConversions.toLocalDate(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInstant() {
        InstantConversions.toLocalDate(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* 15. toLocalTime(Object, Converter)                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_HappyPath() {
        Instant instant = Instant.parse("2000-01-01T08:15:30.456Z");
        assertEquals(instant.atZone(ZoneId.of("UTC")).toLocalTime(),
                     InstantConversions.toLocalTime(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInstant() {
        InstantConversions.toLocalTime(null, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /* Helper – list of all public static methods for reference               */
    /* --------------------------------------------------------------------- */

    // The following comment enumerates the public static API that is under test:
    //   Map<String,Object> toMap(Object, Converter)
    //   ZonedDateTime toZonedDateTime(Object, Converter)
    //   OffsetDateTime toOffsetDateTime(Object, Converter)
    //   long toLong(Object, Converter)
    //   double toDouble(Object, Converter)
    //   AtomicLong toAtomicLong(Object, Converter)
    //   Timestamp toTimestamp(Object, Converter)
    //   java.sql.Date toSqlDate(Object, Converter)
    //   Date toDate(Object, Converter)
    //   Calendar toCalendar(Object, Converter)
    //   BigInteger toBigInteger(Object, Converter)
    //   BigDecimal toBigDecimal(Object, Converter)
    //   LocalDateTime toLocalDateTime(Object, Converter)
    //   LocalDate toLocalDate(Object, Converter)
    //   LocalTime toLocalTime(Object, Converter)
}
