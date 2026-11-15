package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ClassUtilities;
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
import java.util.*;

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // If it does not, replace with an appropriate constructor call.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  asString                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAsStringWithNull() {
        assertNull(StringConversions.asString(null));
    }

    @Test
    public void testAsStringWithObject() {
        Object o = new Integer(42);
        assertEquals("42", StringConversions.asString(o));
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  toByte                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteValid() {
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte("12", converter));
    }

    @Test
    public void testToByteEmptyString() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
    }

    @Test
    public void testToByteOutOfRangeButWithinBigDecimal() {
        // 130 is > Byte.MAX_VALUE (127) but within the BigDecimal range check.
        // The method should fall back to toLong and then truncate.
        assertEquals(Byte.valueOf((byte) -126), // 130 - 256 = -126 when cast to byte
                StringConversions.toByte("130", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteUnparseable() {
        StringConversions.toByte("abc", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  toShort                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToShortValid() {
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345", converter));
    }

    @Test
    public void testToShortEmptyString() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShortUnparseable() {
        StringConversions.toShort("not-a-number", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  toInt                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToIntValid() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
    }

    @Test
    public void testToIntEmptyString() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToIntUnparseable() {
        StringConversions.toInt("123abc", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 5.  toLong                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLongValid() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
    }

    @Test
    public void testToLongEmptyString() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLongUnparseable() {
        StringConversions.toLong("12.34", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 6.  toFloat                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToFloatValid() {
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
    }

    @Test
    public void testToFloatEmptyString() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloatUnparseable() {
        StringConversions.toFloat("abc", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 7.  toDouble                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDoubleValid() {
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
    }

    @Test
    public void testToDoubleEmptyString() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDoubleUnparseable() {
        StringConversions.toDouble("not-a-number", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 8.  toAtomicBoolean                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicBooleanVariousTrueValues() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("TRUE", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("t", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("1", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("y", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("\"true\"", converter).get());
    }

    @Test
    public void testToAtomicBooleanVariousFalseValues() {
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("FALSE", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("0", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("n", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /* 9.  toAtomicInteger                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        assertEquals(123, StringConversions.toAtomicInteger("123", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. toAtomicLong                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong() {
        assertEquals(123456789L, StringConversions.toAtomicLong("123456789", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. toBoolean                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBooleanExactMatches() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    @Test
    public void testToBooleanLenientMatches() {
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("TRUE", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. toCharacter                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharacterEmptyString() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
    }

    @Test
    public void testToCharacterSingleChar() {
        assertEquals('Z', StringConversions.toCharacter("Z", converter));
    }

    @Test
    public void testToCharacterNumericString() {
        assertEquals('A', StringConversions.toCharacter("65", converter));
    }

    @Test
    public void testToCharacterUnicodeEscape() {
        assertEquals('ß', StringConversions.toCharacter("\\u00DF", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterInvalidUnicodeEscape() {
        StringConversions.toCharacter("\\u0G00", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterUnparseable() {
        StringConversions.toCharacter("abc", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 13. toBigInteger                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigIntegerValid() {
        BigInteger expected = new BigInteger("12345678901234567890");
        assertEquals(expected, StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test
    public void testToBigIntegerEmpty() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigIntegerInvalid() {
        StringConversions.toBigInteger("12.34", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. toBigDecimal                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimalValid() {
        BigDecimal expected = new BigDecimal("12345.6789");
        assertEquals(0, expected.compareTo(StringConversions.toBigDecimal("12345.6789", converter)));
    }

    @Test
    public void testToBigDecimalEmpty() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimalInvalid() {
        StringConversions.toBigDecimal("not-a-number", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 15. toURL                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToURLValid() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    public void testToURLEmpty() {
        assertNull(StringConversions.toURL("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLInvalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 16. toURI                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToURIValid() {
        URI uri = StringConversions.toURI("http://example.com/path?query=1", converter);
        assertEquals("/path", uri.getPath());
    }

    @Test
    public void testToURIEmpty() {
        assertNull(StringConversions.toURI("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 17. enumToString                                                       */
    /* ---------------------------------------------------------------------- */

    private enum SampleEnum { FIRST, SECOND }

    @Test
    public void testEnumToString() {
        assertEquals("FIRST", StringConversions.enumToString(SampleEnum.FIRST, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 18. toUUID                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToUUIDValid() {
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        UUID uuid = StringConversions.toUUID(uuidStr, converter);
        assertEquals(UUID.fromString(uuidStr), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUIDInvalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 19. toDuration                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDurationValid() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofSeconds(20, 345_000_000), d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDurationInvalid() {
        StringConversions.toDuration("20 seconds", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 20. toClass                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToClassValid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClassInvalid() {
        StringConversions.toClass("com.nonexistent.FooBar", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 21. toMonthDay                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMonthDayIso() {
        MonthDay md = StringConversions.toMonthDay("--12-25", converter);
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToMonthDaySimplePattern() {
        MonthDay md = StringConversions.toMonthDay("12.25", converter);
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToMonthDayFromZonedDateTimeString() {
        // Using a full datetime; the method should extract month and day.
        MonthDay md = StringConversions.toMonthDay("2022-07-04T10:15:30Z", converter);
        assertEquals(MonthDay.of(7, 4), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDayInvalid() {
        StringConversions.toMonthDay("invalid-monthday", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 22. toYearMonth                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToYearMonthIso() {
        YearMonth ym = StringConversions.toYearMonth("2021-09", converter);
        assertEquals(YearMonth.of(2021, 9), ym);
    }

    @Test
    public void testToYearMonthFromZonedDateTimeString() {
        YearMonth ym = StringConversions.toYearMonth("2020-12-31T23:59:59Z", converter);
        assertEquals(YearMonth.of(2020, 12), ym);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonthInvalid() {
        StringConversions.toYearMonth("not-a-yearmonth", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 23. toPeriod                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToPeriodValid() {
        Period p = StringConversions.toPeriod("P2Y3M4D", converter);
        assertEquals(Period.of(2, 3, 4), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriodInvalid() {
        StringConversions.toPeriod("2 years", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 24. toDate / toSqlDate / toTimestamp                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDateValid() {
        Date d = StringConversions.toDate("2021-08-15T12:30:45Z", converter);
        Instant instant = Instant.parse("2021-08-15T12:30:45Z");
        assertEquals(Date.from(instant), d);
    }

    @Test
    public void testToSqlDateValid() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2021-08-15T00:00:00Z", converter);
        assertEquals(java.sql.Date.valueOf("2021-08-15"), sqlDate);
    }

    @Test
    public void testToTimestampValid() {
        Timestamp ts = StringConversions.toTimestamp("2021-08-15T12:30:45Z", converter);
        assertEquals(Timestamp.from(Instant.parse("2021-08-15T12:30:45Z")), ts);
    }

    @Test
    public void testToDateNullInput() {
        assertNull(StringConversions.toDate(null, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 25. toTimeZone                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimeZoneValid() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals(TimeZone.getTimeZone("UTC"), tz);
    }

    @Test
    public void testToTimeZoneEmpty() {
        assertNull(StringConversions.toTimeZone("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 26. toCalendar                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendarValid() {
        Calendar cal = StringConversions.toCalendar("2021-03-10T04:05:06Z", converter);
        assertNotNull(cal);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(2, cal.get(Calendar.MONTH)); // March = 2 (zero‑based)
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testToCalendarNull() {
        assertNull(StringConversions.toCalendar(null, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 27. toLocalDate, toLocalDateTime, toLocalTime                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDateValid() {
        LocalDate ld = StringConversions.toLocalDate("2022-11-05T00:00:00Z", converter);
        assertEquals(LocalDate.of(2022, 11, 5), ld);
    }

    @Test
    public void testToLocalDateTimeValid() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-11-05T13:45:30Z", converter);
        assertEquals(LocalDateTime.of(2022, 11, 5, 13, 45, 30), ldt);
    }

    @Test
    public void testToLocalTimeIso() {
        LocalTime lt = StringConversions.toLocalTime("13:45:30", converter);
        assertEquals(LocalTime.of(13, 45, 30), lt);
    }

    @Test
    public void testToLocalTimeFromZonedDateTime() {
        LocalTime lt = StringConversions.toLocalTime("2022-01-01T09:15:00+02:00", converter);
        assertEquals(LocalTime.of(9, 15, 0), lt);
    }

    @Test
    public void testToLocalTimeInvalid() {
        assertNull(StringConversions.toLocalTime("not-a-time", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 28. toLocale                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocaleValid() {
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), loc);
    }

    @Test
    public void testToLocaleEmpty() {
        assertNull(StringConversions.toLocale("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 29. toZonedDateTime                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTimeValid() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2023-04-01T12:00:00Z", converter);
        assertEquals(ZonedDateTime.parse("2023-04-01T12:00:00Z"), zdt);
    }

    @Test
    public void testToZonedDateTimeInvalid() {
        assertNull(StringConversions.toZonedDateTime("invalid-date", converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 30. toZoneId                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZoneIdValid() {
        ZoneId zid = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneIdInvalid() {
        StringConversions.toZoneId("Invalid/Zone", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 31. toZoneOffset                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZoneOffsetValid() {
        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffsetInvalid() {
        StringConversions.toZoneOffset("not-an-offset", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 32. toOffsetDateTime                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTimeValid() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-12-31T23:59:59+01:00", converter);
        assertEquals(OffsetDateTime.parse("2022-12-31T23:59:59+01:00"), odt);
    }

    @Test
    public void testToOffsetDateTimeNull() {
        assertNull(StringConversions.toOffsetDateTime(null, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 33. toOffsetTime                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetTimeIso() {
        OffsetTime ot = StringConversions.toOffsetTime("10:15:30+02:00", converter);
        assertEquals(OffsetTime.parse("10:15:30+02:00"), ot);
    }

    @Test
    public void testToOffsetTimeFallbackFromOffsetDateTime() {
        // Provide a full datetime; method should fall back to OffsetDateTime then to OffsetTime.
        OffsetTime ot = StringConversions.toOffsetTime("2022-01-01T08:30:00+03:00", converter);
        assertEquals(OffsetTime.of(8, 30, 0, 0, ZoneOffset.ofHours(3)), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeInvalid() {
        StringConversions.toOffsetTime("not-an-offset-time", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 34. toInstant                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToInstantValid() {
        Instant instant = StringConversions.toInstant("2021-06-01T12:00:00Z", converter);
        assertEquals(Instant.parse("2021-06-01T12:00:00Z"), instant);
    }

    @Test
    public void testToInstantNull() {
        assertNull(StringConversions.toInstant(null, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 35. toCharArray, toCharacterArray, toCharBuffer, toByteArray, toByteBuffer */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharArrayFromString() {
        char[] chars = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a', 'b', 'c'}, chars);
    }

    @Test
    public void testToCharArrayFromEmptyString() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
    }

    @Test
    public void testToCharacterArray() {
        Character[] chars = StringConversions.toCharacterArray("XY", converter);
        assertArrayEquals(new Character[]{'X', 'Y'}, chars);
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer buf = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", buf.toString());
    }

    @Test
    public void testToByteArray() {
        byte[] bytes = StringConversions.toByteArray("test", converter);
        assertArrayEquals("test".getBytes(converter.getOptions().getCharset()), bytes);
    }

    @Test
    public void testToByteArrayEmpty() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("abc", converter);
        assertArrayEquals("abc".getBytes(converter.getOptions().getCharset()), bb.array());
    }

    /* ---------------------------------------------------------------------- */
    /* 36. toString, toStringBuffer, toStringBuilder                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringNonNull() {
        assertEquals("123", StringConversions.toString(123, converter));
    }

    @Test
    public void testToStringNull() {
        assertNull(StringConversions.toString(null, converter));
    }

    @Test
    public void testToStringBuffer() {
        StringBuffer sb = StringConversions.toStringBuffer("buffer", converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBufferNull() {
        assertNull(StringConversions.toStringBuffer(null, converter));
    }

    @Test
    public void testToStringBuilder() {
        StringBuilder sb = StringConversions.toStringBuilder("builder", converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToStringBuilderNull() {
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 37. toYear                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToYearValid() {
        Year y = StringConversions.toYear("2020", converter);
        assertEquals(Year.of(2020), y);
    }

    @Test
    public void testToYearFromZonedDateTimeString() {
        Year y = StringConversions.toYear("2022-08-15T00:00:00Z", converter);
        assertEquals(Year.of(2022), y);
    }

    @Test
    public void testToYearInvalid() {
        assertNull(StringConversions.toYear("invalid-year", converter));
    }
}
