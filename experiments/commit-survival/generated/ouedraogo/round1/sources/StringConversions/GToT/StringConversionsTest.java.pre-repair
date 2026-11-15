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

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    /** A tiny enum used for {@code enumToString} tests. */
    private enum SampleEnum { ONE, TWO }

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Simple “happy‑path” tests for each public static method
     * ------------------------------------------------------------- */

    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte) 127), StringConversions.toByte("127", converter));
        assertEquals(Byte.valueOf((byte) -128), StringConversions.toByte("-128", converter));
        // numeric string that needs BigDecimal handling
        assertEquals(Byte.valueOf((byte) 100), StringConversions.toByte("100.9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteOutOfRange() {
        // 200 is outside the byte range → IllegalArgumentException
        StringConversions.toByte("200", converter);
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
        assertEquals(Short.valueOf((short) 32767), StringConversions.toShort("32767", converter));
        assertEquals(Short.valueOf((short) -32768), StringConversions.toShort("-32768", converter));
        // BigDecimal fallback
        assertEquals(Short.valueOf((short) 12345), StringConversions.toShort("12345.9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShortOutOfRange() {
        StringConversions.toShort("40000", converter);
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
        assertEquals(Integer.valueOf(2147483647), StringConversions.toInt("2147483647", converter));
        assertEquals(Integer.valueOf(-2147483648), StringConversions.toInt("-2147483648", converter));
        // BigDecimal fallback
        assertEquals(Integer.valueOf(123456789), StringConversions.toInt("123456789.99", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToIntOutOfRange() {
        StringConversions.toInt("3000000000", converter);
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
        assertEquals(Long.valueOf(Long.MAX_VALUE), StringConversions.toLong(Long.toString(Long.MAX_VALUE), converter));
        assertEquals(Long.valueOf(Long.MIN_VALUE), StringConversions.toLong(Long.toString(Long.MIN_VALUE), converter));
        // BigDecimal fallback
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123.456", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLongOutOfRange() {
        // value larger than Long.MAX_VALUE
        StringConversions.toLong("9223372036854775808", converter);
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloatInvalid() {
        StringConversions.toFloat("not-a-float", converter);
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDoubleInvalid() {
        StringConversions.toDouble("NaNxyz", converter);
    }

    @Test
    public void testToAtomicBoolean() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
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
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    @Test
    public void testToCharacter() {
        assertEquals('\0', StringConversions.toCharacter("", converter));
        assertEquals('Z', StringConversions.toCharacter("Z", converter));
        assertEquals('A', StringConversions.toCharacter("65", converter)); // numeric code point
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter)); // Unicode escape
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterInvalidNumber() {
        StringConversions.toCharacter("999999", converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterInvalidUnicode() {
        StringConversions.toCharacter("\\uXYZ", converter);
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigIntegerInvalid() {
        StringConversions.toBigInteger("not-a-number", converter);
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.14159"), StringConversions.toBigDecimal("3.14159", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimalInvalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    @Test
    public void testToURL() throws Exception {
        assertNull(StringConversions.toURL("", converter));
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLInvalid() {
        StringConversions.toURL("ht!tp:// bad url", converter);
    }

    @Test
    public void testToURI() {
        assertNull(StringConversions.toURI("", converter));
        URI uri = StringConversions.toURI("http://example.com/path", converter);
        assertEquals("/path", uri.getPath());
    }

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
        assertEquals(Duration.ofSeconds(20, 345_000_000), d);
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
        // ISO format "--12-03"
        MonthDay md1 = StringConversions.toMonthDay("--12-03", converter);
        assertEquals(MonthDay.of(12, 3), md1);

        // fallback pattern "12.03"
        MonthDay md2 = StringConversions.toMonthDay("12.03", converter);
        assertEquals(MonthDay.of(12, 3), md2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDayInvalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym1 = StringConversions.toYearMonth("2021-07", converter);
        assertEquals(YearMonth.of(2021, 7), ym1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonthInvalid() {
        StringConversions.toYearMonth("not-a-year-month", converter);
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
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 15, 12, 30, 0, 0, ZoneId.of("UTC"));
        // Use the same parsing logic that StringConversions uses
        Date date = StringConversions.toDate(zdt.toString(), converter);
        assertEquals(Date.from(zdt.toInstant()), date);
    }

    @Test
    public void testToSqlDateAndTimestamp() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"));
        java.sql.Date sqlDate = StringConversions.toSqlDate(zdt.toString(), converter);
        Timestamp ts = StringConversions.toTimestamp(zdt.toString(), converter);
        assertEquals(zdt.toInstant().toEpochMilli(), sqlDate.getTime());
        assertEquals(zdt.toInstant().toEpochMilli(), ts.getTime());
    }

    @Test
    public void testToTimeZone() {
        assertNull(StringConversions.toTimeZone("", converter));
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertEquals("America/New_York", tz.getID());
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 3, 10, 9, 15, 0, 0, ZoneId.of("UTC"));
        Calendar cal = StringConversions.toCalendar(zdt.toString(), converter);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(2, cal.get(Calendar.MONTH)); // months are zero‑based
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 11, 5, 0, 0, 0, 0, ZoneId.of("UTC"));
        LocalDate ld = StringConversions.toLocalDate(zdt.toString(), converter);
        assertEquals(LocalDate.of(2022, 11, 5), ld);
    }

    @Test
    public void testToLocalDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 11, 5, 13, 45, 30, 0, ZoneId.of("UTC"));
        LocalDateTime ldt = StringConversions.toLocalDateTime(zdt.toString(), converter);
        assertEquals(LocalDateTime.of(2022, 11, 5, 13, 45, 30), ldt);
    }

    @Test
    public void testToLocalTime() {
        // Direct ISO time
        LocalTime lt1 = StringConversions.toLocalTime("10:15:30", converter);
        assertEquals(LocalTime.of(10, 15, 30), lt1);

        // Fallback from a full date‑time string
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 1, 22, 5, 0, 0, ZoneId.of("UTC"));
        LocalTime lt2 = StringConversions.toLocalTime(zdt.toString(), converter);
        assertEquals(LocalTime.of(22, 5, 0), lt2);
    }

    @Test
    public void testToLocale() {
        assertNull(StringConversions.toLocale("", converter));
        Locale locale = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), locale);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2021-08-15T12:00:00Z", converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.parse("2021-08-15T12:00:00Z"), ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToZoneId() {
        ZoneId zid = StringConversions.toZoneId("America/Los_Angeles", converter);
        assertEquals(ZoneId.of("America/Los_Angeles"), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneIdUnknown() {
        // "GMT" is the fallback for unknown IDs; the method throws for unknown IDs
        StringConversions.toZoneId("Nonexistent/Zone", converter);
    }

    @Test
    public void testToZoneOffset() {
        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffsetInvalid() {
        StringConversions.toZoneOffset("invalid", converter);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2021-09-01T10:15:30+01:00", converter);
        assertEquals(OffsetDateTime.of(2021, 9, 1, 10, 15, 30, 0, ZoneOffset.ofHours(1)), odt);
    }

    @Test
    public void testToOffsetTime() {
        // Direct ISO offset time
        OffsetTime ot1 = StringConversions.toOffsetTime("13:45:20+02:00", converter);
        assertEquals(OffsetTime.of(13, 45, 20, 0, ZoneOffset.ofHours(2)), ot1);

        // Fallback via OffsetDateTime
        OffsetTime ot2 = StringConversions.toOffsetTime("2021-10-10T08:30:00+03:00", converter);
        assertEquals(OffsetTime.of(8, 30, 0, 0, ZoneOffset.ofHours(3)), ot2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeInvalid() {
        StringConversions.toOffsetTime("not-a-time", converter);
    }

    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2021-01-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2021-01-01T00:00:00Z"), instant);
    }

    @Test
    public void testToCharArray() {
        char[] empty = StringConversions.toCharArray("", converter);
        assertArrayEquals(new char[0], empty);

        char[] chars = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[] {'a', 'b', 'c'}, chars);
    }

    @Test
    public void testToCharacterArray() {
        Character[] chars = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[]{'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", cb.toString());
    }

    @Test
    public void testToByteArrayAndByteBuffer() {
        byte[] bytes = StringConversions.toByteArray("hello", converter);
        assertArrayEquals("hello".getBytes(converter.getOptions().getCharset()), bytes);

        ByteBuffer bb = StringConversions.toByteBuffer("world", converter);
        assertArrayEquals("world".getBytes(converter.getOptions().getCharset()), bb.array());
    }

    @Test
    public void testToStringVariants() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("test", StringConversions.toString("test", converter));

        assertNull(StringConversions.toStringBuffer(null, converter));
        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));

        assertNull(StringConversions.toStringBuilder(null, converter));
        assertEquals(new StringBuilder("bld"), StringConversions.toStringBuilder("bld", converter));
    }

    @Test
    public void testToYear() {
        Year year = StringConversions.toYear("2023", converter);
        assertEquals(Year.of(2023), year);
    }

    @Test
    public void testToYearFromZonedDateTimeFallback() {
        ZonedDateTime zdt = ZonedDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
        Year year = StringConversions.toYear(zdt.toString(), converter);
        assertEquals(Year.of(1999), year);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearInvalid() {
        StringConversions.toYear("not-a-year", converter);
    }

    /* -------------------------------------------------------------
     *  Additional edge‑case tests for selected methods
     * ------------------------------------------------------------- */

    @Test
    public void testToByteEmptyStringReturnsZero() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
    }

    @Test
    public void testToShortEmptyStringReturnsZero() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test
    public void testToIntEmptyStringReturnsZero() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test
    public void testToLongEmptyStringReturnsZero() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test
    public void testToFloatEmptyStringReturnsZero() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test
    public void testToDoubleEmptyStringReturnsZero() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test
    public void testToBigIntegerEmptyStringZero() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test
    public void testToBigDecimalEmptyStringZero() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test
    public void testToTimeZoneNullString() {
        assertNull(StringConversions.toTimeZone(null, converter));
    }

    @Test
    public void testToLocaleNullString() {
        assertNull(StringConversions.toLocale(null, converter));
    }

    @Test
    public void testToZoneIdEmptyString() {
        assertNull(StringConversions.toZoneId("", converter));
    }

    @Test
    public void testToZoneOffsetEmptyString() {
        assertNull(StringConversions.toZoneOffset("", converter));
    }

    @Test
    public void testToCharArrayNullObject() {
        // The method calls from.toString(); passing null would cause NPE.
        // Verify that NPE is thrown (as per current implementation).
        try {
            StringConversions.toCharArray(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) { }
    }

    @Test
    public void testToByteArrayNullObject() {
        // asString(null) returns null, which results in empty byte array.
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToCharBufferNullObject() {
        CharBuffer cb = StringConversions.toCharBuffer(null, converter);
        assertNull(cb);
    }
}
