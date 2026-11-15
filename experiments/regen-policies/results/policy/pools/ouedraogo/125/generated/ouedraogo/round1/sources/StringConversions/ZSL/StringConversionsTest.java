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
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class StringConversionsTest {

    private Converter converter;

    /** a simple enum for enumToString test */
    private enum TestEnum { VALUE1, VALUE2 }

    @Before
    public void setUp() {
        // ConverterOptions is assumed to have a public no‑arg constructor.
        // The default options provide a charset (UTF‑8) and a ZoneId (UTC).
        ConverterOptions options = new ConverterOptions();
        this.converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  asString                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("123", StringConversions.asString(123));
        assertEquals("true", StringConversions.asString(true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive numeric conversions                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToByte_NormalAndDecimal() {
        assertEquals(Byte.valueOf((byte) 5), StringConversions.toByte("5", converter));

        // decimal string – should be rounded down
        assertEquals(Byte.valueOf((byte) 12), StringConversions.toByte("12.9", converter));

        // empty string → zero
        assertEquals(Byte.valueOf((byte) 0), StringConversions.toByte("", converter));

        // out‑of‑range should throw IllegalArgumentException
        try {
            StringConversions.toByte("200", converter);
            fail("Expected IllegalArgumentException for out‑of‑range byte");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testToShort_NormalAndDecimal() {
        assertEquals(Short.valueOf((short) 1234), StringConversions.toShort("1234", converter));
        assertEquals(Short.valueOf((short) 12), StringConversions.toShort("12.9", converter));
        assertEquals(Short.valueOf((short) 0), StringConversions.toShort("", converter));

        try {
            StringConversions.toShort("40000", converter); // > Short.MAX_VALUE
            fail("Expected IllegalArgumentException for out‑of‑range short");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testToInt_NormalAndDecimal() {
        assertEquals(Integer.valueOf(123456), StringConversions.toInt("123456", converter));
        assertEquals(Integer.valueOf(12), StringConversions.toInt("12.9", converter));
        assertEquals(Integer.valueOf(0), StringConversions.toInt("", converter));

        try {
            StringConversions.toInt("3000000000", converter); // > Integer.MAX_VALUE
            fail("Expected IllegalArgumentException for out‑of‑range int");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testToLong_NormalAndDecimal() {
        assertEquals(Long.valueOf(1234567890123L), StringConversions.toLong("1234567890123", converter));
        assertEquals(Long.valueOf(123L), StringConversions.toLong("123.999", converter)); // rounded down
        assertEquals(Long.valueOf(0L), StringConversions.toLong("", converter));
    }

    @Test
    public void testToFloatAndDouble() {
        assertEquals(Float.valueOf(1.23f), StringConversions.toFloat("1.23", converter));
        assertEquals(Float.valueOf(0f), StringConversions.toFloat("", converter));

        assertEquals(Double.valueOf(4.56), StringConversions.toDouble("4.56", converter));
        assertEquals(Double.valueOf(0.0), StringConversions.toDouble("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Atomic wrappers                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean("true", converter);
        assertTrue(ab.get());

        AtomicInteger ai = StringConversions.toAtomicInteger("42", converter);
        assertEquals(42, ai.get());

        AtomicLong al = StringConversions.toAtomicLong("123456789", converter);
        assertEquals(123456789L, al.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean conversion                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBooleanVariations() {
        assertTrue(StringConversions.toBoolean("true", converter));
        assertFalse(StringConversions.toBoolean("false", converter));
        assertTrue(StringConversions.toBoolean("TRUE", converter));
        assertTrue(StringConversions.toBoolean("t", converter));
        assertTrue(StringConversions.toBoolean("1", converter));
        assertTrue(StringConversions.toBoolean("y", converter));
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Character conversion                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCharacter() {
        // empty string → null char (0)
        assertEquals((char)0, StringConversions.toCharacter("", converter));

        // single character
        assertEquals('Z', StringConversions.toCharacter("Z", converter));

        // all‑digit string → numeric code point
        assertEquals('A', StringConversions.toCharacter("65", converter));

        // unicode escape
        assertEquals('Ω', StringConversions.toCharacter("\\u03A9", converter));

        // invalid unicode escape should throw
        try {
            StringConversions.toCharacter("\\u123", converter);
            fail("Expected IllegalArgumentException for malformed unicode escape");
        } catch (IllegalArgumentException ignored) { }
    }

    /* ---------------------------------------------------------------------- */
    /*  BigInteger / BigDecimal                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testBigIntegerAndDecimal() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));

        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
        assertEquals(new BigDecimal("3.14159"), StringConversions.toBigDecimal("3.14159", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  URL / URI                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToURLAndURI() throws Exception {
        URL url = StringConversions.toURL("https://example.com/path?query=1", converter);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());

        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());
        assertEquals("user@example.com", uri.getSchemeSpecificPart());
    }

    /* ---------------------------------------------------------------------- */
    /*  Enum to String                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEnumToString() {
        assertEquals("VALUE1", StringConversions.enumToString(TestEnum.VALUE1, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  UUID                                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToUUID() {
        UUID expected = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertEquals(expected, StringConversions.toUUID("123e4567-e89b-12d3-a456-426655440000", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  Duration                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDuration() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofMillis(20345), d);
    }

    /* ---------------------------------------------------------------------- */
    /*  Class conversion                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToClass() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    /* ---------------------------------------------------------------------- */
    /*  MonthDay                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToMonthDay() {
        // ISO format
        MonthDay md1 = StringConversions.toMonthDay("2000-02-29", converter);
        assertEquals(MonthDay.of(2, 29), md1);

        // MM.DD pattern
        MonthDay md2 = StringConversions.toMonthDay("12.31", converter);
        assertEquals(MonthDay.of(12, 31), md2);

        // ZonedDateTime fallback
        MonthDay md3 = StringConversions.toMonthDay("2020-03-05T10:15:30Z", converter);
        assertEquals(MonthDay.of(3, 5), md3);
    }

    /* ---------------------------------------------------------------------- */
    /*  YearMonth                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToYearMonth() {
        YearMonth ym1 = StringConversions.toYearMonth("2021-07", converter);
        assertEquals(YearMonth.of(2021, 7), ym1);

        // fallback via ZonedDateTime
        YearMonth ym2 = StringConversions.toYearMonth("2020-12-25T00:00:00Z", converter);
        assertEquals(YearMonth.of(2020, 12), ym2);
    }

    /* ---------------------------------------------------------------------- */
    /*  Period                                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToPeriod() {
        Period p = StringConversions.toPeriod("P2Y3M5D", converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    /* ---------------------------------------------------------------------- */
    /*  Date / SqlDate / Timestamp                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testDateConversions() {
        String iso = "2020-01-02T03:04:05Z";
        ZonedDateTime zdt = ZonedDateTime.parse(iso);
        Date date = StringConversions.toDate(iso, converter);
        assertEquals(Date.from(zdt.toInstant()), date);

        java.sql.Date sqlDate = StringConversions.toSqlDate(iso, converter);
        assertEquals(new java.sql.Date(zdt.toInstant().toEpochMilli()), sqlDate);

        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertEquals(new Timestamp(zdt.toInstant().toEpochMilli()), ts);
    }

    /* ---------------------------------------------------------------------- */
    /*  TimeZone and Calendar                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testTimeZoneAndCalendar() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals("UTC", tz.getID());

        Calendar cal = StringConversions.toCalendar("2020-06-01T12:00:00Z", converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(2020, cal.get(Calendar.YEAR));
        assertEquals(6, cal.get(Calendar.MONTH) + 1); // months are 0‑based
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /* ---------------------------------------------------------------------- */
    /*  LocalDate / LocalDateTime / LocalTime                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testLocalDateAndTime() {
        LocalDate ld = StringConversions.toLocalDate("2021-03-15T00:00:00Z", converter);
        assertEquals(LocalDate.of(2021, 3, 15), ld);

        LocalDateTime ldt = StringConversions.toLocalDateTime("2021-03-15T10:20:30Z", converter);
        assertEquals(LocalDateTime.of(2021, 3, 15, 10, 20, 30), ldt);

        // ISO local time
        LocalTime lt1 = StringConversions.toLocalTime("14:30:15", converter);
        assertEquals(LocalTime.of(14, 30, 15), lt1);

        // fallback via ZonedDateTime
        LocalTime lt2 = StringConversions.toLocalTime("2020-08-01T09:45:00Z", converter);
        assertEquals(LocalTime.of(9, 45, 0), lt2);
    }

    /* ---------------------------------------------------------------------- */
    /*  Locale                                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocale() {
        Locale locale = StringConversions.toLocale("fr-CA", converter);
        assertEquals(Locale.forLanguageTag("fr-CA"), locale);
    }

    /* ---------------------------------------------------------------------- */
    /*  ZoneId / ZoneOffset                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testZoneIdAndOffset() {
        ZoneId zid = StringConversions.toZoneId("America/New_York", converter);
        assertEquals(ZoneId.of("America/New_York"), zid);

        // fallback to TimeZone when ZoneId.of fails
        ZoneId zidFallback = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(TimeZone.getTimeZone("GMT+2").toZoneId(), zidFallback);

        ZoneOffset zo = StringConversions.toZoneOffset("+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zo);
    }

    /* ---------------------------------------------------------------------- */
    /*  OffsetDateTime / OffsetTime                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testOffsetConversions() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2020-01-01T12:34:56+03:00", converter);
        assertEquals(OffsetDateTime.of(2020, 1, 1, 12, 34, 56, 0, ZoneOffset.ofHours(3)), odt);

        // ISO OffsetTime
        OffsetTime ot1 = StringConversions.toOffsetTime("10:15:30+01:00", converter);
        assertEquals(OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(1)), ot1);

        // fallback via OffsetDateTime
        OffsetTime ot2 = StringConversions.toOffsetTime("2020-02-02T08:00:00+02:00", converter);
        assertEquals(OffsetTime.of(8, 0, 0, 0, ZoneOffset.ofHours(2)), ot2);
    }

    /* ---------------------------------------------------------------------- */
    /*  Instant                                                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInstant() {
        Instant instant = StringConversions.toInstant("2020-05-20T14:00:00Z", converter);
        assertEquals(Instant.parse("2020-05-20T14:00:00Z"), instant);
    }

    /* ---------------------------------------------------------------------- */
    /*  Char / CharArray / CharBuffer / ByteArray / ByteBuffer                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCharAndByteConversions() {
        char[] charArray = StringConversions.toCharArray("abc", converter);
        assertArrayEquals(new char[]{'a', 'b', 'c'}, charArray);

        // empty string → empty array
        assertEquals(0, StringConversions.toCharArray("", converter).length);

        Character[] characterArray = StringConversions.toCharacterArray("xyz", converter);
        assertArrayEquals(new Character[]{'x', 'y', 'z'}, characterArray);

        CharBuffer cb = StringConversions.toCharBuffer("hello", converter);
        assertEquals("hello", cb.toString());

        byte[] bytes = StringConversions.toByteArray("test", converter);
        assertArrayEquals("test".getBytes(converter.getOptions().getCharset()), bytes);

        ByteBuffer bb = StringConversions.toByteBuffer("data", converter);
        assertArrayEquals("data".getBytes(converter.getOptions().getCharset()), bb.array());
    }

    /* ---------------------------------------------------------------------- */
    /*  String / StringBuilder / StringBuffer                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testStringFamily() {
        assertEquals("sample", StringConversions.toString("sample", converter));
        assertNull(StringConversions.toString(null, converter));

        StringBuilder sb = StringConversions.toStringBuilder("builder", converter);
        assertNotNull(sb);
        assertEquals("builder", sb.toString());

        StringBuffer sbuf = StringConversions.toStringBuffer("buffer", converter);
        assertNotNull(sbuf);
        assertEquals("buffer", sbuf.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Year conversion                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToYear() {
        // direct 4‑digit year
        Year y1 = StringConversions.toYear("2022", converter);
        assertEquals(Year.of(2022), y1);

        // fallback via ZonedDateTime
        Year y2 = StringConversions.toYear("2021-11-30T00:00:00Z", converter);
        assertEquals(Year.of(2021), y2);
    }
}
