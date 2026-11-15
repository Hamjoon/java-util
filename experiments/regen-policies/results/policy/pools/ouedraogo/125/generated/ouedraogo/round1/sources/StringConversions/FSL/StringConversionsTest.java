package com.cedarsoftware.util.convert;

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

import org.junit.BeforeClass;
import org.junit.Test;

public class StringConversionsTest {

    private static Converter converter;

    @BeforeClass
    public static void setUp() {
        // ConverterOptions is part of the library – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------- asString ---------- */
    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    /* ---------- toByte ---------- */
    @Test
    public void testToByteValid() {
        assertEquals(Byte.valueOf((byte) 5), StringConversions.toByte("5", converter));
        assertEquals(Byte.valueOf(Byte.MAX_VALUE), StringConversions.toByte(String.valueOf(Byte.MAX_VALUE), converter));
    }

    @Test
    public void testToByteEmpty() {
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte("", converter));
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte(null, converter));
    }

    @Test
    public void testToByteOutOfRange() {
        try {
            StringConversions.toByte("200", converter);
            fail("Expected IllegalArgumentException for out‑of‑range byte");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toShort ---------- */
    @Test
    public void testToShortValid() {
        assertEquals(Short.valueOf((short) 1234), StringConversions.toShort("1234", converter));
    }

    @Test
    public void testToShortEmpty() {
        assertEquals(Short.valueOf((short)0), StringConversions.toShort("", converter));
    }

    @Test
    public void testToShortOutOfRange() {
        try {
            StringConversions.toShort(String.valueOf(Short.MAX_VALUE + 1), converter);
            fail("Expected IllegalArgumentException for out‑of‑range short");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toInt ---------- */
    @Test
    public void testToIntValid() {
        assertEquals(Integer.valueOf(42), StringConversions.toInt("42", converter));
    }

    @Test
    public void testToIntEmpty() {
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));
    }

    @Test
    public void testToIntOutOfRange() {
        try {
            StringConversions.toInt(String.valueOf((long) Integer.MAX_VALUE + 1), converter);
            fail("Expected IllegalArgumentException for out‑of‑range int");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toLong ---------- */
    @Test
    public void testToLongValid() {
        assertEquals(Long.valueOf(123456789L), StringConversions.toLong("123456789", converter));
    }

    @Test
    public void testToLongEmpty() {
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test
    public void testToLongOutOfRange() {
        try {
            // Long.MAX_VALUE + 1 cannot be expressed as a literal; use a string that exceeds the range
            String tooBig = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE).toPlainString();
            StringConversions.toLong(tooBig, converter);
            fail("Expected IllegalArgumentException for out‑of‑range long");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toFloat & toDouble ---------- */
    @Test
    public void testToFloatValid() {
        assertEquals(Float.valueOf(3.14f), StringConversions.toFloat("3.14", converter));
    }

    @Test
    public void testToFloatEmpty() {
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));
    }

    @Test
    public void testToFloatInvalid() {
        try {
            StringConversions.toFloat("not-a-float", converter);
            fail("Expected IllegalArgumentException for invalid float");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testToDoubleValid() {
        assertEquals(Double.valueOf(2.71828), StringConversions.toDouble("2.71828", converter));
    }

    @Test
    public void testToDoubleEmpty() {
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    @Test
    public void testToDoubleInvalid() {
        try {
            StringConversions.toDouble("NaNish", converter);
            fail("Expected IllegalArgumentException for invalid double");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- Atomic types ---------- */
    @Test
    public void testAtomicConversions() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());

        assertEquals(7, StringConversions.toAtomicInteger("7", converter).get());
        assertEquals(123456789L, StringConversions.toAtomicLong("123456789", converter).get());
    }

    /* ---------- toBoolean ---------- */
    @Test
    public void testToBooleanVarious() {
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

    /* ---------- toCharacter ---------- */
    @Test
    public void testToCharacterSimple() {
        assertEquals('a', StringConversions.toCharacter("a", converter));
        assertEquals('\0', StringConversions.toCharacter("", converter));
    }

    @Test
    public void testToCharacterFromDigits() {
        assertEquals('A', StringConversions.toCharacter("65", converter)); // 65 -> 'A'
    }

    @Test
    public void testToCharacterUnicodeEscape() {
        assertEquals('Ā', StringConversions.toCharacter("\\u0100", converter));
    }

    @Test
    public void testToCharacterInvalidUnicode() {
        try {
            StringConversions.toCharacter("\\u0G00", converter);
            fail("Expected IllegalArgumentException for malformed unicode");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testToCharacterNonDigitNonUnicode() {
        try {
            StringConversions.toCharacter("ab", converter);
            fail("Expected IllegalArgumentException for non‑digit non‑unicode string");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toBigInteger & toBigDecimal ---------- */
    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.valueOf(12345L), StringConversions.toBigInteger("12345", converter));
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(new BigDecimal("12.34"), StringConversions.toBigDecimal("12.34", converter));
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    /* ---------- toURL & toURI ---------- */
    @Test
    public void testToURL() throws Exception {
        URL url = StringConversions.toURL("http://example.com", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    public void testToURLInvalid() {
        try {
            StringConversions.toURL("ht!tp://bad_url", converter);
            fail("Expected IllegalArgumentException for malformed URL");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testToURI() {
        URI uri = StringConversions.toURI("urn:test:123", converter);
        assertEquals("urn", uri.getScheme());
        assertEquals("test:123", uri.getSchemeSpecificPart());
    }

    /* ---------- enumToString ---------- */
    private enum SampleEnum { ONE, TWO }

    @Test
    public void testEnumToString() {
        assertEquals("ONE", StringConversions.enumToString(SampleEnum.ONE, converter));
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    /* ---------- toUUID ---------- */
    @Test
    public void testToUUID() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test
    public void testToUUIDInvalid() {
        try {
            StringConversions.toUUID("not-a-uuid", converter);
            fail("Expected IllegalArgumentException for invalid UUID");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toDuration ---------- */
    @Test
    public void testToDuration() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofMillis(20345), d);
    }

    @Test
    public void testToDurationInvalid() {
        try {
            StringConversions.toDuration("20 seconds", converter);
            fail("Expected IllegalArgumentException for bad duration");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toClass ---------- */
    @Test
    public void testToClass() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test
    public void testToClassNotFound() {
        try {
            StringConversions.toClass("com.nonexistent.Foo", converter);
            fail("Expected IllegalArgumentException for missing class");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toMonthDay ---------- */
    @Test
    public void testToMonthDayIso() {
        MonthDay md = StringConversions.toMonthDay("--12-25", converter); // ISO format with leading --
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToMonthDayPattern() {
        MonthDay md = StringConversions.toMonthDay("3.14", converter); // MM.DD pattern
        assertEquals(MonthDay.of(3, 14), md);
    }

    @Test
    public void testToMonthDayFromZonedDateTime() {
        // DateUtilities parses ISO date‑time strings; use a known format.
        MonthDay md = StringConversions.toMonthDay("2022-07-04T10:15:30Z", converter);
        assertEquals(MonthDay.of(7, 4), md);
    }

    @Test
    public void testToMonthDayInvalid() {
        try {
            StringConversions.toMonthDay("invalid", converter);
            fail("Expected IllegalArgumentException for invalid MonthDay");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toYearMonth ---------- */
    @Test
    public void testToYearMonthIso() {
        YearMonth ym = StringConversions.toYearMonth("2023-08", converter);
        assertEquals(YearMonth.of(2023, 8), ym);
    }

    @Test
    public void testToYearMonthFromZonedDateTime() {
        YearMonth ym = StringConversions.toYearMonth("2020-12-31T23:59:59Z", converter);
        assertEquals(YearMonth.of(2020, 12), ym);
    }

    @Test
    public void testToYearMonthInvalid() {
        try {
            StringConversions.toYearMonth("not-a-year-month", converter);
            fail("Expected IllegalArgumentException for invalid YearMonth");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toPeriod ---------- */
    @Test
    public void testToPeriod() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test
    public void testToPeriodInvalid() {
        try {
            StringConversions.toPeriod("2 years", converter);
            fail("Expected IllegalArgumentException for invalid Period");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toDate / toSqlDate / toTimestamp ---------- */
    @Test
    public void testDateConversions() {
        // Use an ISO instant string.
        String iso = "2021-04-05T12:30:45Z";
        Date date = StringConversions.toDate(iso, converter);
        assertNotNull(date);
        assertEquals(Instant.parse(iso), date.toInstant());

        java.sql.Date sqlDate = StringConversions.toSqlDate(iso, converter);
        assertEquals(Date.from(Instant.parse(iso)), sqlDate);

        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertEquals(Instant.parse(iso).toEpochMilli(), ts.getTime());
    }

    @Test
    public void testDateConversionsNull() {
        assertNull(StringConversions.toDate("", converter));
        assertNull(StringConversions.toSqlDate("", converter));
        assertNull(StringConversions.toTimestamp("", converter));
    }

    /* ---------- toTimeZone ---------- */
    @Test
    public void testToTimeZone() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals("UTC", tz.getID());

        assertNull(StringConversions.toTimeZone("   ", converter));
    }

    /* ---------- toCalendar ---------- */
    @Test
    public void testToCalendar() {
        Calendar cal = StringConversions.toCalendar("2022-01-01T00:00:00Z", converter);
        assertNotNull(cal);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
    }

    /* ---------- toLocalDate / toLocalDateTime ---------- */
    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2020-02-29T00:00:00Z", converter);
        assertEquals(LocalDate.of(2020, 2, 29), ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2020-02-29T15:45:30Z", converter);
        assertEquals(LocalDateTime.of(2020, 2, 29, 15, 45, 30), ldt);
    }

    /* ---------- toLocalTime ---------- */
    @Test
    public void testToLocalTimeDirect() {
        LocalTime lt = StringConversions.toLocalTime("10:15:30", converter);
        assertEquals(LocalTime.of(10, 15, 30), lt);
    }

    @Test
    public void testToLocalTimeFromZoned() {
        LocalTime lt = StringConversions.toLocalTime("2021-03-04T22:30:00Z", converter);
        assertEquals(LocalTime.of(22, 30), lt);
    }

    /* ---------- toLocale ---------- */
    @Test
    public void testToLocale() {
        Locale loc = StringConversions.toLocale("fr-FR", converter);
        assertEquals(Locale.forLanguageTag("fr-FR"), loc);
        assertNull(StringConversions.toLocale("", converter));
    }

    /* ---------- toZoneId ---------- */
    @Test
    public void testToZoneIdValid() {
        ZoneId zid = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void testToZoneIdFallback() {
        // "GMT+2" is not a standard ZoneId but is accepted by TimeZone.
        ZoneId zid = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zid);
    }

    @Test
    public void testToZoneIdInvalid() {
        try {
            StringConversions.toZoneId("NoSuch/Zone", converter);
            fail("Expected IllegalArgumentException for unknown zone");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toZoneOffset ---------- */
    @Test
    public void testToZoneOffsetValid() {
        ZoneOffset zo = StringConversions.toZoneOffset("-05:00", converter);
        assertEquals(ZoneOffset.of("-05:00"), zo);
    }

    @Test
    public void testToZoneOffsetInvalid() {
        try {
            StringConversions.toZoneOffset("invalid", converter);
            fail("Expected IllegalArgumentException for bad offset");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toOffsetDateTime & toOffsetTime ---------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2022-10-12T08:30:00+02:00", converter);
        assertEquals(OffsetDateTime.of(2022, 10, 12, 8, 30, 0, 0, ZoneOffset.ofHours(2)), odt);
    }

    @Test
    public void testToOffsetTimeDirect() {
        OffsetTime ot = StringConversions.toOffsetTime("13:45:00+01:00", converter);
        assertEquals(OffsetTime.of(13, 45, 0, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test
    public void testToOffsetTimeFromOffsetDateTime() {
        // Provide a full date‑time; the method should extract the time part.
        OffsetTime ot = StringConversions.toOffsetTime("2022-01-01T09:15:00+03:00", converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(3)), ot);
    }

    @Test
    public void testToOffsetTimeInvalid() {
        try {
            StringConversions.toOffsetTime("not-a-time", converter);
            fail("Expected IllegalArgumentException for invalid OffsetTime");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- toInstant ---------- */
    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2022-12-31T23:59:59Z", converter);
        assertEquals(Instant.parse("2022-12-31T23:59:59Z"), instant);
    }

    /* ---------- toCharArray & toCharacterArray ---------- */
    @Test
    public void testToCharArray() {
        char[] expected = new char[] { 'h', 'i' };
        assertArrayEquals(expected, StringConversions.toCharArray("hi", converter));
        assertArrayEquals(new char[0], StringConversions.toCharArray("", converter));
    }

    @Test
    public void testToCharacterArray() {
        Character[] expected = new Character[] { 'J', 'a', 'v', 'a' };
        assertArrayEquals(expected, StringConversions.toCharacterArray("Java", converter));
    }

    /* ---------- toCharBuffer ---------- */
    @Test
    public void testToCharBuffer() {
        CharBuffer buf = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", buf.toString());
    }

    /* ---------- toByteArray & toByteBuffer ---------- */
    @Test
    public void testToByteArray() throws Exception {
        byte[] bytes = StringConversions.toByteArray("abc", converter);
        assertArrayEquals("abc".getBytes(converter.getOptions().getCharset()), bytes);

        assertArrayEquals(new byte[0], StringConversions.toByteArray("", converter));
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("xyz", converter);
        assertEquals("xyz", new String(bb.array(), converter.getOptions().getCharset()));
    }

    /* ---------- toString, toStringBuffer, toStringBuilder ---------- */
    @Test
    public void testToStringVariants() {
        assertEquals("test", StringConversions.toString("test", converter));
        assertNull(StringConversions.toString(null, converter));

        StringBuffer sb = StringConversions.toStringBuffer("buf", converter);
        assertEquals("buf", sb.toString());

        StringBuilder sbd = StringConversions.toStringBuilder("bld", converter);
        assertEquals("bld", sbd.toString());

        assertNull(StringConversions.toStringBuffer(null, converter));
        assertNull(StringConversions.toStringBuilder(null, converter));
    }

    /* ---------- toYear ---------- */
    @Test
    public void testToYearDirect() {
        Year y = StringConversions.toYear("1999", converter);
        assertEquals(Year.of(1999), y);
    }

    @Test
    public void testToYearFromZonedDateTime() {
        Year y = StringConversions.toYear("2021-06-15T00:00:00Z", converter);
        assertEquals(Year.of(2021), y);
    }

    @Test
    public void testToYearInvalid() {
        try {
            StringConversions.toYear("not-a-year", converter);
            fail("Expected IllegalArgumentException for invalid Year");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
