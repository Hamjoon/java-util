package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // It provides a default ZoneId (UTC) which is sufficient for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  List of public (package‑private) static methods in NumberConversions
     * -------------------------------------------------------------
     * byte   toByte(Object, Converter)
     * Byte   toByteZero(Object, Converter)
     * short  toShort(Object, Converter)
     * Short  toShortZero(Object, Converter)
     * int    toInt(Object, Converter)
     * Integer toIntZero(Object, Converter)
     * long   toLong(Object, Converter)
     * Long   toLongZero(Object, Converter)
     * float  toFloat(Object, Converter)
     * Float  toFloatZero(Object, Converter)
     * String floatToString(Object, Converter)
     * double toDouble(Object, Converter)
     * Double toDoubleZero(Object, Converter)
     * String doubleToString(Object, Converter)
     * BigDecimal integerTypeToBigDecimal(Object, Converter)
     * BigInteger integerTypeToBigInteger(Object, Converter)
     * AtomicLong toAtomicLong(Object, Converter)
     * AtomicInteger toAtomicInteger(Object, Converter)
     * AtomicBoolean toAtomicBoolean(Object, Converter)
     * BigDecimal floatingPointToBigDecimal(Object, Converter)
     * BigInteger floatingPointToBigInteger(Object, Converter)
     * boolean isIntTypeNotZero(Object, Converter)
     * boolean isFloatTypeNotZero(Object, Converter)
     * boolean isBigIntegerNotZero(Object, Converter)
     * boolean isBigDecimalNotZero(Object, Converter)
     * char   toCharacter(Object, Converter)
     * Date   toDate(Object, Converter)
     * Duration toDuration(Object, Converter)
     * Instant toInstant(Object, Converter)
     * java.sql.Date toSqlDate(Object, Converter)
     * Timestamp toTimestamp(Object, Converter)
     * Calendar toCalendar(Object, Converter)
     * LocalTime toLocalTime(Object, Converter)
     * LocalDate toLocalDate(Object, Converter)
     * LocalDateTime toLocalDateTime(Object, Converter)
     * ZonedDateTime toZonedDateTime(Object, Converter)
     * OffsetTime toOffsetTime(Object, Converter)
     * OffsetDateTime toOffsetDateTime(Object, Converter)
     * Year   toYear(Object, Converter)
     */

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testToByte() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals((byte) -12, NumberConversions.toByte((short) -12, converter));
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(123, converter));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 32000, NumberConversions.toShort((int) 32000, converter));
        assertEquals((short) -123, NumberConversions.toShort((byte) -123, converter));
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(999, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(42, NumberConversions.toInt((short) 42, converter));
        assertEquals(-1, NumberConversions.toInt(-1L, converter));
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(12345, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789L, NumberConversions.toLong(123456789L, converter));
        assertEquals(5L, NumberConversions.toLong(5, converter));
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(99, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, NumberConversions.toFloat(3.14f, converter), 0.00001f);
        assertEquals(-2.0f, NumberConversions.toFloat(-2, converter), 0.00001f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(1.23f, converter));
    }

    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        assertEquals("1.5", NumberConversions.floatToString(1.5f, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(2.71828, NumberConversions.toDouble(2.71828, converter), 0.0000001);
        assertEquals(-1.0, NumberConversions.toDouble(-1, converter), 0.0);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(3.14, converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        assertEquals("3.1415", NumberConversions.doubleToString(3.1415d, converter));
    }

    @Test
    public void testIntegerTypeToBigDecimal() {
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(123L, converter);
        assertEquals(BigDecimal.valueOf(123L), bd);
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        BigInteger bi = NumberConversions.integerTypeToBigInteger(456, converter);
        assertEquals(BigInteger.valueOf(456L), bi);
    }

    @Test
    public void testAtomicConversions() {
        assertEquals(new AtomicLong(77L), NumberConversions.toAtomicLong(77, converter));
        assertEquals(new AtomicInteger(33), NumberConversions.toAtomicInteger(33, converter));
        assertEquals(new AtomicBoolean(true), NumberConversions.toAtomicBoolean(1L, converter));
        assertEquals(new AtomicBoolean(false), NumberConversions.toAtomicBoolean(0, converter));
    }

    @Test
    public void testFloatingPointToBigDecimal() {
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(12.34, converter);
        assertEquals(BigDecimal.valueOf(12.34), bd);
    }

    @Test
    public void testFloatingPointToBigInteger() {
        // 12.9 -> floor -> 12
        BigInteger bi = NumberConversions.floatingPointToBigInteger(12.9, converter);
        assertEquals(BigInteger.valueOf(12L), bi);
        // -3.2 -> ceil -> -3
        bi = NumberConversions.floatingPointToBigInteger(-3.2, converter);
        assertEquals(BigInteger.valueOf(-3L), bi);
    }

    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.TEN, converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    @Test
    public void testToCharacterWithinRange() {
        assertEquals('A', NumberConversions.toCharacter(65, converter));
        assertEquals('\u0000', NumberConversions.toCharacter(0, converter));
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter((int) Character.MAX_VALUE, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative() {
        NumberConversions.toCharacter(-1, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeTooLarge() {
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, converter);
    }

    @Test
    public void testToDate() {
        long now = System.currentTimeMillis();
        Date d = NumberConversions.toDate(now, converter);
        assertEquals(now, d.getTime());
    }

    @Test
    public void testToInstant() {
        long epoch = 1609459200000L; // 2021‑01‑01T00:00:00Z
        Instant instant = NumberConversions.toInstant(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test
    public void testToSqlDate() {
        // 2020‑02‑29 (leap day)
        long epoch = LocalDate.of(2020, 2, 29)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(java.sql.Date.valueOf("2020-02-29"), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        long epoch = 123456789L;
        Timestamp ts = NumberConversions.toTimestamp(epoch, converter);
        assertEquals(epoch, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long epoch = 1000L;
        Calendar cal = NumberConversions.toCalendar(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalTimeValid() {
        // 01:02:03.004 = (1*3600 + 2*60 + 3) * 1000 + 4 = 3723004 ms
        long millis = ((1 * 3600) + (2 * 60) + 3) * 1000L + 4;
        LocalTime lt = NumberConversions.toLocalTime(millis, converter);
        assertEquals(LocalTime.of(1, 2, 3, 4_000_000), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        NumberConversions.toLocalTime(-5L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        // 24 hours = 86_400_000 ms, limit is 86_399_999
        NumberConversions.toLocalTime(86_400_000L, converter);
    }

    @Test
    public void testToLocalDateAndDateTime() {
        // Use epoch for 2022‑03‑15T12:30:45Z
        long epoch = Instant.parse("2022-03-15T12:30:45Z").toEpochMilli();
        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        assertEquals(LocalDate.of(2022, 3, 15), ld);

        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);
        assertEquals(LocalDateTime.of(2022, 3, 15, 12, 30, 45), ldt);
    }

    @Test
    public void testToZonedDateTimeAndOffsetDateTime() {
        long epoch = Instant.parse("2021-12-31T23:59:59Z").toEpochMilli();
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch).atZone(converter.getOptions().getZoneId()), zdt);

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToOffsetTimeFromLong() {
        long epoch = Instant.parse("2020-01-01T10:15:30Z").toEpochMilli();
        OffsetTime ot = NumberConversions.toOffsetTime(epoch, converter);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId());
        assertEquals(expected, ot);
    }

    @Test
    public void testToOffsetTimeFromBigDecimal() {
        // 5.5 seconds after epoch
        BigDecimal bd = BigDecimal.valueOf(5.5);
        OffsetTime ot = NumberConversions.toOffsetTime(bd, converter);
        Instant instant = Instant.ofEpochMilli(0).plusNanos((long) (5.5 * 1_000_000_000L));
        OffsetTime expected = OffsetTime.ofInstant(instant, converter.getOptions().getZoneId());
        assertEquals(expected, ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeUnsupported() {
        NumberConversions.toOffsetTime("not a number", converter);
    }

    @Test
    public void testToYear() {
        Year y = NumberConversions.toYear((short) 2023, converter);
        assertEquals(Year.of(2023), y);
    }

    /* -------------------------------------------------------------
     *  Duration conversion edge‑cases
     * ------------------------------------------------------------- */

    @Test
    public void testToDurationFromLong() {
        Duration d = NumberConversions.toDuration(1500L, converter);
        assertEquals(Duration.ofMillis(1500L), d);
    }

    @Test
    public void testToDurationFromBigDecimal() {
        // 2.75 seconds = 2 seconds + 750_000_000 nanos
        BigDecimal bd = new BigDecimal("2.75");
        Duration d = NumberConversions.toDuration(bd, converter);
        assertEquals(Duration.ofSeconds(2, 750_000_000L), d);
    }

    @Test
    public void testToDurationFromDouble() {
        Double dv = 3.125; // 3 seconds + 125_000_000 nanos
        Duration d = NumberConversions.toDuration(dv, converter);
        assertEquals(Duration.ofSeconds(3, 125_000_000L), d);
    }

    @Test
    public void testToDurationFallback() {
        // Use a type not covered above – e.g., AtomicInteger
        AtomicInteger ai = new AtomicInteger(42);
        Duration d = NumberConversions.toDuration(ai, converter);
        assertEquals(Duration.ofMillis(42L), d);
    }

    /* -------------------------------------------------------------
     *  Null handling – most methods will throw NPE because they cast.
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        NumberConversions.toByte(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        NumberConversions.toCharacter(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDurationNull() {
        NumberConversions.toDuration(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNull() {
        NumberConversions.toLocalTime(null, converter);
    }
}
