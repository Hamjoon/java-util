package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions</li>
 *   <li>Edge cases (empty strings, nulls, boundary numeric values)</li>
 *   <li>Failure scenarios that must throw {@link IllegalArgumentException}</li>
 * </ul>
 *
 * The {@link Converter} instance is created with default {@link ConverterOptions}
 * (the real library provides a public no‑arg constructor).  All charset/zone
 * related calls rely on the defaults supplied by those options.
 */
public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Its default charset is UTF‑8 and default zone is the system zone.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Simple scalar conversions
     * ------------------------------------------------------------- */

    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("123", StringConversions.asString(123));
        assertEquals("true", StringConversions.asString(true));
    }

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte) 127), StringConversions.toByte("127", converter));
        // out‑of‑range numeric string falls back to BigDecimal handling
        assertEquals(Byte.valueOf((byte) -128), StringConversions.toByte("-128", converter));
        // non‑numeric string throws
        try {
            StringConversions.toByte("not a number", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
        assertEquals(Short.valueOf(Short.MAX_VALUE), StringConversions.toShort(String.valueOf(Short.MAX_VALUE), converter));
        // fallback via BigDecimal
        assertEquals(Short.valueOf(Short.MIN_VALUE), StringConversions.toShort(String.valueOf(Short.MIN_VALUE), converter));
        try {
            StringConversions.toShort("12.34", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
        assertEquals(Integer.valueOf(42), StringConversions.toInt("42", converter));
        // fallback via BigDecimal (value just inside Integer.MAX_VALUE)
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), StringConversions.toInt(String.valueOf(Integer.MAX_VALUE), converter));
        try {
            StringConversions.toInt("notInt", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
        assertEquals(Long.valueOf(Long.MAX_VALUE), StringConversions.toLong(String.valueOf(Long.MAX_VALUE), converter));
        // fallback via BigDecimal with decimal part (should truncate)
        assertEquals(Long.valueOf(123L), StringConversions.toLong("123.999", converter));
        try {
            StringConversions.toLong("abc", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
        try {
            StringConversions.toFloat("NaNx", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
        try {
            StringConversions.toDouble("xyz", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean("true", converter);
        assertTrue(ab.get());
        ab = StringConversions.toAtomicBoolean("false", converter);
        assertFalse(ab.get());
        ab = StringConversions.toAtomicBoolean("t", converter);
        assertTrue(ab.get());
        ab = StringConversions.toAtomicBoolean("0", converter);
        assertFalse(ab.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = StringConversions.toAtomicInteger("123", converter);
        assertEquals(123, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = StringConversions.toAtomicLong(String.valueOf(Long.MAX_VALUE), converter);
        assertEquals(Long.MAX_VALUE, al.get());
    }

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
    public void testToCharacter() {
        // empty string => '\0'
        assertEquals('\0', StringConversions.toCharacter("", converter));

        // single character
        assertEquals('Z', StringConversions.toCharacter("Z", converter));

        // numeric code point
        assertEquals('A', StringConversions.toCharacter("65", converter));

        // unicode escape
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));

        // invalid numeric string
        try {
            StringConversions.toCharacter("9999999999", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        // malformed unicode escape
        try {
            StringConversions.toCharacter("\\u123", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(BigInteger.valueOf(12345), StringConversions.toBigInteger("12345", converter));
        try {
            StringConversions.toBigInteger("12.34", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.1415"), StringConversions.toBigDecimal("3.1415", converter));
        try {
            StringConversions.toBigDecimal("notNumber", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToURL() throws Exception {
        URL url = StringConversions.toURL("https://example.com/path?query=1", converter);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());

        // empty string => null
        assertNull(StringConversions.toURL("", converter));

        // malformed URL
        try {
            StringConversions.toURL("ht!tp://bad", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToURI() {
        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());
        assertNull(StringConversions.toURI("", converter));
    }

    @Test
    public void testEnumToString() {
        assertEquals("MONDAY", StringConversions.enumToString(DayOfWeek.MONDAY, converter));
    }

    @Test
    public void testToUUID() {
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        UUID uuid = StringConversions.toUUID(uuidStr, converter);
        assertEquals(UUID.fromString(uuidStr), uuid);

        try {
            StringConversions.toUUID("not-a-uuid", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------------
     *  Temporal conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToDuration_decimalAndIso() {
        // decimal seconds
        Duration d1 = StringConversions.toDuration("123.456", converter);
        assertEquals(123, d1.getSeconds());
        assertEquals(456_000_000, d1.getNano());

        // ISO‑8601
        Duration d2 = StringConversions.toDuration("PT1H2M3.5S", converter);
        assertEquals(Duration.ofHours(1).plusMinutes(2).plusSeconds(3).plusMillis(500), d2);

        // invalid format
        try {
            StringConversions.toDuration("not-a-duration", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToClass() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);

        try {
            StringConversions.toClass("non.existent.ClassName", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToMonthDay() {
        // ISO format
        MonthDay md1 = StringConversions.toMonthDay("2000-12-25", converter);
        assertEquals(MonthDay.of(12, 25), md1);

        // MM.DD pattern
        MonthDay md2 = StringConversions.toMonthDay("3.5", converter);
        assertEquals(MonthDay.of(3, 5), md2);

        // ZonedDateTime fallback
        MonthDay md3 = StringConversions.toMonthDay("2021-07-04T12:00Z", converter);
        assertEquals(MonthDay.of(7, 4), md3);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym1 = StringConversions.toYearMonth("2022-08", converter);
        assertEquals(YearMonth.of(2022, 8), ym1);

        // ZonedDateTime fallback
        YearMonth ym2 = StringConversions.toYearMonth("2020-01-15T10:00+01:00", converter);
        assertEquals(YearMonth.of(2020, 1), ym2);
    }

    @Test
    public void testToPeriod() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);

        try {
            StringConversions.toPeriod("invalid", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToDateAndInstant() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 21, 13, 45, 0, 0, ZoneId.of("UTC"));
        Date date = StringConversions.toDate(zdt.toString(), converter);
        assertNotNull(date);
        assertEquals(Instant.parse(zdt.toString()), date.toInstant());

        // null handling
        assertNull(StringConversions.toDate(null, converter));
    }

    @Test
    public void testToSqlDate() {
        // simple yyyy‑MM‑dd
        java.sql.Date sql1 = StringConversions.toSqlDate("2021-12-31", converter);
        assertEquals(2021, sql1.toLocalDate().getYear());

        // ISO‑8601 with zone
        java.sql.Date sql2 = StringConversions.toSqlDate("2022-01-01T00:00:00Z", converter);
        assertEquals(LocalDate.of(2022, 1, 1), sql2.toLocalDate());

        // invalid string -> null
        assertNull(StringConversions.toSqlDate("not-a-date", converter));
    }

    @Test
    public void testToTimestamp() {
        // regular instant
        Timestamp ts = StringConversions.toTimestamp("2020-02-02T12:00:00Z", converter);
        assertEquals(Instant.parse("2020-02-02T12:00:00Z"), ts.toInstant());

        // year before 0001 should throw
        try {
            // 0000‑01‑01T00:00:00Z is before year 1
            StringConversions.toTimestamp("-0001-01-01T00:00:00Z", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToTimeZone() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());

        assertNull(StringConversions.toTimeZone("", converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = StringConversions.toCalendar("2022-03-15T10:20:30+02:00", converter);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(3, cal.get(Calendar.MONTH) + 1);
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));

        // year < 2 triggers exception
        try {
            StringConversions.toCalendar("0001-01-01T00:00:00Z", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToLocalDateAndLocalDateTime() {
        LocalDate ld = StringConversions.toLocalDate("2021-07-04T00:00:00Z", converter);
        assertEquals(LocalDate.of(2021, 7, 4), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime("2021-07-04T15:30:45+01:00", converter);
        assertEquals(LocalDateTime.of(2021, 7, 4, 15, 30, 45), ldt);
    }

    @Test
    public void testToLocalTime() {
        // direct parse
        LocalTime lt1 = StringConversions.toLocalTime("13:45:20", converter);
        assertEquals(LocalTime.of(13, 45, 20), lt1);

        // fallback via ZonedDateTime
        LocalTime lt2 = StringConversions.toLocalTime("2021-09-01T08:15:00-04:00", converter);
        assertEquals(LocalTime.of(8, 15), lt2);
    }

    @Test
    public void testToLocale() {
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.US, loc);
        assertNull(StringConversions.toLocale("", converter));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2022-10-05T09:30:00+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
    }

    @Test
    public void testToZoneId() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);

        // unknown ID falls back to TimeZone conversion
        ZoneId zid2 = StringConversions.toZoneId("GMT+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zid2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_unknown() {
        // "Fake/Zone" resolves to GMT which triggers the explicit exception
        StringConversions.toZoneId("Fake/Zone", converter);
    }

    @Test
    public void testToZoneOffset() {
        ZoneOffset zo = StringConversions.toZoneOffset("+05:30", converter);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_invalid() {
        StringConversions.toZoneOffset("invalid-offset", converter);
    }

    @Test
    public void testToOffsetDateTimeAndOffsetTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-11-01T12:00:00+03:00", converter);
        assertEquals(OffsetDateTime.of(2022, 11, 1, 12, 0, 0, 0, ZoneOffset.ofHours(3)), odt);

        OffsetTime ot = StringConversions.toOffsetTime("13:45:30+02:00", converter);
        assertEquals(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), ot);

        // fallback via OffsetDateTime
        OffsetTime ot2 = StringConversions.toOffsetTime("2022-12-01T10:15:00+01:00", converter);
        assertEquals(OffsetTime.of(10, 15, 0, 0, ZoneOffset.ofHours(1)), ot2);
    }

    @Test
    public void testToInstant() {
        Instant ins = StringConversions.toInstant("2023-01-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), ins);
    }

    /* -------------------------------------------------------------
     *  Array / Buffer / String families
     * ------------------------------------------------------------- */

    @Test
    public void testToCharArrayAndCharacterArray() {
        char[] ca = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[] {'a', 'b', 'c'}, ca);

        Character[] cobj = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[] {'x', 'y', 'z'}, cobj);
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", cb.toString());
    }

    @Test
    public void testToByteArrayAndByteBuffer() {
        byte[] bytes = StringConversions.toByteArray("hello", converter);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);

        ByteBuffer bb = StringConversions.toByteBuffer("world", converter);
        assertEquals(ByteBuffer.wrap("world".getBytes(StandardCharsets.UTF_8)), bb);
    }

    @Test
    public void testToStringVariants() {
        assertEquals("test", StringConversions.toString("test", converter));
        assertNull(StringConversions.toString(null, converter));

        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));
        assertNull(StringConversions.toStringBuffer(null, converter));

        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    @Test
    public void testToYear() {
        Year y = StringConversions.toYear("2020", converter);
        assertEquals(Year.of(2020), y);

        // fallback via ZonedDateTime
        Year y2 = StringConversions.toYear("2021-06-15T12:00Z", converter);
        assertEquals(Year.of(2021), y2);
    }

    @Test
    public void testToPatternAndCurrency() {
        Pattern p = StringConversions.toPattern("\\d+", converter);
        assertTrue(p.matcher("12345").matches());

        Currency cur = StringConversions.toCurrency("USD", converter);
        assertEquals(Currency.getInstance("USD"), cur);
    }
}
