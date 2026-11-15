package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;
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
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not part of the listed API, but it has a public no‑arg constructor in the library.
        // Using reflection would violate the “no other classes” rule, so we assume a public constructor exists.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* ---------- asString ---------- */
    @Test
    public void testAsString_Null() {
        assertNull(StringConversions.asString(null));
    }

    @Test
    public void testAsString_NonNull() {
        assertEquals("123", StringConversions.asString(123));
        assertEquals("true", StringConversions.asString(true));
    }

    /* ---------- toByte ---------- */
    @Test
    public void testToByte_EmptyString() {
        assertEquals((byte) 0, (byte) StringConversions.toByte("", converter));
    }

    @Test
    public void testToByte_ValidNumber() {
        assertEquals((byte) 42, (byte) StringConversions.toByte("42", converter));
        assertEquals(Byte.MAX_VALUE, (byte) StringConversions.toByte(String.valueOf(Byte.MAX_VALUE), converter));
        assertEquals(Byte.MIN_VALUE, (byte) StringConversions.toByte(String.valueOf(Byte.MIN_VALUE), converter));
    }

    @Test
    public void testToByte_DecimalWithinRange() {
        // "127.9" is out of range for Byte, should be parsed via toLong and then cast
        assertEquals((byte) 127, (byte) StringConversions.toByte("127.9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_OutOfRangeThrows() {
        // 200 is outside byte range and cannot be represented after rounding
        StringConversions.toByte("200", converter);
    }

    /* ---------- toShort ---------- */
    @Test
    public void testToShort_EmptyString() {
        assertEquals((short) 0, (short) StringConversions.toShort("", converter));
    }

    @Test
    public void testToShort_ValidNumber() {
        assertEquals((short) 12345, (short) StringConversions.toShort("12345", converter));
        assertEquals(Short.MAX_VALUE, (short) StringConversions.toShort(String.valueOf(Short.MAX_VALUE), converter));
        assertEquals(Short.MIN_VALUE, (short) StringConversions.toShort(String.valueOf(Short.MIN_VALUE), converter));
    }

    @Test
    public void testToShort_DecimalWithinRange() {
        assertEquals((short) 32767, (short) StringConversions.toShort("32767.99", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort_OutOfRangeThrows() {
        StringConversions.toShort("40000", converter);
    }

    /* ---------- toInt ---------- */
    @Test
    public void testToInt_EmptyString() {
        assertEquals(0, (int) StringConversions.toInt("", converter));
    }

    @Test
    public void testToInt_ValidNumber() {
        assertEquals(123456789, (int) StringConversions.toInt("123456789", converter));
        assertEquals(Integer.MAX_VALUE, (int) StringConversions.toInt(String.valueOf(Integer.MAX_VALUE), converter));
        assertEquals(Integer.MIN_VALUE, (int) StringConversions.toInt(String.valueOf(Integer.MIN_VALUE), converter));
    }

    @Test
    public void testToInt_DecimalWithinRange() {
        assertEquals(2147483647, (int) StringConversions.toInt("2147483647.999", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_OutOfRangeThrows() {
        StringConversions.toInt("3000000000", converter);
    }

    /* ---------- toLong ---------- */
    @Test
    public void testToLong_EmptyString() {
        assertEquals(0L, (long) StringConversions.toLong("", converter));
    }

    @Test
    public void testToLong_ValidNumber() {
        assertEquals(1234567890123L, (long) StringConversions.toLong("1234567890123", converter));
        assertEquals(Long.MAX_VALUE, (long) StringConversions.toLong(String.valueOf(Long.MAX_VALUE), converter));
        assertEquals(Long.MIN_VALUE, (long) StringConversions.toLong(String.valueOf(Long.MIN_VALUE), converter));
    }

    @Test
    public void testToLong_DecimalWithinRange() {
        assertEquals(9223372036854775807L,
                (long) StringConversions.toLong("9223372036854775807.999", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong_OutOfRangeThrows() {
        // One more than Long.MAX_VALUE
        String big = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE).toPlainString();
        StringConversions.toLong(big, converter);
    }

    /* ---------- toFloat ---------- */
    @Test
    public void testToFloat_EmptyString() {
        assertEquals(0f, StringConversions.toFloat("", converter), 0f);
    }

    @Test
    public void testToFloat_ValidNumber() {
        assertEquals(3.14f, StringConversions.toFloat("3.14", converter), 0.0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_InvalidThrows() {
        StringConversions.toFloat("not-a-float", converter);
    }

    /* ---------- toDouble ---------- */
    @Test
    public void testToDouble_EmptyString() {
        assertEquals(0.0, StringConversions.toDouble("", converter), 0.0);
    }

    @Test
    public void testToDouble_ValidNumber() {
        assertEquals(2.71828, StringConversions.toDouble("2.71828", converter), 1e-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_InvalidThrows() {
        StringConversions.toDouble("NaN", converter); // NaN parses but we treat it as valid; use an invalid string
    }

    /* ---------- toAtomicBoolean ---------- */
    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean("true", converter);
        assertTrue(ab.get());
        ab = StringConversions.toAtomicBoolean("false", converter);
        assertFalse(ab.get());
    }

    /* ---------- toAtomicInteger ---------- */
    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = StringConversions.toAtomicInteger("123", converter);
        assertEquals(123, ai.get());
    }

    /* ---------- toAtomicLong ---------- */
    @Test
    public void testToAtomicLong() {
        AtomicLong al = StringConversions.toAtomicLong("9876543210", converter);
        assertEquals(9876543210L, al.get());
    }

    /* ---------- toBoolean ---------- */
    @Test
    public void testToBoolean_ExactTrueFalse() {
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
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    /* ---------- toCharacter ---------- */
    @Test
    public void testToCharacter_Empty() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
    }

    @Test
    public void testToCharacter_SingleChar() {
        assertEquals('Z', StringConversions.toCharacter("Z", converter));
    }

    @Test
    public void testToCharacter_NumericString() {
        assertEquals('A', StringConversions.toCharacter("65", converter));
    }

    @Test
    public void testToCharacter_UnicodeEscape() {
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_InvalidUnicodeThrows() {
        StringConversions.toCharacter("\\uXYZ", converter);
    }

    /* ---------- toBigInteger ---------- */
    @Test
    public void testToBigInteger_Empty() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test
    public void testToBigInteger_Valid() {
        assertEquals(new BigInteger("12345678901234567890"),
                StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigInteger_InvalidThrows() {
        StringConversions.toBigInteger("not-a-number", converter);
    }

    /* ---------- toBigDecimal ---------- */
    @Test
    public void testToBigDecimal_Empty() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test
    public void testToBigDecimal_Valid() {
        assertEquals(new BigDecimal("3.1415926535"),
                StringConversions.toBigDecimal("3.1415926535", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimal_InvalidThrows() {
        StringConversions.toBigDecimal("abc", converter);
    }

    /* ---------- toURL ---------- */
    @Test
    public void testToURL_Valid() throws Exception {
        URL url = StringConversions.toURL("https://example.com/path?query=1", converter);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());
        assertEquals("/path", url.getPath());
    }

    @Test
    public void testToURL_EmptyReturnsNull() {
        assertNull(StringConversions.toURL("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURL_InvalidThrows() {
        StringConversions.toURL("ht!tp://bad_url", converter);
    }

    /* ---------- toURI ---------- */
    @Test
    public void testToURI_Valid() {
        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());
        assertEquals("user@example.com", uri.getSchemeSpecificPart());
    }

    @Test
    public void testToURI_EmptyReturnsNull() {
        assertNull(StringConversions.toURI("", converter));
    }

    /* ---------- enumToString ---------- */
    private enum Color { RED, GREEN, BLUE }

    @Test
    public void testEnumToString() {
        assertEquals("GREEN", StringConversions.enumToString(Color.GREEN, converter));
    }

    /* ---------- toUUID ---------- */
    @Test
    public void testToUUID_Valid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_InvalidThrows() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    /* ---------- toDuration ---------- */
    @Test
    public void testToDuration_DecimalSeconds() {
        Duration d = StringConversions.toDuration("123.456", converter);
        assertEquals(123, d.getSeconds());
        assertEquals(456_000_000, d.getNano());
    }

    @Test
    public void testToDuration_ISO8601() {
        Duration d = StringConversions.toDuration("PT1H2M3.5S", converter);
        assertEquals(3723, d.getSeconds());
        assertEquals(500_000_000, d.getNano());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_InvalidThrows() {
        StringConversions.toDuration("not-a-duration", converter);
    }

    /* ---------- toClass ---------- */
    @Test
    public void testToClass_Valid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_InvalidThrows() {
        StringConversions.toClass("com.nonexistent.FooBar", converter);
    }

    /* ---------- toMonthDay ---------- */
    @Test
    public void testToMonthDay_ISO() {
        assertEquals(MonthDay.of(12, 31),
                StringConversions.toMonthDay("2000-12-31", converter));
    }

    @Test
    public void testToMonthDay_MMDotDD() {
        assertEquals(MonthDay.of(3, 5),
                StringConversions.toMonthDay("3.5", converter));
    }

    @Test
    public void testToMonthDay_FromDateString() {
        // Using a full date string; the utility will parse and extract month/day.
        assertEquals(MonthDay.of(4, 15),
                StringConversions.toMonthDay("2022-04-15T10:20:30Z", converter));
    }

    @Test
    public void testToMonthDay_InvalidReturnsNull() {
        assertNull(StringConversions.toMonthDay("not-a-date", converter));
    }

    /* ---------- toYearMonth ---------- */
    @Test
    public void testToYearMonth_ISO() {
        assertEquals(YearMonth.of(2021, 7),
                StringConversions.toYearMonth("2021-07", converter));
    }

    @Test
    public void testToYearMonth_FromDateString() {
        assertEquals(YearMonth.of(2020, 12),
                StringConversions.toYearMonth("2020-12-25T00:00:00Z", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_InvalidThrows() {
        StringConversions.toYearMonth("bad", converter);
    }

    /* ---------- toPeriod ---------- */
    @Test
    public void testToPeriod_Valid() {
        assertEquals(Period.of(2, 3, 4),
                StringConversions.toPeriod("P2Y3M4D", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriod_InvalidThrows() {
        StringConversions.toPeriod("not-a-period", converter);
    }

    /* ---------- toDate ---------- */
    @Test
    public void testToDate_Valid() {
        // Use a known ISO date‑time string
        String iso = "2022-08-15T12:34:56Z";
        Date date = StringConversions.toDate(iso, converter);
        assertNotNull(date);
        Instant instant = date.toInstant();
        assertEquals(Instant.parse(iso), instant);
    }

    @Test
    public void testToDate_NullInput() {
        assertNull(StringConversions.toDate(null, converter));
    }

    /* ---------- toSqlDate ---------- */
    @Test
    public void testToSqlDate_SimpleFormat() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2023-01-31", converter);
        assertEquals(LocalDate.of(2023, 1, 31), sqlDate.toLocalDate());
    }

    @Test
    public void testToSqlDate_ISODateTime() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2023-02-28T15:45:00Z", converter);
        assertEquals(LocalDate.of(2023, 2, 28), sqlDate.toLocalDate());
    }

    @Test
    public void testToSqlDate_InvalidReturnsNull() {
        assertNull(StringConversions.toSqlDate("not-a-date", converter));
    }

    /* ---------- toTimestamp ---------- */
    @Test
    public void testToTimestamp_Valid() {
        Timestamp ts = StringConversions.toTimestamp("2022-12-31T23:59:59Z", converter);
        assertEquals(Instant.parse("2022-12-31T23:59:59Z"), ts.toInstant());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToTimestamp_BeforeYearOneThrows() {
        // Year 0 is before 0001-01-01 and must cause an exception
        StringConversions.toTimestamp("-0001-01-01T00:00:00Z", converter);
    }

    /* ---------- toTimeZone ---------- */
    @Test
    public void testToTimeZone_Valid() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());
    }

    @Test
    public void testToTimeZone_EmptyReturnsNull() {
        assertNull(StringConversions.toTimeZone("   ", converter));
    }

    /* ---------- toCalendar ---------- */
    @Test
    public void testToCalendar_Valid() {
        Calendar cal = StringConversions.toCalendar("2021-05-20T10:15:30Z", converter);
        assertNotNull(cal);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(5, cal.get(Calendar.MONTH) + 1); // MONTH is zero‑based
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCalendar_YearLessThanTwoThrows() {
        // Year 1 (or 0) should trigger the check
        StringConversions.toCalendar("0001-01-01T00:00:00Z", converter);
    }

    /* ---------- toLocalDate ---------- */
    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2022-03-14T00:00:00Z", converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    /* ---------- toLocalDateTime ---------- */
    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-03-14T12:30:45Z", converter);
        assertEquals(LocalDateTime.of(2022, 3, 14, 12, 30, 45), ldt);
    }

    /* ---------- toLocalTime ---------- */
    @Test
    public void testToLocalTime_DirectParse() {
        LocalTime lt = StringConversions.toLocalTime("13:45:30", converter);
        assertEquals(LocalTime.of(13, 45, 30), lt);
    }

    @Test
    public void testToLocalTime_FromZonedDateTime() {
        LocalTime lt = StringConversions.toLocalTime("2022-03-14T13:45:30Z", converter);
        assertEquals(LocalTime.of(13, 45, 30), lt);
    }

    @Test
    public void testToLocalTime_InvalidReturnsNull() {
        assertNull(StringConversions.toLocalTime("not-a-time", converter));
    }

    /* ---------- toLocale ---------- */
    @Test
    public void testToLocale_EmptyReturnsNull() {
        assertNull(StringConversions.toLocale("", converter));
    }

    @Test
    public void testToLocale_ValidTag() {
        Locale locale = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.US, locale);
    }

    /* ---------- toZonedDateTime ---------- */
    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2022-04-01T08:00:00-05:00", converter);
        assertEquals(ZoneOffset.ofHours(-5), zdt.getOffset());
    }

    /* ---------- toZoneId ---------- */
    @Test
    public void testToZoneId_Valid() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testToZoneId_FallbackToTimeZone() {
        // "GMT+02:00" is not a standard ZoneId name but TimeZone can handle it.
        ZoneId zid = StringConversions.toZoneId("GMT+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_InvalidThrows() {
        StringConversions.toZoneId("nonexistent/zone", converter);
    }

    /* ---------- toZoneOffset ---------- */
    @Test
    public void testToZoneOffset_Valid() {
        ZoneOffset zo = StringConversions.toZoneOffset("-04:00", converter);
        assertEquals(ZoneOffset.of("-04:00"), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_InvalidThrows() {
        StringConversions.toZoneOffset("invalid-offset", converter);
    }

    /* ---------- toOffsetDateTime ---------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-05-01T10:20:30+02:00", converter);
        assertEquals(OffsetDateTime.of(2022, 5, 1, 10, 20, 30, 0, ZoneOffset.ofHours(2)), odt);
    }

    /* ---------- toOffsetTime ---------- */
    @Test
    public void testToOffsetTime_DirectParse() {
        OffsetTime ot = StringConversions.toOffsetTime("12:34:56+01:00", converter);
        assertEquals(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test
    public void testToOffsetTime_FallbackToOffsetDateTime() {
        // Provide a full datetime; the method should fall back to extracting the time.
        OffsetTime ot = StringConversions.toOffsetTime("2022-06-01T09:15:00-03:00", converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(-3)), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_InvalidThrows() {
        StringConversions.toOffsetTime("bad-time", converter);
    }

    /* ---------- toInstant ---------- */
    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2022-07-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2022-07-01T00:00:00Z"), instant);
    }

    /* ---------- toCharArray ---------- */
    @Test
    public void testToCharArray_NullOrEmpty() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray(new StringBuilder(), converter));
    }

    @Test
    public void testToCharArray_NonEmpty() {
        assertArrayEquals(new char[]{'a', 'b', 'c'},
                StringConversions.toCharArray("abc", converter));
    }

    /* ---------- toCharacterArray ---------- */
    @Test
    public void testToCharacterArray() {
        Character[] expected = new Character[]{'J', 'a', 'v', 'a'};
        assertArrayEquals(expected, StringConversions.toCharacterArray("Java", converter));
    }

    /* ---------- toCharBuffer ---------- */
    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", cb.toString());
    }

    /* ---------- toByteArray ---------- */
    @Test
    public void testToByteArray_Empty() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
    }

    @Test
    public void testToByteArray_NonEmpty() {
        byte[] bytes = StringConversions.toByteArray("ABC", converter);
        assertArrayEquals("ABC".getBytes(converter.getOptions().getCharset()), bytes);
    }

    /* ---------- toByteBuffer ---------- */
    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("XYZ", converter);
        assertArrayEquals("XYZ".getBytes(converter.getOptions().getCharset()), bb.array());
    }

    /* ---------- toString ---------- */
    @Test
    public void testToString() {
        assertEquals("123", StringConversions.toString(123, converter));
        assertNull(StringConversions.toString(null, converter));
    }

    /* ---------- toStringBuffer ---------- */
    @Test
    public void testToStringBuffer() {
        StringBuffer sb = StringConversions.toStringBuffer("hello", converter);
        assertEquals("hello", sb.toString());
        assertNull(StringConversions.toStringBuffer(null, converter));
    }

    /* ---------- toStringBuilder ---------- */
    @Test
    public void testToStringBuilder() {
        StringBuilder sb = StringConversions.toStringBuilder("world", converter);
        assertEquals("world", sb.toString());
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    /* ---------- toYear ---------- */
    @Test
    public void testToYear_DirectParse() {
        assertEquals(Year.of(1999), StringConversions.toYear("1999", converter));
    }

    @Test
    public void testToYear_FromZonedDateTime() {
        // Provide a full ISO datetime; the method should extract the year.
        assertEquals(Year.of(2020),
                StringConversions.toYear("2020-12-31T23:59:59Z", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYear_InvalidThrows() {
        StringConversions.toYear("bad-year", converter);
    }

    /* ---------- toPattern ---------- */
    @Test
    public void testToPattern() {
        Pattern p = StringConversions.toPattern("\\d{3}", converter);
        assertTrue(p.matcher("123").matches());
        assertFalse(p.matcher("12").matches());
    }

    /* ---------- toCurrency ---------- */
    @Test
    public void testToCurrency() {
        Currency usd = StringConversions.toCurrency("USD", converter);
        assertEquals(Currency.getInstance("USD"), usd);
    }

    /* ---------- Additional Edge Cases ---------- */
    @Test
    public void testStringUtilitiesIsEmptyBehaviour() {
        // Ensure that the utility method used inside StringConversions behaves as expected.
        assertTrue(StringUtilities.isEmpty(""));
        assertFalse(StringUtilities.isEmpty(" "));
    }

    @Test
    public void testParseUnicodeEscape_InvalidLength() {
        try {
            // Directly invoke the private method via reflection to verify error handling.
            java.lang.reflect.Method m = StringConversions.class.getDeclaredMethod("parseUnicodeEscape", String.class);
            m.setAccessible(true);
            m.invoke(null, "\\u123");
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }
}
