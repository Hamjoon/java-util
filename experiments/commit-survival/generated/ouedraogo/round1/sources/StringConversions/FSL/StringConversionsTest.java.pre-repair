package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringConversions}.
 *
 * The tests exercise every public static conversion method, checking
 * normal operation, edge cases and the expected exceptions.
 *
 * A single {@link Converter} instance is created once and reused; the
 * default {@link ConverterOptions} supplied by the library are sufficient
 * for the charset (UTF‑8) and zone‑id (UTC) used by the conversions.
 */
public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a no‑arg constructor for ConverterOptions.
        // Using the default options (UTF‑8 charset, UTC zone) is enough for
        // all conversion methods exercised here.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------
     *  asString
     * ------------------------------------------------------- */
    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    /* -------------------------------------------------------
     *  toByte
     * ------------------------------------------------------- */
    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte)127), StringConversions.toByte("127", converter));
        // value larger than Byte.MAX_VALUE – should be rounded down via toLong()
        assertEquals(Byte.valueOf((byte)12), StringConversions.toByte("12.9", converter));
        // out‑of‑range value causes IllegalArgumentException
        try {
            StringConversions.toByte("200", converter);
            fail("Expected IllegalArgumentException for out‑of‑range byte");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toShort
     * ------------------------------------------------------- */
    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short)0), StringConversions.toShort("", converter));
        assertEquals(Short.valueOf((short)32767), StringConversions.toShort("32767", converter));
        // decimal string is rounded down
        assertEquals(Short.valueOf((short)123), StringConversions.toShort("123.9", converter));
        // out‑of‑range
        try {
            StringConversions.toShort("40000", converter);
            fail("Expected IllegalArgumentException for out‑of‑range short");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toInt
     * ------------------------------------------------------- */
    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
        assertEquals(Integer.valueOf(42), StringConversions.toInt("42", converter));
        // decimal string rounded down
        assertEquals(Integer.valueOf(99), StringConversions.toInt("99.99", converter));
        // out‑of‑range
        try {
            StringConversions.toInt("3000000000", converter);
            fail("Expected IllegalArgumentException for out‑of‑range int");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toLong
     * ------------------------------------------------------- */
    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
        assertEquals(Long.valueOf(123456789L), StringConversions.toLong("123456789", converter));
        // decimal rounded down
        assertEquals(Long.valueOf(5L), StringConversions.toLong("5.999", converter));
        // out‑of‑range
        try {
            StringConversions.toLong("9223372036854775808", converter);
            fail("Expected IllegalArgumentException for out‑of‑range long");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toFloat
     * ------------------------------------------------------- */
    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
        try {
            StringConversions.toFloat("not-a-float", converter);
            fail("Expected IllegalArgumentException for invalid float");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toDouble
     * ------------------------------------------------------- */
    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
        try {
            StringConversions.toDouble("NaNstring", converter);
            fail("Expected IllegalArgumentException for invalid double");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toAtomicBoolean / toBoolean
     * ------------------------------------------------------- */
    @Test
    public void testToBoolean() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    @Test
    public void testToAtomicBoolean() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
    }

    /* -------------------------------------------------------
     *  toAtomicInteger / toAtomicLong
     * ------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        assertEquals(7, StringConversions.toAtomicInteger("7", converter).get());
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(123L, StringConversions.toAtomicLong("123", converter).get());
    }

    /* -------------------------------------------------------
     *  toCharacter
     * ------------------------------------------------------- */
    @Test
    public void testToCharacter() {
        // empty string → null char (0)
        assertEquals((char)0, StringConversions.toCharacter("", converter));

        // single character
        assertEquals('Z', StringConversions.toCharacter("Z", converter));

        // numeric Unicode code point
        assertEquals('A', StringConversions.toCharacter("65", converter));

        // Unicode escape
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));

        // invalid Unicode escape
        try {
            StringConversions.toCharacter("\\uXYZ", converter);
            fail("Expected IllegalArgumentException for malformed Unicode escape");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toBigInteger
     * ------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
        try {
            StringConversions.toBigInteger("not-a-number", converter);
            fail("Expected IllegalArgumentException for invalid BigInteger");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.14159"), StringConversions.toBigDecimal("3.14159", converter));
        try {
            StringConversions.toBigDecimal("abc", converter);
            fail("Expected IllegalArgumentException for invalid BigDecimal");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toURL / toURI
     * ------------------------------------------------------- */
    @Test
    public void testToURL() throws Exception {
        assertNull(StringConversions.toURL("", converter));
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());

        try {
            StringConversions.toURL("ht!tp://bad url", converter);
            fail("Expected IllegalArgumentException for malformed URL");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToURI() {
        assertNull(StringConversions.toURI("", converter));
        URI uri = StringConversions.toURI("http://example.org/path", converter);
        assertEquals("/path", uri.getPath());
    }

    /* -------------------------------------------------------
     *  enumToString
     * ------------------------------------------------------- */
    private enum SampleEnum { ONE, TWO }

    @Test
    public void testEnumToString() {
        assertEquals("ONE", StringConversions.enumToString(SampleEnum.ONE, converter));
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    /* -------------------------------------------------------
     *  toUUID
     * ------------------------------------------------------- */
    @Test
    public void testToUUID() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));

        try {
            StringConversions.toUUID("not-a-uuid", converter);
            fail("Expected IllegalArgumentException for invalid UUID");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toDuration
     * ------------------------------------------------------- */
    @Test
    public void testToDuration() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofMillis(20345), d);

        try {
            StringConversions.toDuration("not-a-duration", converter);
            fail("Expected IllegalArgumentException for invalid Duration");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toClass
     * ------------------------------------------------------- */
    @Test
    public void testToClass() {
        Class<?> cls = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, cls);

        try {
            StringConversions.toClass("non.existent.ClassName", converter);
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  toMonthDay
     * ------------------------------------------------------- */
    @Test
    public void testToMonthDay() {
        // ISO format
        MonthDay md1 = StringConversions.toMonthDay("--12-03", converter);
        assertEquals(MonthDay.of(12, 3), md1);

        // Simple mm.dd pattern
        MonthDay md2 = StringConversions.toMonthDay("4.5", converter);
        assertEquals(MonthDay.of(4, 5), md2);

        // Date string fallback
        MonthDay md3 = StringConversions.toMonthDay("2021-07-15T10:00:00Z", converter);
        assertEquals(MonthDay.of(7, 15), md3);
    }

    /* -------------------------------------------------------
     *  toYearMonth
     * ------------------------------------------------------- */
    @Test
    public void testToYearMonth() {
        YearMonth ym1 = StringConversions.toYearMonth("2020-11", converter);
        assertEquals(YearMonth.of(2020, 11), ym1);

        // Date fallback
        YearMonth ym2 = StringConversions.toYearMonth("2022-03-04T12:00Z", converter);
        assertEquals(YearMonth.of(2022, 3), ym2);
    }

    /* -------------------------------------------------------
     *  toPeriod
     * ------------------------------------------------------- */
    @Test
    public void testToPeriod() {
        Period p = StringConversions.toPeriod("P2Y3M4D", converter);
        assertEquals(Period.of(2, 3, 4), p);

        try {
            StringConversions.toPeriod("bad-period", converter);
            fail("Expected IllegalArgumentException for invalid Period");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  Date / Time conversions
     * ------------------------------------------------------- */
    private static final String ISO_INSTANT = "2020-01-02T03:04:05Z";

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime(ISO_INSTANT, converter);
        assertEquals(Instant.parse(ISO_INSTANT), zdt.toInstant());
    }

    @Test
    public void testToDateAndRelated() {
        Date date = StringConversions.toDate(ISO_INSTANT, converter);
        assertEquals(Date.from(Instant.parse(ISO_INSTANT)), date);

        java.sql.Date sqlDate = StringConversions.toSqlDate(ISO_INSTANT, converter);
        assertEquals(java.sql.Date.from(Instant.parse(ISO_INSTANT)), sqlDate);

        Timestamp ts = StringConversions.toTimestamp(ISO_INSTANT, converter);
        assertEquals(Timestamp.from(Instant.parse(ISO_INSTANT)), ts);
    }

    @Test
    public void testToLocalDateAndTime() {
        LocalDate ld = StringConversions.toLocalDate(ISO_INSTANT, converter);
        assertEquals(LocalDate.of(2020, 1, 2), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime(ISO_INSTANT, converter);
        assertEquals(LocalDateTime.of(2020, 1, 2, 3, 4, 5), ldt);

        LocalTime lt = StringConversions.toLocalTime("12:34:56", converter);
        assertEquals(LocalTime.of(12, 34, 56), lt);

        // fallback via ZonedDateTime
        LocalTime lt2 = StringConversions.toLocalTime(ISO_INSTANT, converter);
        assertEquals(LocalTime.of(3, 4, 5), lt2);
    }

    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant(ISO_INSTANT, converter);
        assertEquals(Instant.parse(ISO_INSTANT), instant);
    }

    @Test
    public void testToTimeZoneAndCalendar() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals(TimeZone.getTimeZone("UTC"), tz);

        assertNull(StringConversions.toTimeZone("   ", converter));

        Calendar cal = StringConversions.toCalendar(ISO_INSTANT, converter);
        assertNotNull(cal);
        assertEquals(ZoneId.of("UTC"), cal.getTimeZone().toZoneId());
        assertEquals(Instant.parse(ISO_INSTANT).toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToZoneIdAndOffset() {
        ZoneId zid = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid);

        // fallback to TimeZone when ZoneId.of fails
        ZoneId zid2 = StringConversions.toZoneId("GMT+02:00", converter);
        assertEquals(ZoneId.of("GMT+02:00"), zid2);

        // invalid zone → exception
        try {
            StringConversions.toZoneId("no-such-zone", converter);
            fail("Expected IllegalArgumentException for unknown zone");
        } catch (IllegalArgumentException ignored) {}

        ZoneOffset zo = StringConversions.toZoneOffset("-05:00", converter);
        assertEquals(ZoneOffset.of("-05:00"), zo);

        try {
            StringConversions.toZoneOffset("bad-offset", converter);
            fail("Expected IllegalArgumentException for bad offset");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testOffsetDateTimeAndTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime(ISO_INSTANT, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.parse(ISO_INSTANT), ZoneOffset.UTC), odt);

        OffsetTime ot = StringConversions.toOffsetTime("12:34:56+02:00", converter);
        assertEquals(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2)), ot);

        // fallback via OffsetDateTime
        OffsetTime ot2 = StringConversions.toOffsetTime(ISO_INSTANT, converter);
        assertEquals(OffsetTime.of(3, 4, 5, 0, ZoneOffset.UTC), ot2);
    }

    /* -------------------------------------------------------
     *  toLocale
     * ------------------------------------------------------- */
    @Test
    public void testToLocale() {
        assertNull(StringConversions.toLocale("", converter));
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), loc);
    }

    /* -------------------------------------------------------
     *  toCharArray / toCharacterArray / toCharBuffer
     * ------------------------------------------------------- */
    @Test
    public void testCharConversions() {
        char[] chars = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a', 'b', 'c'}, chars);

        char[] empty = StringConversions.toCharArray("", converter);
        assertArrayEquals(new char[0], empty);

        Character[] objChars = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[]{'x', 'y', 'z'}, objChars);

        CharBuffer buffer = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", buffer.toString());
    }

    /* -------------------------------------------------------
     *  toByteArray / toByteBuffer
     * ------------------------------------------------------- */
    @Test
    public void testByteConversions() {
        byte[] bytes = StringConversions.toByteArray("AB", converter);
        assertArrayEquals(new byte[]{'A', 'B'}, bytes);

        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));

        ByteBuffer bb = StringConversions.toByteBuffer("CD", converter);
        assertEquals(ByteBuffer.wrap(new byte[]{'C', 'D'}), bb);
    }

    /* -------------------------------------------------------
     *  toString / StringBuffer / StringBuilder
     * ------------------------------------------------------- */
    @Test
    public void testStringObjectConversions() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("test", StringConversions.toString("test", converter));

        assertNull(StringConversions.toStringBuffer(null, converter));
        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));

        assertNull(StringConversions.toStringBuilder(null, converter));
        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
    }

    /* -------------------------------------------------------
     *  toYear
     * ------------------------------------------------------- */
    @Test
    public void testToYear() {
        Year y = StringConversions.toYear("2021", converter);
        assertEquals(Year.of(2021), y);

        // fallback via ZonedDateTime
        Year y2 = StringConversions.toYear(ISO_INSTANT, converter);
        assertEquals(Year.of(2020), y2);

        // null result when parsing fails completely
        Year y3 = StringConversions.toYear("bad-year", converter);
        assertNull(y3);
    }

    /* -------------------------------------------------------
     *  toLocale edge cases
     * ------------------------------------------------------- */
    @Test
    public void testToLocaleEmpty() {
        assertNull(StringConversions.toLocale("", converter));
    }
}
