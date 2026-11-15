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
import java.util.*;

public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // The default options give us a sensible charset (UTF‑8) and the default ZoneId.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic conversion tests                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("abc", StringConversions.asString("abc"));
        assertEquals("123", StringConversions.asString(123));
    }

    @Test
    public void testToByte() {
        assertEquals((byte) 0, (byte) StringConversions.toByte("", converter));
        assertEquals((byte) 127, (byte) StringConversions.toByte("127", converter));
        assertEquals((byte) -128, (byte) StringConversions.toByte("-128", converter));

        // value out of range – should be rounded via BigDecimal then clipped
        assertEquals((byte) 127, (byte) StringConversions.toByte("127.9", converter));

        // non‑numeric string that can be interpreted as a long within range
        assertEquals((byte) 100, (byte) StringConversions.toByte("100", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteInvalid() {
        // outside Byte range and not parseable as a long
        StringConversions.toByte("200", converter);
    }

    @Test
    public void testToShort() {
        assertEquals((short) 0, (short) StringConversions.toShort("", converter));
        assertEquals((short) 32767, (short) StringConversions.toShort("32767", converter));
        assertEquals((short) -32768, (short) StringConversions.toShort("-32768", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShortInvalid() {
        StringConversions.toShort("40000", converter);
    }

    @Test
    public void testToInt() {
        assertEquals(0, (int) StringConversions.toInt("", converter));
        assertEquals(Integer.MAX_VALUE, (int) StringConversions.toInt(String.valueOf(Integer.MAX_VALUE), converter));
        assertEquals(Integer.MIN_VALUE, (int) StringConversions.toInt(String.valueOf(Integer.MIN_VALUE), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToIntInvalid() {
        StringConversions.toInt("3000000000", converter); // > Integer.MAX_VALUE
    }

    @Test
    public void testToLong() {
        assertEquals(0L, (long) StringConversions.toLong("", converter));
        assertEquals(Long.MAX_VALUE, (long) StringConversions.toLong(String.valueOf(Long.MAX_VALUE), converter));
        assertEquals(Long.MIN_VALUE, (long) StringConversions.toLong(String.valueOf(Long.MIN_VALUE), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLongInvalid() {
        // value > Long.MAX_VALUE
        StringConversions.toLong("9223372036854775808", converter);
    }

    @Test
    public void testToFloat() {
        assertEquals(0f, StringConversions.toFloat("", converter), 0f);
        assertEquals(1.23f, StringConversions.toFloat("1.23", converter), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloatInvalid() {
        StringConversions.toFloat("not-a-float", converter);
    }

    @Test
    public void testToDouble() {
        assertEquals(0.0, StringConversions.toDouble("", converter), 0.0);
        assertEquals(12345.6789, StringConversions.toDouble("12345.6789", converter), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDoubleInvalid() {
        StringConversions.toDouble("NaNabc", converter);
    }

    @Test
    public void testToAtomicBoolean() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("t", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("1", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("y", converter).get());
    }

    @Test
    public void testToAtomicInteger() {
        assertEquals(42, StringConversions.toAtomicInteger("42", converter).get());
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(123456789L, StringConversions.toAtomicLong("123456789", converter).get());
    }

    @Test
    public void testToBoolean() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
        assertTrue(StringConversions.toBoolean("TRUE", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    @Test
    public void testToCharacter() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
        assertEquals('A', StringConversions.toCharacter("A", converter));
        assertEquals('Z', StringConversions.toCharacter("90", converter)); // 90 -> 'Z'
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterInvalidNumber() {
        StringConversions.toCharacter("99999", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterInvalidUnicode() {
        StringConversions.toCharacter("\\u123", converter); // wrong length
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigIntegerInvalid() {
        StringConversions.toBigInteger("12.34", converter);
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("12345.6789"), StringConversions.toBigDecimal("12345.6789", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimalInvalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    @Test
    public void testToURL() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLInvalid() {
        StringConversions.toURL("ht!tp://[bad]", converter);
    }

    @Test
    public void testToURI() {
        URI uri = StringConversions.toURI("urn:example:test", converter);
        assertEquals("urn", uri.getScheme());
        assertEquals("example:test", uri.getSchemeSpecificPart());
    }

    private enum SampleEnum { ONE, TWO }

    @Test
    public void testEnumToString() {
        assertEquals("ONE", StringConversions.enumToString(SampleEnum.ONE, converter));
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    @Test
    public void testToUUID() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUIDInvalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    @Test
    public void testToDuration() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofMillis(20345), d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDurationInvalid() {
        StringConversions.toDuration("20 seconds", converter);
    }

    @Test
    public void testToClass() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClassNotFound() {
        StringConversions.toClass("com.nonexistent.FooBar", converter);
    }

    @Test
    public void testToMonthDay() {
        // ISO format
        MonthDay md = StringConversions.toMonthDay("--12-25", converter);
        assertEquals(MonthDay.of(12, 25), md);

        // MM.DD pattern
        md = StringConversions.toMonthDay("3.4", converter);
        assertEquals(MonthDay.of(3, 4), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDayInvalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = StringConversions.toYearMonth("2023-07", converter);
        assertEquals(YearMonth.of(2023, 7), ym);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonthInvalid() {
        StringConversions.toYearMonth("2023/07", converter);
    }

    @Test
    public void testToPeriod() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPeriodInvalid() {
        StringConversions.toPeriod("2 years", converter);
    }

    @Test
    public void testToDate() {
        Date date = StringConversions.toDate("2023-01-01T12:34:56Z", converter);
        assertNotNull(date);
        Instant instant = date.toInstant();
        assertEquals(Instant.parse("2023-01-01T12:34:56Z"), instant);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("2023-01-01T00:00:00Z", converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2023, 1, 1)), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = StringConversions.toTimestamp("2023-01-01T00:00:00Z", converter);
        assertEquals(Timestamp.from(Instant.parse("2023-01-01T00:00:00Z")), ts);
    }

    @Test
    public void testToTimeZone() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), tz);
    }

    @Test
    public void testToTimeZoneNullOrEmpty() {
        assertNull(StringConversions.toTimeZone(null, converter));
        assertNull(StringConversions.toTimeZone("   ", converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = StringConversions.toCalendar("2023-03-15T10:00:00Z", converter);
        assertNotNull(cal);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(3, cal.get(Calendar.MONTH) + 1);
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2023-04-01T00:00:00Z", converter);
        assertEquals(LocalDate.of(2023, 4, 1), ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2023-04-01T12:34:56Z", converter);
        assertEquals(LocalDateTime.of(2023, 4, 1, 12, 34, 56), ldt);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = StringConversions.toLocalTime("15:30:45", converter);
        assertEquals(LocalTime.of(15, 30, 45), lt);
    }

    @Test
    public void testToLocalTimeWithZonedFallback() {
        // using a full datetime string – the method will fall back to ZonedDateTime parsing
        LocalTime lt = StringConversions.toLocalTime("2023-05-01T08:15:00Z", converter);
        assertEquals(LocalTime.of(8, 15, 0), lt);
    }

    @Test
    public void testToLocale() {
        Locale locale = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), locale);
    }

    @Test
    public void testToLocaleEmpty() {
        assertNull(StringConversions.toLocale("", converter));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2023-06-01T10:20:30+02:00", converter);
        assertEquals(ZonedDateTime.parse("2023-06-01T10:20:30+02:00"), zdt);
    }

    @Test
    public void testToZoneId() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneIdInvalid() {
        // "GMT" is the fallback for unknown IDs; we force an error by passing an unknown ID.
        StringConversions.toZoneId("UNKNOWN_ZONE_ID_12345", converter);
    }

    @Test
    public void testToZoneOffset() {
        ZoneOffset zo = StringConversions.toZoneOffset("-05:00", converter);
        assertEquals(ZoneOffset.of("-05:00"), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffsetInvalid() {
        StringConversions.toZoneOffset("not-an-offset", converter);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2023-07-01T12:00:00+01:00", converter);
        assertEquals(OffsetDateTime.parse("2023-07-01T12:00:00+01:00"), odt);
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime ot = StringConversions.toOffsetTime("13:45:30+02:00", converter);
        assertEquals(OffsetTime.parse("13:45:30+02:00"), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeInvalid() {
        StringConversions.toOffsetTime("invalid-time", converter);
    }

    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2023-08-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2023-08-01T00:00:00Z"), instant);
    }

    @Test
    public void testToCharArray() {
        char[] arr = StringConversions.toCharArray("hello", converter);
        assertArrayEquals(new char[]{'h','e','l','l','o'}, arr);
    }

    @Test
    public void testToCharArrayEmpty() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
    }

    @Test
    public void testToCharacterArray() {
        Character[] arr = StringConversions.toCharacterArray("ab", converter);
        assertArrayEquals(new Character[]{'a','b'}, arr);
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", cb.toString());
    }

    @Test
    public void testToByteArray() {
        byte[] bytes = StringConversions.toByteArray("abc", converter);
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testToByteArrayEmptyOrNull() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("xyz", converter);
        assertArrayEquals("xyz".getBytes(StandardCharsets.UTF_8), bb.array());
    }

    @Test
    public void testToString() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("123", StringConversions.toString(123, converter));
    }

    @Test
    public void testToStringBuffer() {
        StringBuffer sb = StringConversions.toStringBuffer("test", converter);
        assertEquals("test", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        StringBuilder sb = StringConversions.toStringBuilder("builder", converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToYear() {
        Year y = StringConversions.toYear("2025", converter);
        assertEquals(Year.of(2025), y);
    }

    @Test
    public void testToYearFromZonedDateTimeFallback() {
        Year y = StringConversions.toYear("2025-12-31T23:59:59Z", converter);
        assertEquals(Year.of(2025), y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearInvalid() {
        StringConversions.toYear("not-a-year", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge cases for numeric conversions (empty, whitespace, null)          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumericEmptyStrings() {
        assertEquals((byte)0, (byte) StringConversions.toByte("", converter));
        assertEquals((short)0, (short) StringConversions.toShort("", converter));
        assertEquals(0, (int) StringConversions.toInt("", converter));
        assertEquals(0L, (long) StringConversions.toLong("", converter));
        assertEquals(0f, StringConversions.toFloat("", converter), 0f);
        assertEquals(0.0, StringConversions.toDouble("", converter), 0.0);
    }

    @Test
    public void testNumericWhitespaceStrings() {
        // whitespace is not considered empty by StringUtilities.isEmpty,
        // so parsing will be attempted and will fail.
        try {
            StringConversions.toInt("   ", converter);
            fail("Expected IllegalArgumentException for whitespace string");
        } catch (IllegalArgumentException ignored) { }
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling for object‑to‑string based converters                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullInputsForStringBasedMethods() {
        assertNull(StringConversions.toURL(null, converter));
        assertNull(StringConversions.toURI(null, converter));
        assertNull(StringConversions.toLocale(null, converter));
        assertNull(StringConversions.toZoneId(null, converter));
        assertNull(StringConversions.toZoneOffset(null, converter));
        assertNull(StringConversions.toZonedDateTime(null, converter));
        assertNull(StringConversions.toCharArray(null, converter));
    }
}
