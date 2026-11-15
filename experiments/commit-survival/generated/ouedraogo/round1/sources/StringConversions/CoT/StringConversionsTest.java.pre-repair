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
import java.time.temporal.ChronoField;
import java.util.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    /**
     * Minimal stub of {@link ConverterOptions} that provides the defaults required by the
     * conversion methods (charset and zoneId).  The real library supplies this class; for the
     * purpose of the tests we only need a concrete instance with sensible defaults.
     */
    private static class SimpleConverterOptions extends ConverterOptions {
        SimpleConverterOptions() {
            super();
            // charset defaults to UTF‑8 and zoneId defaults to UTC in the real implementation.
            // The setters are protected in the real class; we assume the default constructor
            // already sets these values appropriately.
        }
    }

    @Before
    public void setUp() {
        // The real Converter requires a ConverterOptions instance.
        converter = new Converter(new SimpleConverterOptions());
    }

    /* -------------------------------------------------
     *  List of public static methods (signatures)
     * ------------------------------------------------- */
    // asString(Object)
    // toByte(Object, Converter)
    // toShort(Object, Converter)
    // toInt(Object, Converter)
    // toLong(Object, Converter)
    // toFloat(Object, Converter)
    // toDouble(Object, Converter)
    // toAtomicBoolean(Object, Converter)
    // toAtomicInteger(Object, Converter)
    // toAtomicLong(Object, Converter)
    // toBoolean(Object, Converter)
    // toCharacter(Object, Converter)
    // toBigInteger(Object, Converter)
    // toBigDecimal(Object, Converter)
    // toURL(Object, Converter)
    // toURI(Object, Converter)
    // enumToString(Object, Converter)
    // toUUID(Object, Converter)
    // toDuration(Object, Converter)
    // toClass(Object, Converter)
    // toMonthDay(Object, Converter)
    // toYearMonth(Object, Converter)
    // toPeriod(Object, Converter)
    // toDate(Object, Converter)
    // toSqlDate(Object, Converter)
    // toTimestamp(Object, Converter)
    // toTimeZone(Object, Converter)
    // toCalendar(Object, Converter)
    // toLocalDate(Object, Converter)
    // toLocalDateTime(Object, Converter)
    // toLocalTime(Object, Converter)
    // toLocale(Object, Converter)
    // toZonedDateTime(Object, Converter)
    // toZoneId(Object, Converter)
    // toZoneOffset(Object, Converter)
    // toOffsetDateTime(Object, Converter)
    // toOffsetTime(Object, Converter)
    // toInstant(Object, Converter)
    // toCharArray(Object, Converter)
    // toCharacterArray(Object, Converter)
    // toCharBuffer(Object, Converter)
    // toByteArray(Object, Converter)
    // toByteBuffer(Object, Converter)
    // toString(Object, Converter)
    // toStringBuffer(Object, Converter)
    // toStringBuilder(Object, Converter)
    // toYear(Object, Converter)

    /* -------------------------------------------------
     *  Basic functional tests
     * ------------------------------------------------- */

    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    @Test
    public void testToByte_NormalAndEmpty() {
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte("12", converter));
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByte_OutOfRange() {
        // 200 is outside Byte.MAX_VALUE (127)
        StringConversions.toByte("200", converter);
    }

    @Test
    public void testToShort_NormalAndEmpty() {
        assertEquals(Short.valueOf((short) 32000), StringConversions.toShort("32000", converter));
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort_OutOfRange() {
        // 40000 exceeds Short.MAX_VALUE (32767)
        StringConversions.toShort("40000", converter);
    }

    @Test
    public void testToInt_NormalAndEmpty() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_OutOfRange() {
        // value larger than Integer.MAX_VALUE
        StringConversions.toInt("3000000000", converter);
    }

    @Test
    public void testToLong_NormalAndEmpty() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong_Invalid() {
        StringConversions.toLong("not-a-number", converter);
    }

    @Test
    public void testToFloat_NormalAndEmpty() {
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFloat_Invalid() {
        StringConversions.toFloat("abc", converter);
    }

    @Test
    public void testToDouble_NormalAndEmpty() {
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDouble_Invalid() {
        StringConversions.toDouble("xyz", converter);
    }

    @Test
    public void testAtomicConversions() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());

        assertEquals(42, StringConversions.toAtomicInteger("42", converter).get());
        assertEquals(1234567890123L, StringConversions.toAtomicLong("1234567890123", converter).get());
    }

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
        assertFalse(StringConversions.toBoolean("no", converter));
        assertFalse(StringConversions.toBoolean("0", converter));
    }

    @Test
    public void testToCharacter_EmptySingleNumericUnicode() {
        // empty string -> null char (0)
        assertEquals((char) 0, StringConversions.toCharacter("", converter));

        // single character
        assertEquals('Z', StringConversions.toCharacter("Z", converter));

        // numeric character code
        assertEquals('A', StringConversions.toCharacter("65", converter));

        // unicode escape
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_InvalidUnicode() {
        StringConversions.toCharacter("\\u123", converter); // too short
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.valueOf(1234567890123456789L), StringConversions.toBigInteger("1234567890123456789", converter));
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigInteger_Invalid() {
        StringConversions.toBigInteger("not-a-number", converter);
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(new BigDecimal("3.14159"), StringConversions.toBigDecimal("3.14159", converter));
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBigDecimal_Invalid() {
        StringConversions.toBigDecimal("abc", converter);
    }

    @Test
    public void testToURL_ValidAndEmpty() throws Exception {
        URL url = StringConversions.toURL("https://example.com/path?query=1", converter);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());

        assertNull(StringConversions.toURL("", converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURL_Invalid() {
        StringConversions.toURL("ht!tp://bad url", converter);
    }

    @Test
    public void testToURI_ValidAndEmpty() {
        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());

        assertNull(StringConversions.toURI("", converter));
    }

    private enum SampleEnum { VALUE }

    @Test
    public void testEnumToString() {
        assertEquals("VALUE", StringConversions.enumToString(SampleEnum.VALUE, converter));
    }

    @Test
    public void testToUUID_ValidAndInvalid() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_Invalid() {
        StringConversions.toUUID("not-a-uuid", converter);
    }

    @Test
    public void testToDuration_ValidAndInvalid() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(20345, d.toMillis());

        try {
            StringConversions.toDuration("not-a-duration", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testToClass_ValidAndInvalid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);

        try {
            StringConversions.toClass("non.existent.ClassName", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testToMonthDay_StandardAndPattern() {
        // ISO format --MM-DD
        MonthDay md1 = StringConversions.toMonthDay("--04-05", converter);
        assertEquals(MonthDay.of(4, 5), md1);

        // custom pattern MM.DD
        MonthDay md2 = StringConversions.toMonthDay("4.5", converter);
        assertEquals(MonthDay.of(4, 5), md2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_Invalid() {
        StringConversions.toMonthDay("invalid", converter);
    }

    @Test
    public void testToYearMonth_StandardAndFallback() {
        YearMonth ym1 = StringConversions.toYearMonth("2023-07", converter);
        assertEquals(YearMonth.of(2023, 7), ym1);

        // fallback using a full date string
        YearMonth ym2 = StringConversions.toYearMonth("2023-07-15T12:00:00Z", converter);
        assertEquals(YearMonth.of(2023, 7), ym2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_Invalid() {
        StringConversions.toYearMonth("bad-year-month", converter);
    }

    @Test
    public void testToPeriod_ValidAndInvalid() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);

        try {
            StringConversions.toPeriod("not-a-period", converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testDateConversions() {
        // ISO instant
        String iso = "2023-04-05T12:30:15Z";

        Date date = StringConversions.toDate(iso, converter);
        assertNotNull(date);
        assertEquals(Instant.parse(iso), date.toInstant());

        java.sql.Date sqlDate = StringConversions.toSqlDate(iso, converter);
        assertEquals(Date.from(Instant.parse(iso)), sqlDate);

        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertEquals(Instant.parse(iso).toEpochMilli(), ts.getTime());
    }

    @Test
    public void testTimeZoneAndCalendar() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals("UTC", tz.getID());

        Calendar cal = StringConversions.toCalendar("2023-04-05T12:00:00Z", converter);
        assertNotNull(cal);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(4, cal.get(Calendar.MONTH) + 1); // MONTH is zero‑based
    }

    @Test
    public void testLocalDateAndTimeConversions() {
        String iso = "2023-04-05T14:20:30Z";

        LocalDate ld = StringConversions.toLocalDate(iso, converter);
        assertEquals(LocalDate.of(2023, 4, 5), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime(iso, converter);
        assertEquals(LocalDateTime.of(2023, 4, 5, 14, 20, 30), ldt);

        LocalTime lt = StringConversions.toLocalTime("15:45:10", converter);
        assertEquals(LocalTime.of(15, 45, 10), lt);

        // fallback from full date‑time string
        LocalTime lt2 = StringConversions.toLocalTime(iso, converter);
        assertEquals(LocalTime.of(14, 20, 30), lt2);
    }

    @Test
    public void testToLocale() {
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), loc);

        assertNull(StringConversions.toLocale("", converter));
    }

    @Test
    public void testZonedAndOffsetConversions() {
        String iso = "2023-04-05T12:00:00+02:00";

        ZonedDateTime zdt = StringConversions.toZonedDateTime(iso, converter);
        assertEquals(OffsetDateTime.parse(iso).toZonedDateTime(), zdt);

        ZoneId zid = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid);

        // unknown zone falls back to TimeZone; GMT triggers exception
        try {
            StringConversions.toZoneId("DoesNotExist/Zone", converter);
            fail("Expected IllegalArgumentException for unknown zone");
        } catch (IllegalArgumentException ignored) {
        }

        ZoneOffset zo = StringConversions.toZoneOffset("-05:00", converter);
        assertEquals(ZoneOffset.of("-05:00"), zo);

        try {
            StringConversions.toZoneOffset("invalid", converter);
            fail("Expected IllegalArgumentException for invalid offset");
        } catch (IllegalArgumentException ignored) {
        }

        OffsetDateTime odt = StringConversions.toOffsetDateTime(iso, converter);
        assertEquals(OffsetDateTime.parse(iso), odt);

        OffsetTime ot = StringConversions.toOffsetTime("12:34:56+02:00", converter);
        assertEquals(OffsetTime.parse("12:34:56+02:00"), ot);

        // fallback: parse a ZonedDateTime and then extract OffsetTime
        OffsetTime ot2 = StringConversions.toOffsetTime(iso, converter);
        assertEquals(OffsetTime.parse("12:00:00+02:00"), ot2);
    }

    @Test
    public void testInstantConversion() {
        String iso = "2023-04-05T12:00:00Z";
        Instant instant = StringConversions.toInstant(iso, converter);
        assertEquals(Instant.parse(iso), instant);
    }

    @Test
    public void testCharArrayAndBufferConversions() {
        char[] chars = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[] {'a', 'b', 'c'}, chars);

        Character[] charObjs = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[] {'x', 'y', 'z'}, charObjs);

        CharBuffer buffer = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", buffer.toString());

        byte[] bytes = StringConversions.toByteArray("AB", converter);
        assertArrayEquals(new byte[] {'A', 'B'}, bytes);

        ByteBuffer byteBuf = StringConversions.toByteBuffer("CD", converter);
        assertEquals('C', (char) byteBuf.get());
        assertEquals('D', (char) byteBuf.get());
    }

    @Test
    public void testStringRepresentations() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("test", StringConversions.toString("test", converter));

        assertNull(StringConversions.toStringBuffer(null, converter));
        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));

        assertNull(StringConversions.toStringBuilder(null, converter));
        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
    }

    @Test
    public void testToYear_StandardAndFallback() {
        Year y1 = StringConversions.toYear("2023", converter);
        assertEquals(Year.of(2023), y1);

        // fallback using a full datetime
        Year y2 = StringConversions.toYear("2023-08-15T00:00Z", converter);
        assertEquals(Year.of(2023), y2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYear_Invalid() {
        StringConversions.toYear("not-a-year", converter);
    }

    /* -------------------------------------------------
     *  Edge‑case / exception tests not covered above
     * ------------------------------------------------- */

    @Test
    public void testToByte_EmptyStringReturnsZero() {
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
    }

    @Test
    public void testToShort_EmptyStringReturnsZero() {
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));
    }

    @Test
    public void testToInt_EmptyStringReturnsZero() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test
    public void testToLong_EmptyStringReturnsZero() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test
    public void testToFloat_EmptyStringReturnsZero() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test
    public void testToDouble_EmptyStringReturnsZero() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test
    public void testToCharArray_NullOrEmpty() {
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray(null, converter));
    }

    @Test
    public void testToByteArray_NullOrEmpty() {
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }
}
