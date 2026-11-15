package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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

public class StringConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the original library.
        // It provides a default charset (UTF‑8) which is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @After
    public void tearDown() {
        converter = null;
    }

    /* ---------------------------------------------------------------------- */
    /*  asString                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testAsString() {
        assertNull(StringConversions.asString(null));
        assertEquals("hello", StringConversions.asString("hello"));
        assertEquals("123", StringConversions.asString(123));
    }

    /* ---------------------------------------------------------------------- */
    /*  toByte                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToByte_EmptyString() {
        assertEquals(Byte.valueOf((byte)0), StringConversions.toByte("", converter));
    }

    @Test
    public void testToByte_ValidByte() {
        assertEquals(Byte.valueOf((byte)12), StringConversions.toByte("12", converter));
        assertEquals(Byte.valueOf(Byte.MAX_VALUE), StringConversions.toByte(Byte.toString(Byte.MAX_VALUE), converter));
    }

    @Test
    public void testToByte_FallbackWithinRange() {
        // "128" is out of range for Byte but within Long range; fallback should clamp to null -> exception
        try {
            StringConversions.toByte("128", converter);
            fail("Expected IllegalArgumentException for out‑of‑range value");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToByte_FallbackUsingLong() {
        // "-128" is exactly Byte.MIN_VALUE – should succeed via fallback path
        assertEquals(Byte.valueOf(Byte.MIN_VALUE), StringConversions.toByte("-128", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toShort                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToShort_EmptyString() {
        assertEquals(Short.valueOf((short)0), StringConversions.toShort("", converter));
    }

    @Test
    public void testToShort_Valid() {
        assertEquals(Short.valueOf((short)12345), StringConversions.toShort("12345", converter));
    }

    @Test
    public void testToShort_OutOfRange() {
        try {
            StringConversions.toShort("40000", converter);
            fail("Expected IllegalArgumentException for out‑of‑range short");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toInt                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInt_EmptyString() {
        assertEquals(0, (int) StringConversions.toInt("", converter));
    }

    @Test
    public void testToInt_Valid() {
        assertEquals(123456, (int) StringConversions.toInt("123456", converter));
    }

    @Test
    public void testToInt_OutOfRange() {
        try {
            StringConversions.toInt(Long.toString((long) Integer.MAX_VALUE + 1), converter);
            fail("Expected IllegalArgumentException for out‑of‑range int");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toLong                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLong_EmptyString() {
        assertEquals(0L, (long) StringConversions.toLong("", converter));
    }

    @Test
    public void testToLong_Valid() {
        assertEquals(1234567890123L, (long) StringConversions.toLong("1234567890123", converter));
    }

    @Test
    public void testToLong_OutOfRange() {
        try {
            // value > Long.MAX_VALUE will cause NumberFormatException then fallback returns null
            String huge = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE).toPlainString();
            StringConversions.toLong(huge, converter);
            fail("Expected IllegalArgumentException for out‑of‑range long");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toFloat                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToFloat_EmptyString() {
        assertEquals(0f, StringConversions.toFloat("", converter), 0f);
    }

    @Test
    public void testToFloat_Valid() {
        assertEquals(3.14f, StringConversions.toFloat("3.14", converter), 0f);
    }

    @Test
    public void testToFloat_Invalid() {
        try {
            StringConversions.toFloat("not-a-float", converter);
            fail("Expected IllegalArgumentException for invalid float");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toDouble                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDouble_EmptyString() {
        assertEquals(0.0, StringConversions.toDouble("", converter), 0.0);
    }

    @Test
    public void testToDouble_Valid() {
        assertEquals(2.71828, StringConversions.toDouble("2.71828", converter), 0.0);
    }

    @Test
    public void testToDouble_Invalid() {
        try {
            StringConversions.toDouble("NaN123", converter);
            fail("Expected IllegalArgumentException for invalid double");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicBoolean                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicBoolean_Various() {
        assertTrue(StringConversions.toAtomicBoolean("true", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("TRUE", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("t", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("1", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("y", converter).get());
        assertTrue(StringConversions.toAtomicBoolean("\"true\"", converter).get());

        assertFalse(StringConversions.toAtomicBoolean("false", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("FALSE", converter).get());
        assertFalse(StringConversions.toAtomicBoolean("0", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicInteger                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        assertEquals(42, StringConversions.toAtomicInteger("42", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicLong                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        assertEquals(123456789L, StringConversions.toAtomicLong("123456789", converter).get());
    }

    /* ---------------------------------------------------------------------- */
    /*  toBoolean                                                             */
    /* ---------------------------------------------------------------------- */
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
        assertFalse(StringConversions.toBoolean("0", converter));
        assertFalse(StringConversions.toBoolean("no", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toCharacter                                                           */
    /* ---------------------------------------------------------------------- */
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

    @Test
    public void testToCharacter_InvalidNumeric() {
        try {
            StringConversions.toCharacter("9999999999", converter);
            fail("Expected IllegalArgumentException for invalid numeric char");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testToCharacter_InvalidUnicode() {
        try {
            StringConversions.toCharacter("\\uXYZ", converter);
            fail("Expected IllegalArgumentException for malformed unicode");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigInteger                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Empty() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter));
    }

    @Test
    public void testToBigInteger_Valid() {
        assertEquals(new BigInteger("12345678901234567890"), StringConversions.toBigInteger("12345678901234567890", converter));
    }

    @Test
    public void testToBigInteger_Invalid() {
        try {
            StringConversions.toBigInteger("not-a-number", converter);
            fail("Expected IllegalArgumentException for invalid BigInteger");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigDecimal                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Empty() {
        assertEquals(BigDecimal.ZERO, StringConversions.toBigDecimal("", converter));
    }

    @Test
    public void testToBigDecimal_Valid() {
        assertEquals(new BigDecimal("12345.6789"), StringConversions.toBigDecimal("12345.6789", converter));
    }

    @Test
    public void testToBigDecimal_Invalid() {
        try {
            StringConversions.toBigDecimal("abc", converter);
            fail("Expected IllegalArgumentException for invalid BigDecimal");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toURL                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToURL_Empty() throws Exception {
        assertNull(StringConversions.toURL("", converter));
    }

    @Test
    public void testToURL_Valid() throws Exception {
        URL url = StringConversions.toURL("http://example.com/path?query=1", converter);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    public void testToURL_Invalid() {
        try {
            StringConversions.toURL("ht!tp://bad_url", converter);
            fail("Expected IllegalArgumentException for malformed URL");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toURI                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToURI_Empty() {
        assertNull(StringConversions.toURI("", converter));
    }

    @Test
    public void testToURI_Valid() {
        URI uri = StringConversions.toURI("mailto:user@example.com", converter);
        assertEquals("mailto", uri.getScheme());
        assertEquals("user@example.com", uri.getSchemeSpecificPart());
    }

    /* ---------------------------------------------------------------------- */
    /*  enumToString                                                          */
    /* ---------------------------------------------------------------------- */
    private enum SampleEnum { ONE, TWO, THREE }

    @Test
    public void testEnumToString() {
        assertEquals("TWO", StringConversions.enumToString(SampleEnum.TWO, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toUUID                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToUUID_Valid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringConversions.toUUID(uuid.toString(), converter));
    }

    @Test
    public void testToUUID_Invalid() {
        try {
            StringConversions.toUUID("not-a-uuid", converter);
            fail("Expected IllegalArgumentException for malformed UUID");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toDuration                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDuration_Valid() {
        Duration d = StringConversions.toDuration("PT20.345S", converter);
        assertEquals(Duration.ofSeconds(20, 345_000_000), d);
    }

    @Test
    public void testToDuration_Invalid() {
        try {
            StringConversions.toDuration("20 seconds", converter);
            fail("Expected IllegalArgumentException for invalid Duration");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toClass                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToClass_Valid() {
        Class<?> clazz = StringConversions.toClass("java.lang.String", converter);
        assertEquals(String.class, clazz);
    }

    @Test
    public void testToClass_Invalid() {
        try {
            StringConversions.toClass("non.existent.ClassName", converter);
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toMonthDay                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToMonthDay_ISO() {
        MonthDay md = StringConversions.toMonthDay("2007-12-03", converter);
        assertEquals(MonthDay.of(12, 3), md);
    }

    @Test
    public void testToMonthDay_DotPattern() {
        MonthDay md = StringConversions.toMonthDay("12.25", converter);
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToMonthDay_FromZonedDateTime() {
        // DateUtilities.parseDate will handle ISO date‑time strings.
        MonthDay md = StringConversions.toMonthDay("2021-07-04T12:00:00Z", converter);
        assertEquals(MonthDay.of(7, 4), md);
    }

    @Test
    public void testToMonthDay_Invalid() {
        try {
            StringConversions.toMonthDay("invalid", converter);
            fail("Expected IllegalArgumentException for unparsable month‑day");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toYearMonth                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToYearMonth_ISO() {
        YearMonth ym = StringConversions.toYearMonth("2020-05", converter);
        assertEquals(YearMonth.of(2020, 5), ym);
    }

    @Test
    public void testToYearMonth_FromZonedDateTime() {
        YearMonth ym = StringConversions.toYearMonth("2022-11-15T08:30:00Z", converter);
        assertEquals(YearMonth.of(2022, 11), ym);
    }

    @Test
    public void testToYearMonth_Invalid() {
        try {
            StringConversions.toYearMonth("not-a-year-month", converter);
            fail("Expected IllegalArgumentException for invalid YearMonth");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toPeriod                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToPeriod_Valid() {
        Period p = StringConversions.toPeriod("P2Y3M4D", converter);
        assertEquals(Period.of(2, 3, 4), p);
    }

    @Test
    public void testToPeriod_Invalid() {
        try {
            StringConversions.toPeriod("2 years", converter);
            fail("Expected IllegalArgumentException for malformed Period");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toDate, toSqlDate, toTimestamp                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDate_Valid() {
        Date date = StringConversions.toDate("2020-01-02T03:04:05Z", converter);
        Instant instant = Instant.parse("2020-01-02T03:04:05Z");
        assertEquals(Date.from(instant), date);
    }

    @Test
    public void testToDate_NullInput() {
        assertNull(StringConversions.toDate(null, converter));
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = StringConversions.toSqlDate("1970-01-01T00:00:00Z", converter);
        assertEquals(0L, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = StringConversions.toTimestamp("1970-01-01T00:00:01Z", converter);
        assertEquals(1000L, ts.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toTimeZone                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToTimeZone_Valid() {
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void testToTimeZone_NullOrEmpty() {
        assertNull(StringConversions.toTimeZone(null, converter));
        assertNull(StringConversions.toTimeZone("   ", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toCalendar                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        Calendar cal = StringConversions.toCalendar("2021-12-31T23:59:59Z", converter);
        assertNotNull(cal);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }

    /* ---------------------------------------------------------------------- */
    /*  toLocalDate, toLocalDateTime, toLocalTime                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate() {
        LocalDate ld = StringConversions.toLocalDate("2022-02-28T10:15:30Z", converter);
        assertEquals(LocalDate.of(2022, 2, 28), ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = StringConversions.toLocalDateTime("2022-02-28T10:15:30Z", converter);
        assertEquals(LocalDateTime.of(2022, 2, 28, 10, 15, 30), ldt);
    }

    @Test
    public void testToLocalTime_ISO() {
        LocalTime lt = StringConversions.toLocalTime("10:15:30", converter);
        assertEquals(LocalTime.of(10, 15, 30), lt);
    }

    @Test
    public void testToLocalTime_Fallback() {
        // Fallback via ZonedDateTime
        LocalTime lt = StringConversions.toLocalTime("2022-01-01T23:45:00Z", converter);
        assertEquals(LocalTime.of(23, 45, 0), lt);
    }

    /* ---------------------------------------------------------------------- */
    /*  toLocale                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocale_Valid() {
        Locale loc = StringConversions.toLocale("en-US", converter);
        assertEquals(Locale.US, loc);
    }

    @Test
    public void testToLocale_Empty() {
        assertNull(StringConversions.toLocale("", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toZonedDateTime                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = StringConversions.toZonedDateTime("2020-03-04T05:06:07+02:00", converter);
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
        assertEquals(LocalDateTime.of(2020, 3, 4, 5, 6, 7), zdt.toLocalDateTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toZoneId                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZoneId_Valid() {
        ZoneId zid = StringConversions.toZoneId("Europe/Paris", converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testToZoneId_FallbackToTimeZone() {
        // "GMT+2" is not a standard ZoneId but TimeZone can handle it.
        ZoneId zid = StringConversions.toZoneId("GMT+2", converter);
        assertEquals(ZoneOffset.ofHours(2), zid);
    }

    @Test
    public void testToZoneId_Unknown() {
        try {
            StringConversions.toZoneId("Unknown/Zone", converter);
            fail("Expected IllegalArgumentException for unknown zone");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toZoneOffset                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZoneOffset_Valid() {
        ZoneOffset zo = StringConversions.toZoneOffset("+05:30", converter);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), zo);
    }

    @Test
    public void testToZoneOffset_Invalid() {
        try {
            StringConversions.toZoneOffset("invalid", converter);
            fail("Expected IllegalArgumentException for malformed offset");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toOffsetDateTime                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = StringConversions.toOffsetDateTime("2021-07-01T12:00:00+03:00", converter);
        assertEquals(OffsetDateTime.of(2021, 7, 1, 12, 0, 0, 0, ZoneOffset.ofHours(3)), odt);
    }

    /* ---------------------------------------------------------------------- */
    /*  toOffsetTime                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_ISO() {
        OffsetTime ot = StringConversions.toOffsetTime("13:45:30+02:00", converter);
        assertEquals(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), ot);
    }

    @Test
    public void testToOffsetTime_FallbackViaOffsetDateTime() {
        OffsetTime ot = StringConversions.toOffsetTime("2021-08-01T09:15:00+01:00", converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test
    public void testToOffsetTime_Invalid() {
        try {
            StringConversions.toOffsetTime("bad-time", converter);
            fail("Expected IllegalArgumentException for malformed OffsetTime");
        } catch (IllegalArgumentException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  toInstant                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInstant() {
        Instant i = StringConversions.toInstant("2020-01-01T00:00:00Z", converter);
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), i);
    }

    /* ---------------------------------------------------------------------- */
    /*  toCharArray                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCharArray_Null() {
        assertArrayEquals(EMPTY_CHAR_ARRAY, StringConversions.toCharArray(null, converter));
    }

    @Test
    public void testToCharArray_Empty() {
        assertArrayEquals(EMPTY_CHAR_ARRAY, StringConversions.toCharArray("", converter));
    }

    @Test
    public void testToCharArray_NonEmpty() {
        assertArrayEquals(new char[]{'a', 'b', 'c'}, StringConversions.toCharArray("abc", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toCharacterArray                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCharacterArray() {
        Character[] expected = {'J', 'a', 'v', 'a'};
        assertArrayEquals(expected, StringConversions.toCharacterArray("Java", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toCharBuffer                                                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCharBuffer() {
        CharBuffer cb = StringConversions.toCharBuffer("buffer", converter);
        assertEquals("buffer", cb.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  toByteArray / toByteBuffer                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToByteArray_NullOrEmpty() {
        assertArrayEquals(EMPTY_BYTE_ARRAY, StringConversions.toByteArray(null, converter));
        assertArrayEquals(EMPTY_BYTE_ARRAY, StringConversions.toByteArray("", converter));
    }

    @Test
    public void testToByteArray_NonEmpty() {
        byte[] bytes = StringConversions.toByteArray("abc", converter);
        assertArrayEquals("abc".getBytes(), bytes);
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer bb = StringConversions.toByteBuffer("xyz", converter);
        assertEquals(ByteBuffer.wrap("xyz".getBytes()), bb);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString, toStringBuffer, toStringBuilder                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToString() {
        assertNull(StringConversions.toString(null, converter));
        assertEquals("test", StringConversions.toString("test", converter));
    }

    @Test
    public void testToStringBuffer() {
        assertNull(StringConversions.toStringBuffer(null, converter));
        assertEquals(new StringBuffer("buf"), StringConversions.toStringBuffer("buf", converter));
    }

    @Test
    public void testToStringBuilder() {
        assertNull(StringConversions.toStringBuilder(null, converter));
        assertEquals(new StringBuilder("builder"), StringConversions.toStringBuilder("builder", converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toYear                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToYear_Valid() {
        Year y = StringConversions.toYear("2022", converter);
        assertEquals(Year.of(2022), y);
    }

    @Test
    public void testToYear_FallbackViaZonedDateTime() {
        Year y = StringConversions.toYear("2022-03-04T12:00:00Z", converter);
        assertEquals(Year.of(2022), y);
    }

    @Test
    public void testToYear_Invalid() {
        try {
            StringConversions.toYear("not-a-year", converter);
            fail("Expected IllegalArgumentException for malformed year");
        } catch (IllegalArgumentException ignored) {}
    }
}
