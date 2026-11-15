package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
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
import java.util.concurrent.atomic.*;

public class StringConversionsTest {

    /*** -----------------------------------------------------------------------
     *  Helper objects used by many conversion methods
     * ----------------------------------------------------------------------- ***/
    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // It supplies a default Charset (UTF‑8) and the default ZoneId.
        converter = new Converter(new ConverterOptions());
    }

    /*** -----------------------------------------------------------------------
     *  1. asString
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testAsString_Null() {
        assertNull(StringConversions.asString(null));
    }

    @Test
    public void testAsString_Object() {
        assertEquals("123", StringConversions.asString(123));
        assertEquals("true", StringConversions.asString(Boolean.TRUE));
    }

    /*** -----------------------------------------------------------------------
     *  2. toByte
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToByte_Valid() {
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte("12", converter));
    }

    @Test
    public void testToByte_EmptyString() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_OutOfRange() {
        // 200 is greater than Byte.MAX_VALUE (127)
        StringConversions.toByte("200", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_NonNumeric() {
        StringConversions.toByte("abc", converter);
    }

    /*** -----------------------------------------------------------------------
     *  3. toShort
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToShort_Valid() {
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345", converter));
    }

    @Test
    public void testToShort_EmptyString() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort_OutOfRange() {
        // 40000 > Short.MAX_VALUE (32767)
        StringConversions.toShort("40000", converter);
    }

    /*** -----------------------------------------------------------------------
     *  4. toInt
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToInt_Valid() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
    }

    @Test
    public void testToInt_EmptyString() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_OutOfRange() {
        // larger than Integer.MAX_VALUE
        StringConversions.toInt("3000000000", converter);
    }

    /*** -----------------------------------------------------------------------
     *  5. toLong
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToLong_Valid() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
    }

    @Test
    public void testToLong_EmptyString() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong_OutOfRange() {
        // value larger than Long.MAX_VALUE (use a 20‑digit number)
        StringConversions.toLong("99999999999999999999", converter);
    }

    /*** -----------------------------------------------------------------------
     *  6. toFloat
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToFloat_Valid() {
        assertEquals(Float.valueOf(12.34f), StringConversions.toFloat("12.34", converter));
    }

    @Test
    public void testToFloat_EmptyString() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_Invalid() {
        StringConversions.toFloat("not-a-float", converter);
    }

    /*** -----------------------------------------------------------------------
     *  7. toDouble
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToDouble_Valid() {
        assertEquals(Double.valueOf(12.3456), StringConversions.toDouble("12.3456", converter));
    }

    @Test
    public void testToDouble_EmptyString() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_Invalid() {
        StringConversions.toDouble("NaNstring", converter);
    }

    /*** -----------------------------------------------------------------------
     *  8. toAtomicBoolean
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToAtomicBoolean_True() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
    }

    @Test
    public void testToAtomicBoolean_False() {
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
    }

    @Test
    public void testToAtomicBoolean_TreatAsTrue() {
        assertTrue(StringConversions.toAtomicBoolean("1", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("y", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("T", converter).get());
    }

    /*** -----------------------------------------------------------------------
     *  9. toAtomicInteger / toAtomicLong
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToAtomicInteger() {
        assertEquals(42, StringConversions.toAtomicInteger("42", converter).get());
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(123456789L, StringConversions.toAtomicLong("123456789", converter).get());
    }

    /*** -----------------------------------------------------------------------
     * 10. toBoolean
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToBoolean_Exact() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    @Test
    public void testToBoolean_IgnoredCaseAndAliases() {
        assertTrue(StringConversions.toBoolean("TrUe", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
    }

    @Test
    public void testToBoolean_Unrecognized() {
        assertFalse(StringConversions.toBoolean("maybe", converter));
    }

    /*** -----------------------------------------------------------------------
     * 11. toCharacter
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToCharacter_SingleChar() {
        assertEquals('a', StringConversions.toCharacter("a", converter));
    }

    @Test
    public void testToCharacter_EmptyString() {
        assertEquals((char) 0, StringConversions.toCharacter("", converter));
    }

    @Test
    public void testToCharacter_AllDigits() {
        assertEquals('A', StringConversions.toCharacter("65", converter)); // 65 = 'A'
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_InvalidUnicodeEscape() {
        StringConversions.toCharacter("\\u123", converter); // too short
    }

    @Test
    public void testToCharacter_UnicodeEscape() {
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));
    }

    /*** -----------------------------------------------------------------------
     * 12. toBigInteger
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToBigInteger_Valid() {
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test
    public void testToBigInteger_EmptyString() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigInteger_Invalid() {
        StringConversions.toBigInteger("12.34", converter);
    }

    /*** -----------------------------------------------------------------------
     * 13. toBigDecimal
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToBigDecimal_Valid() {
        assertEquals(new BigDecimal("12345.6789"), StringConversions.toBigDecimal("12345.6789", converter));
    }

    @Test
    public void testToBigDecimal_EmptyString() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimal_Invalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    /*** -----------------------------------------------------------------------
     * 14. toURL
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToURL_Valid() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    public void testToURL_EmptyString() {
        assertNull(StringConversions.toURL("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURL_Invalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    /*** -----------------------------------------------------------------------
     * 15. toURI
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToURI_Valid() {
        URI uri = StringConversions.toURI("http://example.com/path?query=1", converter);
        assertEquals("/path", uri.getPath());
    }

    @Test
    public void testToURI_EmptyString() {
        assertNull(StringConversions.toURI("", converter));
    }

    /*** -----------------------------------------------------------------------
     * 16. enumToString
     * ----------------------------------------------------------------------- ***/
    private enum SampleEnum { FIRST, SECOND }

    @Test
    public void testEnumToString() {
        assertEquals("FIRST", StringConversions.enumToString(SampleEnum.FIRST, converter));
        assertEquals("SECOND", StringConversions.enumToString(SampleEnum.SECOND, converter));
    }

    /*** -----------------------------------------------------------------------
     * 17. toUUID
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToUUID_Valid() {
        UUID expected = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertEquals(expected, StringConversions.toUUID("123e4567-e89b-12d3-a456-426655440000", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_Invalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    /*** -----------------------------------------------------------------------
     * 18. toDuration
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToDuration_DecimalSeconds() {
        Duration d = StringConversions.toDuration("123.456", converter);
        assertEquals(Duration.ofSeconds(123, 456_000_000), d);
    }

    @Test
    public void testToDuration_ISO8601() {
        Duration d = StringConversions.toDuration("PT1H2M3.4S", converter);
        assertEquals(Duration.ofHours(1).plusMinutes(2).plusSeconds(3).plusMillis(400), d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_Invalid() {
        StringConversions.toDuration("not-a-duration", converter);
    }

    /*** -----------------------------------------------------------------------
     * 19. toClass
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToClass_ExistingClass() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_NonExisting() {
        StringConversions.toClass("com.nonexistent.FooBar", converter);
    }

    /*** -----------------------------------------------------------------------
     * 20. toMonthDay
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToMonthDay_ISO() {
        MonthDay md = StringConversions.toMonthDay("2007-12-03", converter);
        assertEquals(MonthDay.of(12, 3), md);
    }

    @Test
    public void testToMonthDay_MM_DD_Pattern() {
        MonthDay md = StringConversions.toMonthDay("3.7", converter);
        assertEquals(MonthDay.of(3, 7), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_Invalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    /*** -----------------------------------------------------------------------
     * 21. toYearMonth
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToYearMonth_ISO() {
        YearMonth ym = StringConversions.toYearMonth("2025-11", converter);
        assertEquals(YearMonth.of(2025, 11), ym);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_Invalid() {
        StringConversions.toYearMonth("13/2025", converter);
    }

    /*** -----------------------------------------------------------------------
     * 22. toPeriod
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToPeriod_ISO() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriod_Invalid() {
        StringConversions.toPeriod("not-a-period", converter);
    }

    /*** -----------------------------------------------------------------------
     * 23. toDate
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToDate_ISODateTime() {
        // 2020-01-02T03:04:05Z => epoch milli 1577925845000
        Date date = StringConversions.toDate("2020-01-02T03:04:05Z", converter);
        assertEquals(Instant.parse("2020-01-02T03:04:05Z"), date.toInstant());
    }

    @Test
    public void testToDate_Null() {
        assertNull(StringConversions.toDate(null, converter));
    }

    /*** -----------------------------------------------------------------------
     * 24. toSqlDate
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToSqlDate_Simple() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2021-08-15", converter);
        assertEquals(LocalDate.of(2021, 8, 15), sqlDate.toLocalDate());
    }

    @Test
    public void testToSqlDate_ISO8601() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2021-08-15T00:00:00Z", converter);
        assertEquals(LocalDate.of(2021, 8, 15), sqlDate.toLocalDate());
    }

    @Test
    public void testToSqlDate_InvalidReturnsNull() {
        // The method falls back to DateUtilities.parseDate which returns null for nonsense.
        java.sql.Date sqlDate = StringConversions.toSqlDate("not-a-date", converter);
        assertNull(sqlDate);
    }

    /*** -----------------------------------------------------------------------
     * 25. toTimestamp
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToTimestamp_Valid() {
        Timestamp ts = StringConversions.toTimestamp("2022-01-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2022-01-01T00:00:00Z"), ts.toInstant());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToTimestamp_TooEarly() {
        // year 0000 is before the allowed range
        StringConversions.toTimestamp("-0001-01-01T00:00:00Z", converter);
    }

    /*** -----------------------------------------------------------------------
     * 26. toTimeZone
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToTimeZone_Valid() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());
    }

    @Test
    public void testToTimeZone_Empty() {
        assertNull(StringConversions.toTimeZone("", converter));
    }

    /*** -----------------------------------------------------------------------
     * 27. toCalendar
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToCalendar_Valid() {
        Calendar cal = StringConversions.toCalendar("2023-03-10T12:00:00Z", converter);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(3, cal.get(Calendar.MONTH) + 1); // MONTH is zero‑based
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCalendar_YearTooLow() {
        // Year 1 is not allowed (Calendar limitation)
        StringConversions.toCalendar("0001-01-01T00:00:00Z", converter);
    }

    /*** -----------------------------------------------------------------------
     * 28. toLocalDate / toLocalDateTime / toLocalTime
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2024-02-29T00:00:00Z", converter);
        assertEquals(LocalDate.of(2024, 2, 29), ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2024-02-29T13:45:30Z", converter);
        assertEquals(LocalDateTime.of(2024, 2, 29, 13, 45, 30), ldt);
    }

    @Test
    public void testToLocalTime_ISO() {
        LocalTime lt = StringConversions.toLocalTime("13:45:30", converter);
        assertEquals(LocalTime.of(13, 45, 30), lt);
    }

    @Test
    public void testToLocalTime_FromZonedDateTime() {
        // "2024-02-29T13:45:30+02:00" => time part is 13:45:30
        LocalTime lt = StringConversions.toLocalTime("2024-02-29T13:45:30+02:00", converter);
        assertEquals(LocalTime.of(13, 45, 30), lt);
    }

    /*** -----------------------------------------------------------------------
     * 29. toLocale
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToLocale_Valid() {
        Locale locale = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.US, locale);
    }

    @Test
    public void testToLocale_Empty() {
        assertNull(StringConversions.toLocale("", converter));
    }

    /*** -----------------------------------------------------------------------
     * 30. toZonedDateTime
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToZonedDateTime_ISO() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2025-12-31T23:59:59Z", converter);
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
        assertEquals(2025, zdt.getYear());
    }

    /*** -----------------------------------------------------------------------
     * 31. toZoneId
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToZoneId_Valid() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_Invalid() {
        StringConversions.toZoneId("not-a-zone", converter);
    }

    /*** -----------------------------------------------------------------------
     * 32. toZoneOffset
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToZoneOffset_Valid() {
        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_Invalid() {
        StringConversions.toZoneOffset("GMT+2", converter);
    }

    /*** -----------------------------------------------------------------------
     * 33. toOffsetDateTime / toOffsetTime
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2023-06-01T12:00:00+03:00", converter);
        assertEquals(OffsetDateTime.of(2023, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(3)), odt);
    }

    @Test
    public void testToOffsetTime_ISO() {
        OffsetTime ot = StringConversions.toOffsetTime("10:15:30+01:00", converter);
        assertEquals(OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Invalid() {
        StringConversions.toOffsetTime("not-a-time", converter);
    }

    /*** -----------------------------------------------------------------------
     * 34. toInstant
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2022-12-31T23:59:59Z", converter);
        assertEquals(Instant.parse("2022-12-31T23:59:59Z"), instant);
    }

    /*** -----------------------------------------------------------------------
     * 35. toCharArray / toCharacterArray
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToCharArray_NullOrEmpty() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray(null, converter));
    }

    @Test
    public void testToCharArray_Normal() {
        char[] arr = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a', 'b', 'c'}, arr);
    }

    @Test
    public void testToCharacterArray() {
        Character[] arr = StringConversions.toCharacterArray("xy", converter);
        assertArrayEquals(new Character[]{'x', 'y'}, arr);
    }

    /*** -----------------------------------------------------------------------
     * 36. toCharBuffer
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", cb.toString());
    }

    /*** -----------------------------------------------------------------------
     * 37. toByteArray / toByteBuffer
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToByteArray_Empty() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToByteArray_Normal() {
        byte[] bytes = StringConversions.toByteArray("abc", converter);
        assertArrayEquals("abc".getBytes(converter.getOptions().getCharset()), bytes);
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("data", converter);
        assertArrayEquals("data".getBytes(converter.getOptions().getCharset()), bb.array());
    }

    /*** -----------------------------------------------------------------------
     * 38. toString / toStringBuffer / toStringBuilder
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToString_Null() {
        assertNull(StringConversions.toString(null, converter));
    }

    @Test
    public void testToString_NonNull() {
        assertEquals("123", StringConversions.toString(123, converter));
    }

    @Test
    public void testToStringBuffer() {
        StringBuffer sb = StringConversions.toStringBuffer("buf", converter);
        assertEquals("buf", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        StringBuilder sb = StringConversions.toStringBuilder("builder", converter);
        assertEquals("builder", sb.toString());
    }

    /*** -----------------------------------------------------------------------
     * 39. toYear
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToYear_Exact() {
        Year y = StringConversions.toYear("2020", converter);
        assertEquals(Year.of(2020), y);
    }

    @Test
    public void testToYear_FromZonedDateTime() {
        Year y = StringConversions.toYear("2020-07-01T00:00:00Z", converter);
        assertEquals(Year.of(2020), y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYear_Invalid() {
        StringConversions.toYear("not-a-year", converter);
    }

    /*** -----------------------------------------------------------------------
     * 40. toPattern
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToPattern() {
        Pattern p = StringConversions.toPattern("\\d+", converter);
        assertTrue(p.matcher("12345").matches());
    }

    /*** -----------------------------------------------------------------------
     * 41. toCurrency
     * ----------------------------------------------------------------------- ***/
    @Test
    public void testToCurrency() {
        Currency cur = StringConversions.toCurrency("USD", converter);
        assertEquals(Currency.getInstance("USD"), cur);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCurrency_Invalid() {
        StringConversions.toCurrency("XYZ", converter);
    }
}
