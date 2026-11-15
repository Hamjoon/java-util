package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringConversions}.
 *
 * The {@code Converter} instance is created once per test class using the default
 * {@code ConverterOptions}.  All tests exercise normal behaviour, edge cases and
 * error handling for every public static method in {@code StringConversions}.
 */
public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
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
    public void testToByte_ValidAndEmpty() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte) 5), StringConversions.toByte("5", converter));
    }

    @Test
    public void testToByte_FallbackToLongWithinRange() {
        // "130" is out of byte range, but within the long fallback logic it will be rejected.
        // Use a value that is a decimal but still fits in a byte after rounding down.
        assertEquals(Byte.valueOf((byte) 127), StringConversions.toByte("127.9", converter));
    }

    @Test
    public void testToByte_OutOfRangeThrows() {
        try {
            StringConversions.toByte("200", converter);
            fail("Expected IllegalArgumentException for value outside byte range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("outside"));
        }
    }

    /* -------------------------------------------------------
     *  toShort
     * ------------------------------------------------------- */
    @Test
    public void testToShort_ValidAndEmpty() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345", converter));
    }

    @Test
    public void testToShort_OutOfRangeThrows() {
        try {
            StringConversions.toShort("40000", converter);
            fail("Expected IllegalArgumentException for short out of range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("outside"));
        }
    }

    /* -------------------------------------------------------
     *  toInt
     * ------------------------------------------------------- */
    @Test
    public void testToInt_ValidAndEmpty() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
    }

    @Test
    public void testToInt_OutOfRangeThrows() {
        try {
            StringConversions.toInt("3000000000", converter); // > Integer.MAX_VALUE
            fail("Expected IllegalArgumentException for int out of range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("outside"));
        }
    }

    /* -------------------------------------------------------
     *  toLong
     * ------------------------------------------------------- */
    @Test
    public void testToLong_ValidAndEmpty() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
    }

    @Test
    public void testToLong_OutOfRangeThrows() {
        try {
            // Long.MAX_VALUE + 1 as a decimal string
            String big = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE).toPlainString();
            StringConversions.toLong(big, converter);
            fail("Expected IllegalArgumentException for long out of range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("outside"));
        }
    }

    /* -------------------------------------------------------
     *  toFloat / toDouble
     * ------------------------------------------------------- */
    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_Invalid() {
        StringConversions.toFloat("not-a-float", converter);
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_Invalid() {
        StringConversions.toDouble("NaN??", converter);
    }

    /* -------------------------------------------------------
     *  Atomic wrappers
     * ------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        assertEquals(new AtomicBoolean(true), StringConversions.toAtomicBoolean("true", converter));
        assertEquals(new AtomicBoolean(false), StringConversions.toAtomicBoolean("false", converter));

        assertEquals(new AtomicInteger(42), StringConversions.toAtomicInteger("42", converter));
        assertEquals(new AtomicLong(123456789L), StringConversions.toAtomicLong("123456789", converter));
    }

    /* -------------------------------------------------------
     *  toBoolean
     * ------------------------------------------------------- */
    @Test
    public void testToBoolean() {
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

    /* -------------------------------------------------------
     *  toCharacter
     * ------------------------------------------------------- */
    @Test
    public void testToCharacter_EmptyAndSingleChar() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
        assertEquals('Z', StringConversions.toCharacter("Z", converter));
    }

    @Test
    public void testToCharacter_NumericString() {
        // 65 -> 'A'
        assertEquals('A', StringConversions.toCharacter("65", converter));
        // With spaces
        assertEquals('B', StringConversions.toCharacter(" 66 ", converter));
    }

    @Test
    public void testToCharacter_UnicodeEscape() {
        assertEquals('A', StringConversions.toCharacter("\\u0041", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_InvalidUnicodeEscape() {
        StringConversions.toCharacter("\\uZZZZ", converter);
    }

    /* -------------------------------------------------------
     *  toBigInteger / toBigDecimal
     * ------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigInteger_Invalid() {
        StringConversions.toBigInteger("not-an-integer", converter);
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.1415926535"), StringConversions.toBigDecimal("3.1415926535", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimal_Invalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    /* -------------------------------------------------------
     *  URL / URI
     * ------------------------------------------------------- */
    @Test
    public void testToURL() throws Exception {
        assertNull(StringConversions.toURL("", converter));
        URL url = StringConversions.toURL("http://example.com/path?query=1", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURL_Invalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    @Test
    public void testToURI() {
        assertNull(StringConversions.toURI("", converter));
        URI uri = StringConversions.toURI("urn:test:12345", converter);
        assertEquals("urn:test:12345", uri.toString());
    }

    /* -------------------------------------------------------
     *  enumToString
     * ------------------------------------------------------- */
    private enum Sample { ONE, TWO }

    @Test
    public void testEnumToString() {
        assertEquals("ONE", StringConversions.enumToString(Sample.ONE, converter));
        assertEquals("TWO", StringConversions.enumToString(Sample.TWO, converter));
    }

    /* -------------------------------------------------------
     *  toUUID
     * ------------------------------------------------------- */
    @Test
    public void testToUUID_Valid() {
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        UUID uuid = StringConversions.toUUID(uuidStr, converter);
        assertEquals(UUID.fromString(uuidStr), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_Invalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    /* -------------------------------------------------------
     *  toDuration
     * ------------------------------------------------------- */
    @Test
    public void testToDuration_DecimalSeconds() {
        Duration d = StringConversions.toDuration("123.456", converter);
        assertEquals(123, d.getSeconds());
        assertEquals(456_000_000, d.getNano());
    }

    @Test
    public void testToDuration_ISO8601() {
        Duration d = StringConversions.toDuration("PT1H2M3.004S", converter);
        assertEquals(3723, d.getSeconds());
        assertEquals(4_000_000, d.getNano());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_Invalid() {
        StringConversions.toDuration("not-a-duration", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_NonString() {
        StringConversions.toDuration(12345, converter);
    }

    /* -------------------------------------------------------
     *  toClass
     * ------------------------------------------------------- */
    @Test
    public void testToClass_Valid() {
        Class<?> c = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_Invalid() {
        StringConversions.toClass("non.existent.ClassName", converter);
    }

    /* -------------------------------------------------------
     *  MonthDay / YearMonth / Period
     * ------------------------------------------------------- */
    @Test
    public void testToMonthDay_ISO() {
        MonthDay md = StringConversions.toMonthDay("--12-25", converter);
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToMonthDay_MMdPattern() {
        MonthDay md = StringConversions.toMonthDay("3.14", converter);
        assertEquals(MonthDay.of(3, 14), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_Invalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    @Test
    public void testToYearMonth_ISO() {
        YearMonth ym = StringConversions.toYearMonth("2023-07", converter);
        assertEquals(YearMonth.of(2023, 7), ym);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_Invalid() {
        StringConversions.toYearMonth("bad-year-month", converter);
    }

    @Test
    public void testToPeriod_Valid() {
        Period p = StringConversions.toPeriod("P2Y3M4D", converter);
        assertEquals(Period.of(2, 3, 4), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriod_Invalid() {
        StringConversions.toPeriod("not-a-period", converter);
    }

    /* -------------------------------------------------------
     *  toDate / toSqlDate / toTimestamp
     * ------------------------------------------------------- */
    @Test
    public void testToDate() {
        // 2020‑01‑02T03:04:05Z => epoch seconds 1577929445
        ZonedDateTime zdt = ZonedDateTime.parse("2020-01-02T03:04:05Z");
        Date d = StringConversions.toDate("2020-01-02T03:04:05Z", converter);
        assertEquals(Date.from(zdt.toInstant()), d);
    }

    @Test
    public void testToSqlDate_Simple() {
        java.sql.Date sql = StringConversions.toSqlDate("2021-12-31", converter);
        assertEquals(java.sql.Date.valueOf("2021-12-31"), sql);
    }

    @Test
    public void testToSqlDate_ISO8601() {
        java.sql.Date sql = StringConversions.toSqlDate("2021-12-31T15:30:45Z", converter);
        // The date part should be 2021‑12‑31 regardless of time.
        assertEquals(java.sql.Date.valueOf("2021-12-31"), sql);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToSqlDate_Invalid() {
        StringConversions.toSqlDate("not-a-date", converter);
    }

    @Test
    public void testToTimestamp_Valid() {
        Timestamp ts = StringConversions.toTimestamp("1970-01-01T00:00:01Z", converter);
        assertEquals(1000L, ts.getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToTimestamp_BeforeYearOne() {
        // 0000‑01‑01T00:00:00Z is before year 1
        StringConversions.toTimestamp("0000-01-01T00:00:00Z", converter);
    }

    /* -------------------------------------------------------
     *  TimeZone / Calendar
     * ------------------------------------------------------- */
    @Test
    public void testToTimeZone() {
        assertNull(StringConversions.toTimeZone("", converter));
        assertEquals(TimeZone.getTimeZone("UTC"), StringConversions.toTimeZone("UTC", converter));
    }

    @Test
    public void testToCalendar_Valid() {
        Calendar cal = StringConversions.toCalendar("2022-03-04T12:00:00Z", converter);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(2, cal.get(Calendar.MONTH)); // March (0‑based)
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCalendar_YearTooSmall() {
        // Year 1 is not representable per implementation guard.
        StringConversions.toCalendar("0001-01-01T00:00:00Z", converter);
    }

    /* -------------------------------------------------------
     *  LocalDate / LocalDateTime / LocalTime
     * ------------------------------------------------------- */
    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2022-08-15T00:00:00Z", converter);
        assertEquals(LocalDate.of(2022, 8, 15), ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-08-15T13:45:30+02:00", converter);
        assertEquals(LocalDateTime.of(2022, 8, 15, 13, 45, 30), ldt);
    }

    @Test
    public void testToLocalTime_Direct() {
        LocalTime lt = StringConversions.toLocalTime("09:30:15", converter);
        assertEquals(LocalTime.of(9, 30, 15), lt);
    }

    @Test
    public void testToLocalTime_FromZoned() {
        LocalTime lt = StringConversions.toLocalTime("2022-01-01T22:15:00Z", converter);
        assertEquals(LocalTime.of(22, 15, 0), lt);
    }

    /* -------------------------------------------------------
     *  Locale
     * ------------------------------------------------------- */
    @Test
    public void testToLocale() {
        assertNull(StringConversions.toLocale("", converter));
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), loc);
    }

    /* -------------------------------------------------------
     *  ZonedDateTime / Instant
     * ------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2022-11-05T12:34:56+01:00", converter);
        assertEquals(ZoneId.of("+01:00"), zdt.getZone());
        assertEquals(2022, zdt.getYear());
    }

    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2022-11-05T12:34:56Z", converter);
        assertEquals(Instant.parse("2022-11-05T12:34:56Z"), instant);
    }

    /* -------------------------------------------------------
     *  ZoneId / ZoneOffset
     * ------------------------------------------------------- */
    @Test
    public void testToZoneId_Valid() {
        assertEquals(ZoneId.of("Europe/Paris"), StringConversions.toZoneId("Europe/Paris", converter));
    }

    @Test
    public void testToZoneId_FallbackToTimeZone() {
        // "GMT+2" is understood by TimeZone but not by ZoneId.of()
        ZoneId zid = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_Invalid() {
        StringConversions.toZoneId("nonexistent/zone", converter);
    }

    @Test
    public void testToZoneOffset_Valid() {
        assertEquals(ZoneOffset.of("-05:00"), StringConversions.toZoneOffset("-05:00", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_Invalid() {
        StringConversions.toZoneOffset("bad-offset", converter);
    }

    /* -------------------------------------------------------
     *  OffsetDateTime / OffsetTime
     * ------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-12-01T10:20:30+03:00", converter);
        assertEquals(OffsetDateTime.parse("2022-12-01T10:20:30+03:00"), odt);
    }

    @Test
    public void testToOffsetTime_Direct() {
        OffsetTime ot = StringConversions.toOffsetTime("13:45:30+02:00", converter);
        assertEquals(OffsetTime.parse("13:45:30+02:00"), ot);
    }

    @Test
    public void testToOffsetTime_FallbackViaOffsetDateTime() {
        // Provide a full datetime; the method should fall back to extracting the time.
        OffsetTime ot = StringConversions.toOffsetTime("2022-01-01T08:15:00+01:00", converter);
        assertEquals(OffsetTime.of(8, 15, 0, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Invalid() {
        StringConversions.toOffsetTime("not-a-time", converter);
    }

    /* -------------------------------------------------------
     *  Char / CharArray / CharBuffer
     * ------------------------------------------------------- */
    @Test
    public void testToCharArray() {
        char[] expected = new char[] { 'a', 'b', 'c' };
        assertArrayEquals(expected, StringConversions.toCharArray("abc", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
    }

    @Test
    public void testToCharacterArray() {
        Character[] expected = new Character[] { 'x', 'y', 'z' };
        assertArrayEquals(expected, StringConversions.toCharacterArray("xyz", converter));
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer buffer = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", buffer.toString());
    }

    /* -------------------------------------------------------
     *  ByteArray / ByteBuffer
     * ------------------------------------------------------- */
    @Test
    public void testToByteArray() throws Exception {
        byte[] bytes = StringConversions.toByteArray("ABC", converter);
        assertArrayEquals("ABC".getBytes(converter.getOptions().getCharset()), bytes);
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer buffer = StringConversions.toByteBuffer("xyz", converter);
        assertEquals(ByteBuffer.wrap("xyz".getBytes(converter.getOptions().getCharset())), buffer);
    }

    /* -------------------------------------------------------
     *  toString / StringBuffer / StringBuilder
     * ------------------------------------------------------- */
    @Test
    public void testToStringVariants() {
        assertEquals("test", StringConversions.toString("test", converter));
        assertNull(StringConversions.toString(null, converter));

        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));
        assertNull(StringConversions.toStringBuffer(null, converter));

        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    /* -------------------------------------------------------
     *  toYear / toPattern / toCurrency
     * ------------------------------------------------------- */
    @Test
    public void testToYear_ValidString() {
        assertEquals(Year.of(2020), StringConversions.toYear("2020", converter));
        // whitespace should be trimmed
        assertEquals(Year.of(1999), StringConversions.toYear(" 1999 ", converter));
    }

    @Test
    public void testToYear_FallbackToZonedDateTime() {
        Year y = StringConversions.toYear("2021-06-15T00:00:00Z", converter);
        assertEquals(Year.of(2021), y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYear_Invalid() {
        StringConversions.toYear("bad-year", converter);
    }

    @Test
    public void testToPattern() {
        Pattern p = StringConversions.toPattern("  \\d+  ", converter);
        assertTrue(p.matcher("12345").matches());
    }

    @Test
    public void testToCurrency() {
        Currency usd = StringConversions.toCurrency("USD", converter);
        assertEquals(Currency.getInstance("USD"), usd);
    }
}
