package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; we rely on its default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     *  List of package‑private static methods (treated as “public” for the test)
     * ---------------------------------------------------------------------- */
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
    // toPattern(Object, Converter)
    // toCurrency(Object, Converter)

    /* ----------------------------- Basic functionality tests ----------------------------- */

    @Test
    public void testAsString() {
        assertEquals("hello", StringConversions.asString("hello"));
        assertNull(StringConversions.asString(null));
        assertEquals("123", StringConversions.asString(123));
    }

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte("12", converter));
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf(Byte.MIN_VALUE), StringConversions.toByte(Byte.toString(Byte.MIN_VALUE), converter));
        // out‑of‑range but numeric – should be clamped via toLong and then exception
        try {
            StringConversions.toByte("200", converter);
            fail("Expected IllegalArgumentException for out‑of‑range byte");
        } catch (IllegalArgumentException ignored) {}
        // null input → NPE on cast? actually cast to String yields null → empty check returns 0
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte(null, converter));
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 1234), StringConversions.toShort("1234", converter));
        assertEquals(Short.valueOf((short)0), StringConversions.toShort("", converter));
        assertEquals(Short.valueOf(Short.MAX_VALUE), StringConversions.toShort(Short.toString(Short.MAX_VALUE), converter));
        try {
            StringConversions.toShort("40000", converter);
            fail("Expected IllegalArgumentException for out‑of‑range short");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), StringConversions.toInt(Integer.toString(Integer.MIN_VALUE), converter));
        try {
            StringConversions.toInt("9999999999", converter);
            fail("Expected IllegalArgumentException for out‑of‑range int");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
        assertEquals(Long.valueOf(Long.MIN_VALUE), StringConversions.toLong(Long.toString(Long.MIN_VALUE), converter));
        try {
            // value larger than Long.MAX_VALUE
            StringConversions.toLong("9223372036854775808", converter);
            fail("Expected IllegalArgumentException for out‑of‑range long");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(12.34f), StringConversions.toFloat("12.34", converter));
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
        try {
            StringConversions.toFloat("not-a-float", converter);
            fail("Expected IllegalArgumentException for non‑numeric float");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(12.3456), StringConversions.toDouble("12.3456", converter));
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
        try {
            StringConversions.toDouble("NaNx", converter);
            fail("Expected IllegalArgumentException for non‑numeric double");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToAtomicTypes() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());

        assertEquals(5, StringConversions.toAtomicInteger("5", converter).get());
        assertEquals(1234567890123L, StringConversions.toAtomicLong("1234567890123", converter).get());
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
        assertFalse(StringConversions.toBoolean(null, converter));
    }

    @Test
    public void testToCharacter() {
        // empty string → null char (0)
        assertEquals('\0', StringConversions.toCharacter("", converter));

        // single character
        assertEquals('Z', StringConversions.toCharacter("Z", converter));

        // numeric code point
        assertEquals('A', StringConversions.toCharacter("65", converter));

        // unicode escape
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));

        // invalid unicode escape
        try {
            StringConversions.toCharacter("\\uXYZ", converter);
            fail("Expected IllegalArgumentException for malformed unicode escape");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
        try {
            StringConversions.toBigInteger("not-a-number", converter);
            fail("Expected IllegalArgumentException for invalid BigInteger");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("12345.6789"), StringConversions.toBigDecimal("12345.6789", converter));
        try {
            StringConversions.toBigDecimal("abc", converter);
            fail("Expected IllegalArgumentException for invalid BigDecimal");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToURLAndURI() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());

        assertNull(StringConversions.toURL("", converter));

        URI uri = StringConversions.toURI("http://example.com/path", converter);
        assertEquals("/path", uri.getPath());

        assertNull(StringConversions.toURI("", converter));
    }

    @Test
    public void testEnumToString() {
        assertEquals("MONDAY", StringConversions.enumToString(DayOfWeek.MONDAY, converter));
    }

    @Test
    public void testToUUID() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
        try {
            StringConversions.toUUID("not-a-uuid", converter);
            fail("Expected IllegalArgumentException for malformed UUID");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToDuration() {
        // decimal seconds
        Duration d1 = StringConversions.toDuration("1.5", converter);
        assertEquals(Duration.ofSeconds(1, 500_000_000), d1);

        // ISO‑8601
        Duration d2 = StringConversions.toDuration("PT2H30M", converter);
        assertEquals(Duration.ofHours(2).plusMinutes(30), d2);

        // invalid format
        try {
            StringConversions.toDuration("not-a-duration", converter);
            fail("Expected IllegalArgumentException for invalid duration");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToClass() {
        assertEquals(String.class, StringConversions.toClass("java.lang.String", converter));
        try {
            StringConversions.toClass("non.existent.ClassName", converter);
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToMonthDay() {
        // ISO format
        assertEquals(MonthDay.of(12, 3), StringConversions.toMonthDay("2007-12-03", converter));

        // MM.DD pattern
        assertEquals(MonthDay.of(4, 5), StringConversions.toMonthDay("4.5", converter));

        // From ZonedDateTime parsing
        assertEquals(MonthDay.of(2, 14), StringConversions.toMonthDay("2023-02-14T10:15:30Z", converter));

        // Invalid
        try {
            StringConversions.toMonthDay("invalid", converter);
            fail("Expected IllegalArgumentException for malformed MonthDay");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToYearMonth() {
        assertEquals(YearMonth.of(2023, 5), StringConversions.toYearMonth("2023-05", converter));
        assertEquals(YearMonth.of(2022, 12), StringConversions.toYearMonth("2022-12-31T23:59:59Z", converter));
        try {
            StringConversions.toYearMonth("bad", converter);
            fail("Expected IllegalArgumentException for malformed YearMonth");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToPeriod() {
        assertEquals(Period.of(1, 2, 3), StringConversions.toPeriod("P1Y2M3D", converter));
        try {
            StringConversions.toPeriod("not-a-period", converter);
            fail("Expected IllegalArgumentException for malformed Period");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToSqlDate() {
        // Simple yyyy‑MM‑dd
        java.sql.Date d1 = StringConversions.toSqlDate("2023-05-01", converter);
        assertEquals(java.sql.Date.valueOf("2023-05-01"), d1);

        // ISO‑8601 with time zone
        java.sql.Date d2 = StringConversions.toSqlDate("2023-05-01T15:30:00Z", converter);
        assertEquals(java.sql.Date.valueOf("2023-05-01"), d2);
    }

    @Test
    public void testToTimestamp() {
        // Normal timestamp
        Timestamp ts = StringConversions.toTimestamp("2023-05-01T12:00:00Z", converter);
        assertEquals(Instant.parse("2023-05-01T12:00:00Z"), ts.toInstant());

        // Year before 0001 should throw
        try {
            StringConversions.toTimestamp("-0005-01-01T00:00:00Z", converter);
            fail("Expected IllegalArgumentException for year before 0001");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToTimeZone() {
        assertEquals(TimeZone.getTimeZone("UTC"), StringConversions.toTimeZone("UTC", converter));
        assertNull(StringConversions.toTimeZone("", converter));
        assertNull(StringConversions.toTimeZone(null, converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = StringConversions.toCalendar("2023-05-01T12:00:00Z", converter);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));

        // Year less than 2 should throw
        try {
            StringConversions.toCalendar("0001-01-01T00:00:00Z", converter);
            fail("Expected IllegalArgumentException for year < 2");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToLocalDateAndDateTime() {
        LocalDate ld = StringConversions.toLocalDate("2023-05-01T12:00:00Z", converter);
        assertEquals(LocalDate.of(2023, 5, 1), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime("2023-05-01T12:34:56Z", converter);
        assertEquals(LocalDateTime.of(2023, 5, 1, 12, 34, 56), ldt);
    }

    @Test
    public void testToLocalTime() {
        // Direct parse
        LocalTime lt1 = StringConversions.toLocalTime("12:34:56", converter);
        assertEquals(LocalTime.of(12, 34, 56), lt1);

        // Fallback via ZonedDateTime
        LocalTime lt2 = StringConversions.toLocalTime("2023-05-01T07:15:00-05:00", converter);
        assertEquals(LocalTime.of(7, 15, 0), lt2);
    }

    @Test
    public void testToLocale() {
        assertNull(StringConversions.toLocale("", converter));
        Locale locale = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.forLanguageTag("en-US"), locale);
    }

    @Test
    public void testToZoneId() {
        ZoneId zid1 = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid1);

        // Fallback to TimeZone when ZoneId.of fails
        ZoneId zid2 = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zid2);

        try {
            StringConversions.toZoneId("Invalid/Zone", converter);
            fail("Expected IllegalArgumentException for unknown zone");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToZoneOffset() {
        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);

        try {
            StringConversions.toZoneOffset("invalid", converter);
            fail("Expected IllegalArgumentException for malformed offset");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToOffsetDateTimeAndOffsetTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2023-05-01T12:30:00+02:00", converter);
        assertEquals(OffsetDateTime.of(2023,5,1,12,30,0,0, ZoneOffset.ofHours(2)), odt);

        OffsetTime ot1 = StringConversions.toOffsetTime("12:30:45+02:00", converter);
        assertEquals(OffsetTime.of(12,30,45,0, ZoneOffset.ofHours(2)), ot1);

        // fallback via OffsetDateTime
        OffsetTime ot2 = StringConversions.toOffsetTime("2023-05-01T12:30:45+02:00", converter);
        assertEquals(OffsetTime.of(12,30,45,0, ZoneOffset.ofHours(2)), ot2);
    }

    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2023-05-01T12:00:00Z", converter);
        assertEquals(Instant.parse("2023-05-01T12:00:00Z"), instant);
    }

    @Test
    public void testCharArrayConversions() {
        char[] chars = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a','b','c'}, chars);

        char[] empty = StringConversions.toCharArray("", converter);
        assertArrayEquals(new char[0], empty);
    }

    @Test
    public void testCharacterArrayConversions() {
        Character[] characters = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[]{'x','y','z'}, characters);
    }

    @Test
    public void testCharBuffer() {
        CharBuffer buf = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", buf.toString());
    }

    @Test
    public void testByteArrayAndByteBuffer() {
        byte[] bytes = StringConversions.toByteArray("ABC", converter);
        assertArrayEquals(new byte[]{'A','B','C'}, bytes);

        ByteBuffer bb = StringConversions.toByteBuffer("ABC", converter);
        assertArrayEquals(new byte[]{'A','B','C'}, bb.array());

        // null or empty yields empty arrays
        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
        assertArrayEquals(new byte[0], StringConversions.toByteArray(null, converter));
    }

    @Test
    public void testToStringAndBuilders() {
        assertEquals("test", StringConversions.toString("test", converter));
        assertNull(StringConversions.toString(null, converter));

        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));
        assertNull(StringConversions.toStringBuffer(null, converter));

        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    @Test
    public void testToYear() {
        Year y1 = StringConversions.toYear("2023", converter);
        assertEquals(Year.of(2023), y1);

        // fallback via ZonedDateTime
        Year y2 = StringConversions.toYear("2023-05-01T00:00:00Z", converter);
        assertEquals(Year.of(2023), y2);

        try {
            StringConversions.toYear("bad-year", converter);
            fail("Expected IllegalArgumentException for malformed year");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToPattern() {
        Pattern p = StringConversions.toPattern("\\d+", converter);
        assertTrue(p.matcher("12345").matches());
        assertFalse(p.matcher("abc").matches());
    }

    @Test
    public void testToCurrency() {
        Currency usd = StringConversions.toCurrency("USD", converter);
        assertEquals(Currency.getInstance("USD"), usd);
    }
}
