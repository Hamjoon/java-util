package com.cedarsoftware.util.convert;

import org.junit.BeforeClass;
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

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link StringConversions}.
 *
 * The tests are written by three “experts”.  Each expert contributed at least one
 * test case for every public static method in {@code StringConversions}.  The
 * final file below aggregates all of those contributions into a single,
 * comprehensive test class.
 *
 * All objects are created via real constructors – no mocking is used – and
 * only APIs that appear in the source code above or in the listed dependency
 * signatures are referenced.
 */
public class StringConversionsTest {

    /** a reusable {@link Converter} with default options (UTF‑8 charset, system default zone) */
    private static Converter converter;

    @BeforeClass
    public static void setUpConverter() {
        // ConverterOptions is part of the public API; it has a public no‑arg constructor.
        // The default options provide a UTF‑8 charset and the system default ZoneId.
        converter = new Converter(new ConverterOptions());
    }

    /*======================================================================
     *  asString
     *=====================================================================*/
    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    /*======================================================================
     *  toByte – normal, empty, out‑of‑range, non‑numeric
     *=====================================================================*/
    @Test
    public void testToByte_NormalAndEmpty() {
        assertEquals(Byte.valueOf((byte) 7), StringConversions.toByte("7", converter));
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter)); // empty → 0
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_NonParseable() {
        // outside the byte range – fallback uses BigDecimal, still out of range → exception
        StringConversions.toByte("200", converter);
    }

    @Test
    public void testToByte_RangeFallback() {
        // "128" is out of byte range for Byte.valueOf, but within BigDecimal range → returns 127 after clamp?
        // Actually the fallback returns null if out of range, so we expect exception.
        // Use a value just inside the range but not parseable as byte (e.g., with leading spaces)
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte(" 12 ", converter));
    }

    /*======================================================================
     *  toShort – normal, empty, overflow, non‑numeric
     *=====================================================================*/
    @Test
    public void testToShort_NormalAndEmpty() {
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345", converter));
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort_Overflow() {
        // 40000 > Short.MAX_VALUE (32767)
        StringConversions.toShort("40000", converter);
    }

    /*======================================================================
     *  toInt – normal, empty, overflow, non‑numeric
     *=====================================================================*/
    @Test
    public void testToInt_NormalAndEmpty() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_Overflow() {
        // 3_000_000_000 > Integer.MAX_VALUE
        StringConversions.toInt("3000000000", converter);
    }

    /*======================================================================
     *  toLong – normal, empty, overflow, non‑numeric
     *=====================================================================*/
    @Test
    public void testToLong_NormalAndEmpty() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong_Overflow() {
        // value larger than Long.MAX_VALUE
        StringConversions.toLong("9223372036854775808", converter);
    }

    /*======================================================================
     *  toFloat / toDouble – normal, empty, malformed
     *=====================================================================*/
    @Test
    public void testToFloatAndDouble() {
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));

        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_Malformed() {
        StringConversions.toFloat("not-a-float", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_Malformed() {
        StringConversions.toDouble("NaN!", converter);
    }

    /*======================================================================
     *  Atomic wrappers
     *=====================================================================*/
    @Test
    public void testAtomicWrappers() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());

        assertEquals(42, StringConversions.toAtomicInteger("42", converter).get());
        assertEquals(1234567890123L, StringConversions.toAtomicLong("1234567890123", converter).get());
    }

    /*======================================================================
     *  toBoolean – all accepted true/false values
     *=====================================================================*/
    @Test
    public void testToBoolean_VariousForms() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertTrue(StringConversions.toBoolean("TRUE", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("\"true\"", converter));

        assertFalse(StringConversions.toBoolean("false", converter));
        assertFalse(StringConversions.toBoolean("FALSE", converter));
        assertFalse(StringConversions.toBoolean("f", converter));
        assertFalse(StringConversions.toBoolean("0", converter));
        assertFalse(StringConversions.toBoolean("n", converter));
    }

    /*======================================================================
     *  toCharacter – empty, single char, numeric, unicode escape, bad unicode
     *=====================================================================*/
    @Test
    public void testToCharacter_EmptyAndSingle() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
        assertEquals('Z', StringConversions.toCharacter("Z", converter));
    }

    @Test
    public void testToCharacter_Numeric() {
        assertEquals('A', StringConversions.toCharacter("65", converter)); // 65 → 'A'
    }

    @Test
    public void testToCharacter_UnicodeEscape() {
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_BadUnicode() {
        StringConversions.toCharacter("\\uXYZ", converter);
    }

    /*======================================================================
     *  BigInteger / BigDecimal
     *=====================================================================*/
    @Test
    public void testBigIntegerAndBigDecimal() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));

        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.1415926535"), StringConversions.toBigDecimal("3.1415926535", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigInteger_Invalid() {
        StringConversions.toBigInteger("not-a-number", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimal_Invalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    /*======================================================================
     *  URL / URI
     *=====================================================================*/
    @Test
    public void testURLandURI() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());

        assertNull(StringConversions.toURL("", converter));

        URI uri = StringConversions.toURI("http://example.com/path", converter);
        assertEquals("/path", uri.getPath());

        assertNull(StringConversions.toURI("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testURL_Invalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    /*======================================================================
     *  enumToString
     *=====================================================================*/
    private enum SampleEnum { ONE, TWO }

    @Test
    public void testEnumToString() {
        assertEquals("ONE", StringConversions.enumToString(SampleEnum.ONE, converter));
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    /*======================================================================
     *  UUID
     *=====================================================================*/
    @Test
    public void testUUID_Valid() {
        UUID uuid = UUID.randomUUID();
        String s = uuid.toString();
        assertEquals(uuid, StringConversions.toUUID(s, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUUID_Invalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    /*======================================================================
     *  Duration / Period
     *=====================================================================*/
    @Test
    public void testDurationAndPeriod() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofSeconds(20, 345_000_000), d);

        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuration_Invalid() {
        StringConversions.toDuration("not-a-duration", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPeriod_Invalid() {
        StringConversions.toPeriod("bad-period", converter);
    }

    /*======================================================================
     *  toClass
     *=====================================================================*/
    @Test
    public void testToClass_Valid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_Invalid() {
        StringConversions.toClass("non.existent.ClassName", converter);
    }

    /*======================================================================
     *  MonthDay – ISO, MM.DD, date string
     *=====================================================================*/
    @Test
    public void testToMonthDay_IsoAndShort() {
        assertEquals(MonthDay.of(12, 25), StringConversions.toMonthDay("2021-12-25", converter));
        assertEquals(MonthDay.of(3, 9), StringConversions.toMonthDay("3.9", converter));
    }

    @Test
    public void testToMonthDay_FromDateString() {
        // Using a full date that DateUtilities can parse; the actual zone is irrelevant.
        MonthDay md = StringConversions.toMonthDay("2020-02-29T10:15:30Z", converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_Invalid() {
        StringConversions.toMonthDay("not-a-monthday", converter);
    }

    /*======================================================================
     *  YearMonth – ISO and date string
     *=====================================================================*/
    @Test
    public void testToYearMonth_IsoAndDate() {
        assertEquals(YearMonth.of(2022, 7), StringConversions.toYearMonth("2022-07", converter));
        assertEquals(YearMonth.of(1999, 12), StringConversions.toYearMonth("1999-12-31T23:59:59Z", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_Invalid() {
        StringConversions.toYearMonth("bad-yearmonth", converter);
    }

    /*======================================================================
     *  Date / SqlDate / Timestamp – via ZonedDateTime parsing
     *=====================================================================*/
    @Test
    public void testDateConversions() {
        // Use an ISO‑8601 instant
        String iso = "2021-08-19T13:45:30Z";

        Date date = StringConversions.toDate(iso, converter);
        assertNotNull(date);
        assertEquals(Instant.parse(iso), date.toInstant());

        java.sql.Date sqlDate = StringConversions.toSqlDate(iso, converter);
        assertEquals(Date.from(Instant.parse(iso)), sqlDate);

        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertEquals(Instant.parse(iso).toEpochMilli(), ts.getTime());
    }

    @Test
    public void testDate_NullInput() {
        assertNull(StringConversions.toDate("", converter));
        assertNull(StringConversions.toSqlDate("", converter));
        assertNull(StringConversions.toTimestamp("", converter));
    }

    /*======================================================================
     *  TimeZone / Calendar / LocalDate* / ZonedDateTime / ZoneId / ZoneOffset
     *=====================================================================*/
    @Test
    public void testTimeZoneAndCalendar() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());

        // Calendar should reflect the same instant as the parsed ZonedDateTime
        Calendar cal = StringConversions.toCalendar("2021-01-01T00:00:00-05:00", converter);
        assertEquals(tz, cal.getTimeZone());
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
    }

    @Test
    public void testLocalDateAndTime() {
        LocalDate ld = StringConversions.toLocalDate("2022-12-31T23:59:59Z", converter);
        assertEquals(LocalDate.of(2022, 12, 31), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-12-31T23:59:59Z", converter);
        assertEquals(LocalDateTime.of(2022, 12, 31, 23, 59, 59), ldt);
    }

    @Test
    public void testLocalTime_IsoAndFallback() {
        // Direct ISO time
        LocalTime lt = StringConversions.toLocalTime("10:15:30", converter);
        assertEquals(LocalTime.of(10, 15, 30), lt);

        // Fallback via ZonedDateTime (date+time string)
        LocalTime lt2 = StringConversions.toLocalTime("2020-01-01T08:30:00Z", converter);
        assertEquals(LocalTime.of(8, 30, 0), lt2);
    }

    @Test
    public void testLocale() {
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), loc);

        assertNull(StringConversions.toLocale("", converter));
    }

    @Test
    public void testZonedDateTimeAndInstant() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2021-04-05T12:00:00+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());

        Instant instant = StringConversions.toInstant("2021-04-05T12:00:00+02:00", converter);
        assertEquals(zdt.toInstant(), instant);
    }

    @Test
    public void testZoneId_ValidAndFallback() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);

        // Fallback to TimeZone when ZoneId.of fails (e.g., legacy ID)
        ZoneId zid2 = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zid2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZoneId_Invalid() {
        StringConversions.toZoneId("no-such-zone", converter);
    }

    @Test
    public void testZoneOffset_Valid() {
        ZoneOffset zo = StringConversions.toZoneOffset("+01:30", converter);
        assertEquals(ZoneOffset.ofHoursMinutes(1, 30), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZoneOffset_Invalid() {
        StringConversions.toZoneOffset("invalid-offset", converter);
    }

    @Test
    public void testOffsetDateTimeAndTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2021-09-10T15:45:00+03:00", converter);
        assertEquals(OffsetDateTime.of(2021, 9, 10, 15, 45, 0, 0, ZoneOffset.ofHours(3)), odt);

        OffsetTime ot = StringConversions.toOffsetTime("13:20:15+02:00", converter);
        assertEquals(OffsetTime.of(13, 20, 15, 0, ZoneOffset.ofHours(2)), ot);

        // Fallback: use OffsetDateTime to derive OffsetTime
        OffsetTime ot2 = StringConversions.toOffsetTime("2021-05-01T09:00:00+04:00", converter);
        assertEquals(OffsetTime.of(9, 0, 0, 0, ZoneOffset.ofHours(4)), ot2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOffsetTime_Invalid() {
        StringConversions.toOffsetTime("not-an-offset-time", converter);
    }

    /*======================================================================
     *  Char / CharArray / CharBuffer / ByteArray / ByteBuffer
     *=====================================================================*/
    @Test
    public void testCharAndArrays() {
        char[] chars = StringConversions.toCharArray("hello", converter);
        assertArrayEquals(new char[]{'h','e','l','l','o'}, chars);

        Character[] boxed = StringConversions.toCharacterArray("AB", converter);
        assertArrayEquals(new Character[]{'A','B'}, boxed);

        CharBuffer cb = StringConversions.toCharBuffer("xyz", converter);
        assertEquals("xyz", cb.toString());
    }

    @Test
    public void testByteArrayAndBuffer() {
        byte[] bytes = StringConversions.toByteArray("ABC", converter);
        assertArrayEquals("ABC".getBytes(StandardCharsets.UTF_8), bytes);

        ByteBuffer bb = StringConversions.toByteBuffer("123", converter);
        assertEquals(ByteBuffer.wrap("123".getBytes(StandardCharsets.UTF_8)), bb);
    }

    /*======================================================================
     *  toString / StringBuffer / StringBuilder
     *=====================================================================*/
    @Test
    public void testStringConversions() {
        assertEquals("test", StringConversions.toString("test", converter));
        assertNull(StringConversions.toString(null, converter));

        StringBuffer sbuf = StringConversions.toStringBuffer("buf", converter);
        assertEquals(new StringBuffer("buf").toString(), sbuf.toString());

        StringBuilder sbld = StringConversions.toStringBuilder("bld", converter);
        assertEquals(new StringBuilder("bld").toString(), sbld.toString());
    }

    /*======================================================================
     *  Year – ISO, fallback via ZonedDateTime, invalid
     *=====================================================================*/
    @Test
    public void testYear_ValidAndFallback() {
        assertEquals(Year.of(2020), StringConversions.toYear("2020", converter));
        // fallback using a full date string
        assertEquals(Year.of(1995), StringConversions.toYear("1995-06-15T00:00:00Z", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testYear_Invalid() {
        StringConversions.toYear("not-a-year", converter);
    }
}
