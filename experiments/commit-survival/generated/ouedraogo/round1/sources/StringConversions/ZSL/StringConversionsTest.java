package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 4 test suite for {@link StringConversions}.
 *
 * The tests exercise every public static conversion method, covering normal,
 * boundary and error conditions.  A single {@link Converter} instance is used
 * throughout – it is created with default {@link ConverterOptions} (the class
 * is part of the library and provides sensible defaults for charset and zone).
 */
public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  Simple pass‑through conversions                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("123", StringConversions.asString(123));
        assertEquals("true", StringConversions.asString(true));
    }

    @Test
    public void testToString() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("abc", StringConversions.toString("abc", converter));
    }

    @Test
    public void testToStringBuffer() {
        assertNull(StringConversions.toStringBuffer(null, converter));
        StringBuilder sb = new StringBuilder("builder");
        assertEquals("builder", StringConversions.toStringBuffer(sb, converter).toString());
    }

    @Test
    public void testToStringBuilder() {
        assertNull(StringConversions.toStringBuilder(null, converter));
        StringBuffer sbuf = new StringBuffer("buffer");
        assertEquals("buffer", StringConversions.toStringBuilder(sbuf, converter).toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive numeric conversions                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByte_Valid() {
        assertEquals(Byte.valueOf((byte) 7), StringConversions.toByte("7", converter));
        assertEquals(Byte.valueOf(Byte.MAX_VALUE), StringConversions.toByte(String.valueOf(Byte.MAX_VALUE), converter));
        assertEquals(Byte.valueOf(Byte.MIN_VALUE), StringConversions.toByte(String.valueOf(Byte.MIN_VALUE), converter));
    }

    @Test
    public void testToByte_EmptyString() {
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte(null, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_OutOfRange() {
        // value larger than Byte.MAX_VALUE – should fall back to BigDecimal range check and fail
        StringConversions.toByte("200", converter);
    }

    @Test
    public void testToShort_Valid() {
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345", converter));
        assertEquals(Short.valueOf(Short.MAX_VALUE), StringConversions.toShort(String.valueOf(Short.MAX_VALUE), converter));
        assertEquals(Short.valueOf(Short.MIN_VALUE), StringConversions.toShort(String.valueOf(Short.MIN_VALUE), converter));
    }

    @Test
    public void testToShort_EmptyString() {
        assertEquals(Short.valueOf((short)0), StringConversions.toShort("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort_OutOfRange() {
        StringConversions.toShort("40000", converter); // exceeds Short.MAX_VALUE
    }

    @Test
    public void testToInt_Valid() {
        assertEquals(Integer.valueOf(42), StringConversions.toInt("42", converter));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), StringConversions.toInt(String.valueOf(Integer.MAX_VALUE), converter));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), StringConversions.toInt(String.valueOf(Integer.MIN_VALUE), converter));
    }

    @Test
    public void testToInt_EmptyString() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_OutOfRange() {
        // value larger than Integer.MAX_VALUE (use long literal)
        StringConversions.toInt("3000000000", converter);
    }

    @Test
    public void testToLong_Valid() {
        assertEquals(Long.valueOf(123456789L), StringConversions.toLong("123456789", converter));
        assertEquals(Long.valueOf(Long.MAX_VALUE), StringConversions.toLong(String.valueOf(Long.MAX_VALUE), converter));
        assertEquals(Long.valueOf(Long.MIN_VALUE), StringConversions.toLong(String.valueOf(Long.MIN_VALUE), converter));
    }

    @Test
    public void testToLong_EmptyString() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong_NotANumber() {
        StringConversions.toLong("not-a-number", converter);
    }

    @Test
    public void testToFloat_Valid() {
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_Invalid() {
        StringConversions.toFloat("NaNx", converter);
    }

    @Test
    public void testToDouble_Valid() {
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_Invalid() {
        StringConversions.toDouble("foo", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Atomic wrappers                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean abTrue = StringConversions.toAtomicBoolean("true", converter);
        AtomicBoolean abFalse = StringConversions.toAtomicBoolean("false", converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = StringConversions.toAtomicInteger("123", converter);
        assertEquals(123, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = StringConversions.toAtomicLong("9876543210", converter);
        assertEquals(9876543210L, al.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean conversion                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBoolean_Standard() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    @Test
    public void testToBoolean_IgnoredCaseAndAliases() {
        assertTrue(StringConversions.toBoolean("TRUE", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
    }

    @Test
    public void testToBoolean_Unrecognized() {
        assertFalse(StringConversions.toBoolean("nope", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Character conversion                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharacter_SingleChar() {
        assertEquals('x', StringConversions.toCharacter("x", converter));
    }

    @Test
    public void testToCharacter_EmptyString() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
    }

    @Test
    public void testToCharacter_DigitString() {
        // "65" -> 'A'
        assertEquals('A', StringConversions.toCharacter("65", converter));
    }

    @Test
    public void testToCharacter_UnicodeEscape() {
        // "\u0042" -> 'B'
        assertEquals('B', StringConversions.toCharacter("\\u0042", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_InvalidUnicodeEscape() {
        StringConversions.toCharacter("\\u123", converter); // wrong length
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_NonDigitNonUnicode() {
        StringConversions.toCharacter("abc", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  BigInteger / BigDecimal                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Valid() {
        assertEquals(BigInteger.valueOf(12345L), StringConversions.toBigInteger("12345", converter));
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigInteger_Invalid() {
        StringConversions.toBigInteger("12.34", converter);
    }

    @Test
    public void testToBigDecimal_Valid() {
        assertEquals(new BigDecimal("123.456"), StringConversions.toBigDecimal("123.456", converter));
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimal_Invalid() {
        StringConversions.toBigDecimal("not-a-number", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  URL / URI                                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToURL_Valid() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURL_Invalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    @Test
    public void testToURI_Valid() {
        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());
        assertEquals("user@example.com", uri.getSchemeSpecificPart());
    }

    @Test
    public void testToURI_Empty() {
        assertNull(StringConversions.toURI("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Enum conversion                                                          */
    /* ---------------------------------------------------------------------- */

    private enum SampleEnum { ONE, TWO, THREE }

    @Test
    public void testEnumToString() {
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  UUID / Duration                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToUUID_Valid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_Invalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    @Test
    public void testToDuration_Valid() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofMillis(20345), d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_Invalid() {
        StringConversions.toDuration("20 seconds", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Class conversion                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToClass_Valid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_NotFound() {
        StringConversions.toClass("com.nonexistent.FooBar", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  MonthDay / YearMonth / Period                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMonthDay_ISO() {
        MonthDay md = StringConversions.toMonthDay("2007-12-03", converter);
        assertEquals(MonthDay.of(12, 3), md);
    }

    @Test
    public void testToMonthDay_MM_DD() {
        MonthDay md = StringConversions.toMonthDay("7.4", converter);
        assertEquals(MonthDay.of(7, 4), md);
    }

    @Test
    public void testToMonthDay_FromZonedDateTime() {
        // Date string that DateUtilities can parse
        String dateStr = "2022-03-15T10:15:30Z";
        MonthDay md = StringConversions.toMonthDay(dateStr, converter);
        assertEquals(MonthDay.of(3, 15), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_Invalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    @Test
    public void testToYearMonth_ISO() {
        YearMonth ym = StringConversions.toYearMonth("2021-08", converter);
        assertEquals(YearMonth.of(2021, 8), ym);
    }

    @Test
    public void testToYearMonth_FromZonedDateTime() {
        String dateStr = "2020-11-05T00:00:00+01:00";
        YearMonth ym = StringConversions.toYearMonth(dateStr, converter);
        assertEquals(YearMonth.of(2020, 11), ym);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_Invalid() {
        StringConversions.toYearMonth("not-a-year-month", converter);
    }

    @Test
    public void testToPeriod_Valid() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriod_Invalid() {
        StringConversions.toPeriod("2 years", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Date / SqlDate / Timestamp                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDate_Valid() {
        Date date = StringConversions.toDate("2022-01-01T12:00:00Z", converter);
        assertNotNull(date);
        Instant instant = date.toInstant();
        assertEquals(Instant.parse("2022-01-01T12:00:00Z"), instant);
    }

    @Test
    public void testToDate_Null() {
        assertNull(StringConversions.toDate("", converter));
    }

    @Test
    public void testToSqlDate_Valid() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2022-02-02T00:00:00Z", converter);
        assertEquals(java.sql.Date.valueOf("2022-02-02"), sqlDate);
    }

    @Test
    public void testToTimestamp_Valid() {
        Timestamp ts = StringConversions.toTimestamp("2022-03-03T15:30:45Z", converter);
        assertEquals(Timestamp.from(Instant.parse("2022-03-03T15:30:45Z")), ts);
    }

    /* ---------------------------------------------------------------------- */
    /*  TimeZone / Calendar / Locale                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimeZone_Valid() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());
    }

    @Test
    public void testToTimeZone_Empty() {
        assertNull(StringConversions.toTimeZone("", converter));
    }

    @Test
    public void testToCalendar_Valid() {
        Calendar cal = StringConversions.toCalendar("2022-04-04T10:20:30Z", converter);
        assertNotNull(cal);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(4, cal.get(Calendar.MONTH) + 1); // MONTH is zero‑based
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testToLocale_Valid() {
        Locale locale = StringConversions.toLocale("fr-CA", converter);
        assertEquals(Locale.forLanguageTag("fr-CA"), locale);
    }

    @Test
    public void testToLocale_Empty() {
        assertNull(StringConversions.toLocale("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  LocalDate / LocalDateTime / LocalTime                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_Valid() {
        LocalDate ld = StringConversions.toLocalDate("2022-05-05T00:00:00Z", converter);
        assertEquals(LocalDate.of(2022, 5, 5), ld);
    }

    @Test
    public void testToLocalDateTime_Valid() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-06-06T13:45:30Z", converter);
        assertEquals(LocalDateTime.of(2022, 6, 6, 13, 45, 30), ldt);
    }

    @Test
    public void testToLocalTime_ISO() {
        LocalTime lt = StringConversions.toLocalTime("13:59:01", converter);
        assertEquals(LocalTime.of(13, 59, 1), lt);
    }

    @Test
    public void testToLocalTime_FromZonedDateTime() {
        LocalTime lt = StringConversions.toLocalTime("2022-07-07T23:11:12+02:00", converter);
        assertEquals(LocalTime.of(23, 11, 12), lt);
    }

    @Test
    public void testToLocalTime_Null() {
        assertNull(StringConversions.toLocalTime("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  ZoneId / ZoneOffset / OffsetDateTime / OffsetTime                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZoneId_Valid() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testToZoneId_FallbackToTimeZone() {
        // "GMT+2" is not a ZoneId but is understood by TimeZone
        ZoneId zid = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_Invalid() {
        StringConversions.toZoneId("UNKNOWN_ZONE", converter);
    }

    @Test
    public void testToZoneOffset_Valid() {
        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_Invalid() {
        StringConversions.toZoneOffset("BAD", converter);
    }

    @Test
    public void testToOffsetDateTime_Valid() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-08-08T12:34:56+03:00", converter);
        assertEquals(OffsetDateTime.parse("2022-08-08T12:34:56+03:00"), odt);
    }

    @Test
    public void testToOffsetTime_ISO() {
        OffsetTime ot = StringConversions.toOffsetTime("10:15:30+01:00", converter);
        assertEquals(OffsetTime.parse("10:15:30+01:00"), ot);
    }

    @Test
    public void testToOffsetTime_FallbackToOffsetDateTime() {
        // Provide a full datetime; method should derive the time component.
        OffsetTime ot = StringConversions.toOffsetTime("2022-09-09T05:06:07-04:00", converter);
        assertEquals(OffsetTime.of(5, 6, 7, 0, ZoneOffset.ofHours(-4)), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Invalid() {
        StringConversions.toOffsetTime("not-a-time", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Instant                                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToInstant_Valid() {
        Instant instant = StringConversions.toInstant("2022-10-10T00:00:00Z", converter);
        assertEquals(Instant.parse("2022-10-10T00:00:00Z"), instant);
    }

    @Test
    public void testToInstant_Null() {
        assertNull(StringConversions.toInstant("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Char / CharArray / CharBuffer / ByteArray / ByteBuffer                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharArray_NullOrEmpty() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray(null, converter));
    }

    @Test
    public void testToCharArray_Normal() {
        char[] result = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a', 'b', 'c'}, result);
    }

    @Test
    public void testToCharacterArray() {
        Character[] expected = new Character[]{'x', 'y', 'z'};
        assertArrayEquals(expected, StringConversions.toCharacterArray("xyz", converter));
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer buf = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", buf.toString());
    }

    @Test
    public void testToByteArray_NullOrEmpty() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToByteArray_Normal() throws Exception {
        byte[] bytes = StringConversions.toByteArray("ABC", converter);
        assertArrayEquals("ABC".getBytes(converter.getOptions().getCharset()), bytes);
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer buffer = StringConversions.toByteBuffer("XYZ", converter);
        assertArrayEquals("XYZ".getBytes(converter.getOptions().getCharset()), buffer.array());
    }

    /* ---------------------------------------------------------------------- */
    /*  Year conversion                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToYear_Valid() {
        Year y = StringConversions.toYear("2025", converter);
        assertEquals(Year.of(2025), y);
    }

    @Test
    public void testToYear_TrimAndParse() {
        Year y = StringConversions.toYear("  1999  ", converter);
        assertEquals(Year.of(1999), y);
    }

    @Test
    public void testToYear_FromZonedDateTime() {
        Year y = StringConversions.toYear("2023-12-31T23:59:59Z", converter);
        assertEquals(Year.of(2023), y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYear_Invalid() {
        StringConversions.toYear("not-a-year", converter);
    }
}
